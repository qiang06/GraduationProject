# -*- coding: utf-8 -*-
"""成绩查询服务（FastAPI）。

设计要点：
  * 自签不透明会话令牌，不复用论坛 JWT——两者凭据语义不同，
    且论坛前端收到 401 会自动登出，耦合会导致超星会话过期把人踢出论坛。
  * 学期列表与单学期成绩分开，前端点到哪个学期才拉哪个（懒加载）。
  * 每个学期结果缓存 10 分钟；成功结果另存磁盘快照，
    超星不可用时回落快照并在响应里标记 offline，避免演示时白屏。
  * 凭据（超星 cookie + shelf token）默认只驻留内存。开启 GRADE_PERSIST_CREDENTIAL
    后可落盘复用，用于跳过滑块验证码——滑块只在 CAS 密码登录时需要。
    密码永不落盘。
"""
import json
import os
import threading
import time
import uuid
from dataclasses import dataclass, field
from pathlib import Path

from fastapi import Body, FastAPI, Header, Query
from fastapi.middleware.cors import CORSMiddleware
from fastapi.responses import JSONResponse

import chaoxing
from chaoxing import ChaoxingError

SESSION_TTL = int(os.getenv("GRADE_SESSION_TTL", "7200"))
TERM_CACHE_TTL = int(os.getenv("GRADE_CACHE_TTL", "600"))
CAPTCHA_ATTEMPTS = int(os.getenv("GRADE_CAPTCHA_ATTEMPTS", "8"))
BASE_DIR = Path(__file__).parent
SNAPSHOT_DIR = Path(os.getenv("GRADE_SNAPSHOT_DIR", str(BASE_DIR / "snapshots")))
CREDENTIAL_DIR = Path(os.getenv("GRADE_CREDENTIAL_DIR", str(BASE_DIR / ".credentials")))
# 仅当显式开启时才把凭据落盘，默认关闭
PERSIST_CREDENTIAL = os.getenv("GRADE_PERSIST_CREDENTIAL", "0").lower() in ("1", "true", "yes", "on")
# shelf token 临期多久算不可用
TOKEN_SKEW_SECONDS = 300

app = FastAPI(title="成绩查询服务", version="1.1.0")
app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_methods=["*"],
    allow_headers=["*"],
)

_login_lock = threading.Lock()


@dataclass
class GradeSession:
    token: str
    student: dict
    cookies: dict
    shelf_token: str
    created_at: float
    last_used: float
    http: object
    cache: dict = field(default_factory=dict)

    def touch(self):
        self.last_used = time.time()

    def expired(self):
        return time.time() - self.last_used > SESSION_TTL


SESSIONS = {}


def fail(status, code, message):
    return JSONResponse(status_code=status, content={"code": code, "message": message})


def error_response(exc):
    mapping = {
        "BAD_CREDENTIALS": 401,
        "SESSION_REQUIRED": 401,
        "SESSION_EXPIRED": 401,
        "SAVED_SESSION_INVALID": 401,
        "CAPTCHA_FAILED": 502,
        "CAPTCHA_UNAVAILABLE": 503,
        "LOGIN_PAGE_CHANGED": 502,
        "LOGIN_REJECTED": 502,
        "SHELF_TOKEN_FAILED": 502,
        "UPSTREAM_BAD_RESPONSE": 502,
    }
    return fail(mapping.get(exc.code, 502), exc.code, exc.message)


def require_session(value):
    if not value:
        raise ChaoxingError("SESSION_REQUIRED", "缺少会话令牌，请先登录")
    session = SESSIONS.get(value)
    if session is None or session.expired():
        SESSIONS.pop(value, None)
        raise ChaoxingError("SESSION_EXPIRED", "登录状态已过期，请重新登录")
    session.touch()
    return session


def safe_name(value):
    return "".join(ch for ch in str(value) if ch.isalnum() or ch in "-_")


# --------------------------------------------------------------------------
# 快照：只有成绩，不含任何凭据
# --------------------------------------------------------------------------
def snapshot_path(student_id, term):
    return SNAPSHOT_DIR / safe_name(student_id) / (safe_name(term) + ".json")


def save_snapshot(student_id, payload):
    try:
        path = snapshot_path(student_id, payload["term"])
        path.parent.mkdir(parents=True, exist_ok=True)
        path.write_text(json.dumps(payload, ensure_ascii=False), encoding="utf-8")
    except OSError:
        pass


def load_snapshot(student_id, term):
    try:
        path = snapshot_path(student_id, term)
        if path.exists():
            data = json.loads(path.read_text(encoding="utf-8"))
            data["offline"] = True
            return data
    except (OSError, ValueError):
        pass
    return None


# --------------------------------------------------------------------------
# 凭据持久化（默认关闭）
# --------------------------------------------------------------------------
def credential_path(student_id):
    return CREDENTIAL_DIR / (safe_name(student_id) + ".json")


