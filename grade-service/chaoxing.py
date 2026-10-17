# -*- coding: utf-8 -*-
"""超星（czlgj）登录与成绩查询客户端。

移植自 py2 工作区的 login_full.py / score.py，做了三处改造：
  1. 凭据由调用方传入，不再硬编码学号密码；
  2. 去掉命令行交互（原脚本识别失败会让人手输滑块坐标），失败直接抛错；
  3. 返回结构化数据，不在库层打印日志。

三个域三套凭据，互不相同，这是整条链路最容易踩坑的地方：
  czlgjcas.chaoxing.com     SESSION / TGC     走登录流程
  czlgj.hall.chaoxing.com   cookie token(JWT) 并且接口还需 Authorization: Bearer <同一个JWT>
  shelf.chaoxing.com        另一个 JWT       放在 token 请求头
"""
import base64
import hashlib
import io
import json
import re
import time
import uuid
from urllib.parse import quote, urljoin

import requests

CAS = "https://czlgjcas.chaoxing.com"
SERVICE = "https://czlgj.hall.chaoxing.com/pedestal/auth/cas/login?redirect=%2Fpersonal-center"
PORTAL = "https://czlgj.hall.chaoxing.com"
SHELF = "https://shelf.chaoxing.com"

CAPTCHA_ID = "qDG21VMg9qS5Rcok4cfpnHGnpf5LhcAv"
SALT = CAPTCHA_ID
CAPTCHA_TYPE = "slide"
TOKEN_TTL_MS = 300000
CAPTCHA_BASE = "https://captcha.chaoxing.com"

# 取图接口的 Referer 必须带尾斜杠，否则实测通过率从 2/4 掉到 0/4
CAPTCHA_REFERER = (
    "https://czlgjcas.chaoxing.com/cas/login?service=https%3A%2F%2Fczlgj.hall."
    "chaoxing.com%2Fpedestal%2Fauth%2Fcas%2Flogin%3Fredirect%3D%252F"
)

UA = ("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 "
      "(KHTML, like Gecko) Chrome/152.0.0.0 Safari/537.36")

APP_ID = "22910f122f074a31ae77209a3b4a7f62"
APP_KEY = "1I3Uh3Th0Q78vrF8"
WFLOGIN_FIXED = (
    "key=scoreQueryStudentMobile&roleType=2&wfw_token=jkx6UjC8VBYKlYIEf__bW"
    "&fidEnc=3eb5c6fa7fea8e1b&uid=0&mappId=19225394&formid=&roleid=&rolename=&topMenu="
    "&mappIdEnc=9c42468bb4033c6155b1eeccd57ab1ac"
    "&wfwEnc=4FFF6EC8AB68E6B5F86CE5C08129539A"
    "&appId=" + APP_ID + "&appKey=" + APP_KEY
)


class ChaoxingError(Exception):
    """带业务码的异常，便于接口层区分状态。"""

    def __init__(self, code, message):
        super().__init__(message)
        self.code = code
        self.message = message


def md5(value):
    return hashlib.md5(str(value).encode("utf-8")).hexdigest()


def parse_jsonp(text):
    """解析 JSONP 响应，不要用字符串切片取字段。"""
    return json.loads(text[text.index("{"): text.rindex("}") + 1])


def login_url():
    """service 必须 URL 编码，裸写服务端认不出。"""
    return CAS + "/cas/login?service=" + quote(SERVICE, safe="")


def login_domain():
    """domain 参数必须用【未编码】的原始 URL，否则报“未获取到即将跳转的系统”。"""
    return CAS + "/cas/login?service=" + SERVICE


def piece_width(piece_bytes):
    """从切片图的 alpha 通道量出实际宽度。"""
    import numpy as np
    from PIL import Image
    arr = np.array(Image.open(io.BytesIO(piece_bytes)).convert("RGBA"))
    cols = np.where(arr[:, :, 3].max(axis=0) > 0)[0]
    return int(cols.max() - cols.min() + 1)