def save_credentials(student_id, cookies, shelf_token):
    """落盘超星凭据。仅保存 cookie 与 shelf token，永不保存密码。"""
    if not PERSIST_CREDENTIAL:
        return
    try:
        CREDENTIAL_DIR.mkdir(parents=True, exist_ok=True)
        path = credential_path(student_id)
        path.write_text(json.dumps({
            "studentId": student_id,
            "cookies": cookies,
            "shelfToken": shelf_token,
            "savedAt": int(time.time()),
        }, ensure_ascii=False), encoding="utf-8")
        try:
            os.chmod(path, 0o600)
        except OSError:
            pass
    except OSError:
        pass


def load_credentials(student_id):
    if not PERSIST_CREDENTIAL:
        return None
    try:
        path = credential_path(student_id)
        if not path.exists():
            return None
        data = json.loads(path.read_text(encoding="utf-8"))
        if data.get("cookies") and data.get("shelfToken"):
            return data
    except (OSError, ValueError):
        pass
    return None


def clear_credentials(student_id):
    try:
        credential_path(student_id).unlink(missing_ok=True)
    except OSError:
        pass


def shelf_token_usable(token):
    """shelf token 是 JWT，只有 24 小时有效期，临期即视为不可用。"""
    if not token:
        return False
    payload = chaoxing.decode_jwt_payload(token)
    if not payload:
        return False
    exp = payload.get("exp")
    if not exp:
        return True
    try:
        return time.time() < float(exp) - TOKEN_SKEW_SECONDS
    except (TypeError, ValueError):
        return False


def refresh_shelf_token(session):
    session.shelf_token = chaoxing.get_shelf_token(session.cookies)
    session.http = chaoxing.shelf_session(session.cookies, session.shelf_token)
    save_credentials(session.student["studentId"], session.cookies, session.shelf_token)


def with_shelf_retry(session, action):
    """执行带 shelf token 的调用；失败时续期一次再重试。

    shelf token 只有 24 小时，且证书可能被服务端提前失效，
    所有依赖它的调用都必须走这条路径，否则会出现
    “学期列表拉不出来但成绩能拉出来”这类不一致现象。
    """
    try:
        return action()
    except Exception:
        refresh_shelf_token(session)
        return action()


def build_session(student_id, cookies, shelf_token, claims=None):
    claims = claims or {}
    token = uuid.uuid4().hex
    now = time.time()
    SESSIONS[token] = GradeSession(
        token=token,
        student={"studentId": student_id,
                 "name": claims.get("username") or claims.get("name") or student_id,
                 "uid": claims.get("uid")},
        cookies=cookies,
        shelf_token=shelf_token,
        created_at=now,
        last_used=now,
        http=chaoxing.shelf_session(cookies, shelf_token),
    )
    return SESSIONS[token]


def find_live_session(student_id):
    for session in SESSIONS.values():
        if session.student.get("studentId") == student_id and not session.expired():
            return session
    return None


def prune_sessions():
    for key in [k for k, v in SESSIONS.items() if v.expired()]:
        SESSIONS.pop(key, None)


def try_resume(student_id):
    """尝试免滑块恢复：内存会话 -> 磁盘凭据（必要时用 cookie 续期 shelf token）。

    返回 GradeSession，失败返回 None。密码在这条路径上完全用不到。
    """
    live = find_live_session(student_id)
    if live:
        return live

    saved = load_credentials(student_id)
    if not saved:
        return None

    cookies = saved["cookies"]
    token = saved["shelfToken"]
    if shelf_token_usable(token):
        return build_session(student_id, cookies, token, saved.get("claims") or {})

    # shelf token 过期，但门户 cookie 可能仍有效 —— 这条路不需要滑块
    try:
        fresh = chaoxing.get_shelf_token(cookies)
    except Exception:
        return None
    save_credentials(student_id, cookies, fresh)
    return build_session(student_id, cookies, fresh, saved.get("claims") or {})


# --------------------------------------------------------------------------
# 接口
# --------------------------------------------------------------------------
@app.get("/grade/api/health")
def health():
    try:
        import ddddocr  # noqa: F401
        ocr_ready = True
    except ImportError:
        ocr_ready = False
    return {"ok": True, "ddddocr": ocr_ready, "sessions": len(SESSIONS),
            "persistCredential": PERSIST_CREDENTIAL}


class Req:
    def __init__(self, data):
        self._data = data or {}

    def text(self, key):
        value = self._data.get(key)
        return value.strip() if isinstance(value, str) else ""