def fetch_challenge(session, server_time):
    """取一张新滑块图，返回 (切片bytes, 背景bytes, token, iv)。"""
    key = md5(str(server_time) + str(uuid.uuid4()))
    tok = "{}:{}".format(md5(str(server_time) + SALT + CAPTCHA_TYPE + key),
                         server_time + TOKEN_TTL_MS)
    iv = md5(SALT + CAPTCHA_TYPE + str(int(time.time() * 1000)) + str(uuid.uuid4()))
    text = session.get(
        CAPTCHA_BASE + "/captcha/get/verification/image",
        params={"callback": "cx_captcha_function", "captchaId": CAPTCHA_ID,
                "type": CAPTCHA_TYPE, "version": "1.1.20", "captchaKey": key,
                "token": tok, "referer": CAPTCHA_REFERER, "iv": iv,
                "_": int(time.time() * 1000)},
        timeout=15).text
    data = parse_jsonp(text)
    vo = data["imageVerificationVo"]
    piece = session.get(vo["cutoutImage"], timeout=15).content
    background = session.get(vo["shadeImage"], timeout=15).content
    return piece, background, data["token"], iv


def submit_captcha(session, token, iv, x):
    text = session.get(
        CAPTCHA_BASE + "/captcha/check/verification/result",
        params={"callback": "cx_captcha_function", "captchaId": CAPTCHA_ID,
                "type": CAPTCHA_TYPE, "token": token,
                "textClickArr": json.dumps([{"x": int(max(x, 0))}], separators=(",", ":")),
                "coordinate": "[]", "runEnv": "10", "version": "1.1.20",
                "t": "a", "iv": iv, "_": int(time.time() * 1000)},
        timeout=15).text
    return parse_jsonp(text)


def solve_slider(session, server_time, attempts):
    """ddddocr 识别滑块。成功返回 validate 字符串，全部失败抛 CAPTCHA_FAILED。

    实测单次成功率约 60%，所以必须换图重试。
    坐标公式：x = ddddocr 返回值 - 切片宽/2
      服务端要的是缺口中心，ddddocr 给的是左边缘，差的正是半个切片宽。
    """
    try:
        import ddddocr
    except ImportError as exc:
        raise ChaoxingError("CAPTCHA_UNAVAILABLE",
                            "服务端未安装 ddddocr，无法自动识别滑块") from exc

    ocr = ddddocr.DdddOcr(det=False, ocr=False, show_ad=False)
    last_message = ""
    for attempt in range(1, attempts + 1):
        piece, background, token, iv = fetch_challenge(session, server_time)
        left = int(ocr.slide_match(piece, background, simple_target=True)["target"][0])
        width = piece_width(piece)
        x = max(left - width // 2, 0)
        result = submit_captcha(session, token, iv, x)
        if result.get("result") is True:
            return json.loads(result["extraData"])["validate"]
        last_message = str(result.get("msg") or "")
        time.sleep(0.35)
    raise ChaoxingError("CAPTCHA_FAILED",
                        "滑块自动识别未通过（已尝试 " + str(attempts) + " 次）"
                        + ("：" + last_message if last_message else ""))


def login(username, password, captcha_attempts=8):
    """完整 CAS 登录，返回门户 cookie 字典。"""
    session = requests.Session()
    session.headers.update({"User-Agent": UA,
                            "Accept-Language": "zh-CN,zh;q=0.9,en;q=0.8"})
    url = login_url()
    domain = login_domain()

    page = session.get(url, timeout=20, headers={"Referer": CAS + "/"}).text
    match = re.search(r'name=["\']execution["\']\s+value=["\']([^"\']+)', page)
    if not match:
        raise ChaoxingError("LOGIN_PAGE_CHANGED",
                            "登录页里没找到 execution 字段，页面结构可能已变化")
    execution = match.group(1)

    session.headers["Referer"] = CAS + "/"
    conf = parse_jsonp(session.get(
        CAPTCHA_BASE + "/captcha/get/conf",
        params={"callback": "cx_captcha_function", "captchaId": CAPTCHA_ID,
                "_": int(time.time() * 1000)},
        timeout=15).text)
    validate = solve_slider(session, conf["t"], captcha_attempts)

    hashed = md5(password)
    xhr = {"X-Requested-With": "XMLHttpRequest", "Referer": url, "Origin": CAS}

    check = session.post(CAS + "/cas/pwdLoginCheck",
                         data={"username": username, "password": hashed,
                               "domain": domain, "validate": validate},
                         headers=xhr, timeout=20).json()
    if check.get("result") != 1:
        raise ChaoxingError("BAD_CREDENTIALS",
                            "学号或密码不正确：" + str(check.get("msg") or "")[:80])
    cas_token = check["msg"]

    session.get(CAS + "/cas/getTimes", params={"username": username},
                headers=xhr, timeout=20)
    session.post(CAS + "/cas/saveDomain",
                 data={"xgh": username, "domain": domain, "remarks": cas_token,
                       "status": 1},
                 headers=xhr, timeout=20)
    session.post(CAS + "/cas/checkTwoFactorTrigger",
                 data={"username": username, "password": hashed, "domain": domain},
                 headers=xhr, timeout=20)

    form = {
        "execution": execution,
        "_eventId": "submit",
        "geolocation": "",
        "authType": "1",
        "time": "1",
        "username": username,
        "password": hashed,
        "validate": cas_token,   # 这里放的是 CasPWDLoginToken，不是滑块 validate
        "rememberMe": "true",
    }
    resp = session.post(url, data=form, timeout=25, allow_redirects=False, headers={
        "User-Agent": UA,
        "Content-Type": "application/x-www-form-urlencoded",
        "Origin": CAS,
        "Referer": url,
        "Accept": "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8",
        "Upgrade-Insecure-Requests": "1",
        "Sec-Fetch-Site": "same-origin",
        "Sec-Fetch-Mode": "navigate",
        "Sec-Fetch-User": "?1",
        "Sec-Fetch-Dest": "document",
    })
    if resp.status_code not in (301, 302, 303):
        raise ChaoxingError("LOGIN_REJECTED",
                            "登录未通过（HTTP " + str(resp.status_code) + "）")

    location = resp.headers.get("Location")
    if not location:
        raise ChaoxingError("LOGIN_REJECTED", "登录响应没有跳转地址")

    current = location
    hops = 0
    while current and hops < 6:
        hops += 1
        if current.startswith("/"):
            current = urljoin(PORTAL, current)
        hop = session.get(current, timeout=25, allow_redirects=False,
                          headers={"User-Agent": UA, "Referer": url})
        current = hop.headers.get("Location")

    portal_token = session.cookies.get("token")
    if not portal_token:
        raise ChaoxingError("LOGIN_REJECTED", "未拿到门户登录态 token")

    info = session.get(PORTAL + "/pedestal/auth/userInfo", timeout=25,
                       headers={"Authorization": "Bearer " + portal_token,
                                "Referer": PORTAL + "/personal-center",
                                "Accept": "*/*"})
    try:
        payload = info.json()
    except ValueError:
        payload = {}
    if payload.get("success") is not True:
        raise ChaoxingError("LOGIN_REJECTED", "门户接口未确认登录态")

    return {"cookies": {c.name: c.value for c in session.cookies},
            "token": portal_token}


def decode_jwt_payload(token):
    try:
        part = token.split(".")[1]
        return json.loads(base64.urlsafe_b64decode(part + "=" * (-len(part) % 4)))
    except Exception:
        return {}


def verify_shelf_token(token):
    return bool(decode_jwt_payload(token))


def get_shelf_token(cookies):
    """用门户 cookie 走 OAuth 跳转链换 shelf 域的 token。"""
    session = requests.Session()
    session.headers.update({"User-Agent": UA, "Referer": PORTAL + "/"})
    session.cookies.update(cookies)

    redirect_uri = SHELF + "/api/login/wfLogin?" + WFLOGIN_FIXED
    url = ("https://auth.chaoxing.com/connect/oauth2/authorize?appid=" + APP_ID
           + "&redirect_uri=" + quote(redirect_uri, safe="")
           + "&response_type=code&scope=snsapi_base&state=1400")
    first = session.get(url, timeout=25, allow_redirects=False)
    if first.status_code != 302:
        raise ChaoxingError("SHELF_TOKEN_FAILED",
                            "OAuth 授权失败（HTTP " + str(first.status_code) + "）")

    second = session.get(first.headers["Location"], timeout=25, allow_redirects=False)
    location = second.headers.get("Location", "")
    match = re.search(r"[?&#]token=([^&\s]+)", location)
    if not match:
        raise ChaoxingError("SHELF_TOKEN_FAILED", "wfLogin 未返回 shelf token")
    token = match.group(1)
    if not verify_shelf_token(token):
        raise ChaoxingError("SHELF_TOKEN_FAILED", "拿到的 shelf token 格式无效")
    return token


def shelf_session(cookies, shelf_token):
    session = requests.Session()
    session.headers.update({
        "User-Agent": UA,
        "Accept": "application/json, text/plain, */*",
        "Referer": SHELF + "/",
        "token": shelf_token,
    })
    session.cookies.update(cookies)
    session.cookies.set("token", shelf_token, domain="shelf.chaoxing.com")
    return session


def call_api(session, path, **params):
    response = session.get(SHELF + path, params=params or None, timeout=25)
    try:
        return response.json()
    except ValueError:
        raise ChaoxingError("UPSTREAM_BAD_RESPONSE",
                            "成绩接口返回了非 JSON 内容（HTTP "
                            + str(response.status_code) + "）")


def fetch_terms(session):
    """返回 (当前学期, 学期列表)。"""
    current = (call_api(session, "/api/resultClassCourse/queryCurrentTerm")
               .get("data") or "")
    raw = call_api(session, "/api/resultClassCourse/queryMicTermList").get("data") or []
    terms = [item.get("term") for item in raw if item.get("term")]
    if current and current not in terms:
        terms.insert(0, current)
    return current, terms


def summarize(courses):
    """按学分加权计算平均绩点。

    服务端 getStudentAvgPoint 返回的 avgPoint 实测恒为 0.00（学校没启用），
    所以绩点必须自己算，否则页面上永远是 0。
    """
    total_credit = 0.0
    numerator = 0.0
    denominator = 0.0
    for row in courses:
        try:
            credit = float(row.get("finalCredit") or 0)
        except (TypeError, ValueError):
            credit = 0.0
        try:
            point = float(row.get("finalGradePoint") or 0)
        except (TypeError, ValueError):
            point = 0.0
        total_credit += credit
        numerator += point * credit
        denominator += credit
    return {
        "totalCredit": round(total_credit, 1),
        "gpa": round(numerator / denominator, 2) if denominator else 0.0,
        "courseCount": len(courses),
    }


def fetch_term(session, term):
    """取单个学期的成绩，含发布状态与统计。"""
    released = bool(call_api(session, "/api/resultStudent/checkReleaseState",
                             term=term).get("data"))
    rows = call_api(session, "/api/resultStudent/queryList4Student",
                    term=term, currentPage=1, pageSize=100).get("data") or {}
    courses = rows.get("rows", []) if isinstance(rows, dict) else []
    avg = call_api(session, "/api/resultStudent/getStudentAvgPoint",
                   term=term, releaseState=1).get("data") or {}
    summary = summarize(courses)
    summary["passCount"] = avg.get("passCount")
    summary["noPassCount"] = avg.get("noPassCount")
    return {"term": term, "released": released, "courses": courses,
            "summary": summary, "empty": len(courses) == 0}