@app.post("/grade/api/login")
def login(payload: dict = Body(...)):
    req = Req(payload)
    student_id = req.text("studentId")
    password = req.text("password")
    if not student_id:
        return fail(400, "BAD_REQUEST", "请填写学号")

    prune_sessions()

    # 有可用凭据就不必再过滑块；滑块只在 CAS 密码登录这一步需要
    resumed = try_resume(student_id)
    if resumed:
        return {"token": resumed.token, "student": resumed.student,
                "expiresIn": SESSION_TTL, "resumed": True}

    if not password:
        return fail(400, "BAD_REQUEST", "请填写密码")

    try:
        with _login_lock:
            login_result = chaoxing.login(student_id, password, CAPTCHA_ATTEMPTS)
        cookies = login_result["cookies"]
        shelf_token = chaoxing.get_shelf_token(cookies)
    except ChaoxingError as exc:
        return error_response(exc)
    except Exception as exc:
        return fail(504, "UPSTREAM_UNAVAILABLE", "无法连接超星服务：" + str(exc)[:120])

    claims = chaoxing.decode_jwt_payload(login_result["token"])
    save_credentials(student_id, cookies, shelf_token)
    session = build_session(student_id, cookies, shelf_token, claims)
    session.student["name"] = claims.get("username") or student_id
    return {"token": session.token, "student": session.student,
            "expiresIn": SESSION_TTL, "resumed": False}


@app.get("/grade/api/saved")
def saved_state(studentId: str = Query(default="")):
    """仅用于前端判断是否展示“继续上次会话”入口。

    只返回布尔值，不泄露任何凭据内容；开关关闭时恒为 false。
    """
    if not PERSIST_CREDENTIAL or not studentId.strip():
        return {"available": False, "persistCredential": PERSIST_CREDENTIAL}
    return {"available": load_credentials(studentId.strip()) is not None,
            "persistCredential": PERSIST_CREDENTIAL}


@app.post("/grade/api/resume")
def resume(payload: dict = Body(...)):
    """仅用已保存的凭据恢复会话，绝不触发密码登录与滑块。"""
    req = Req(payload)
    student_id = req.text("studentId")
    if not student_id:
        return fail(400, "BAD_REQUEST", "请提供学号")
    session = try_resume(student_id)
    if not session:
        return error_response(ChaoxingError(
            "SAVED_SESSION_INVALID", "没有可用的已保存会话，请使用学号密码登录"))
    return {"token": session.token, "student": session.student,
            "expiresIn": SESSION_TTL, "resumed": True}


@app.post("/grade/api/logout")
def logout(x_grade_session: str = Header(default="")):
    session = SESSIONS.pop(x_grade_session or "", None)
    if session:
        # 明确退出时同时清除落盘凭据，避免“点了退出但凭据还在”
        clear_credentials(session.student.get("studentId"))
    return {"ok": True}


@app.get("/grade/api/me")
def me(x_grade_session: str = Header(default="")):
    try:
        session = require_session(x_grade_session)
    except ChaoxingError as exc:
        return error_response(exc)
    return {"student": session.student,
            "expiresIn": int(SESSION_TTL - (time.time() - session.last_used))}


@app.get("/grade/api/terms")
def terms(x_grade_session: str = Header(default="")):
    try:
        session = require_session(x_grade_session)
    except ChaoxingError as exc:
        return error_response(exc)
    try:
        current, term_list = with_shelf_retry(
            session, lambda: chaoxing.fetch_terms(session.http))
    except ChaoxingError as exc:
        return error_response(exc)
    except Exception as exc:
        return fail(504, "UPSTREAM_UNAVAILABLE", "获取学期列表失败：" + str(exc)[:120])
    return {"current": current, "terms": term_list, "student": session.student}


@app.get("/grade/api/term")
def term_detail(term: str = Query(...), refresh: bool = Query(default=False),
                x_grade_session: str = Header(default="")):
    try:
        session = require_session(x_grade_session)
    except ChaoxingError as exc:
        return error_response(exc)

    # refresh=1 时跳过缓存，用于刚公布成绩后强制重新取数
    cached = session.cache.get(term)
    if not refresh and cached and time.time() - cached[0] < TERM_CACHE_TTL:
        return dict(cached[1], cached=True)

    try:
        payload = with_shelf_retry(
            session, lambda: chaoxing.fetch_term(session.http, term))
    except ChaoxingError as exc:
        snapshot = load_snapshot(session.student["studentId"], term)
        if snapshot:
            return dict(snapshot, cached=True)
        return error_response(exc)
    except Exception as exc:
        snapshot = load_snapshot(session.student["studentId"], term)
        if snapshot:
            return dict(snapshot, cached=True)
        return fail(504, "UPSTREAM_UNAVAILABLE", "成绩获取失败：" + str(exc)[:120])

    # fetchedAt 让前端能显示“更新于几点”，避免缓存状态不透明
    payload = dict(payload, cached=False, offline=False, fetchedAt=int(time.time()))
    session.cache[term] = (time.time(), payload)
    if not payload["empty"]:
        save_snapshot(session.student["studentId"], payload)
    return payload
