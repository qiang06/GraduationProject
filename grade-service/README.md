# 成绩查询服务（grade-service）

独立于校园论坛后端的 FastAPI 服务，负责用超星（学习通）账号查询成绩，
并把结果整理成前端需要的形状。

**端口 `8000`**，前端通过 Vite 代理 `/grade` → `http://127.0.0.1:8000` 调用，同源、无 CORS 问题。

## 一、安装依赖

复用 `py2` 的虚拟环境（它已经有 ddddocr / onnxruntime / opencv / Pillow / numpy，
单独建 venv 要重下上百 MB）：

```powershell
C:\Users\qiang\Desktop\1\py2\.venv\Scripts\python.exe -m pip install fastapi uvicorn -i https://pypi.tuna.tsinghua.edu.cn/simple
```

## 二、启动

```powershell
cd grade-service
.\start.cmd
```

或者手动：

```powershell
C:\Users\qiang\Desktop\1\py2\.venv\Scripts\python.exe -m uvicorn app:app --host 127.0.0.1 --port 8000 --app-dir .
```

自检：浏览器打开 <http://127.0.0.1:8000/grade/api/health>，
返回 `{"ok":true,"ddddocr":true,...}` 表示就绪。`ddddocr` 为 `false` 说明依赖缺失。

## 三、接口

| 方法 | 路径 | 说明 |
|---|---|---|
| GET  | `/grade/api/health` | 健康检查与依赖自检 |
| POST | `/grade/api/login` | 学号 + 密码登录；若有可用凭据则跳过滑块 |
| POST | `/grade/api/resume` | 仅用已保存凭据恢复会话，**绝不触发密码登录与滑块** |
| POST | `/grade/api/logout` | 注销会话 |
| GET  | `/grade/api/me` | 当前会话的学生信息 |
| GET  | `/grade/api/terms` | 当前学期与全部学期列表 |
| GET  | `/grade/api/term?term=` | 指定学期的成绩、发布状态与统计 |

除 `health` 与 `login` 外，其余接口都需要请求头 `X-Grade-Session`。

### 错误码

| code | HTTP | 含义 |
|---|---|---|
| `BAD_REQUEST` | 400 | 学号或密码没填 |
| `BAD_CREDENTIALS` | 401 | 学号或密码不正确 |
| `SESSION_REQUIRED` / `SESSION_EXPIRED` | 401 | 会话缺失或过期，前端应退回登录态 |
| `SAVED_SESSION_INVALID` | 401 | 没有可用的已保存会话 |
| `CAPTCHA_FAILED` | 502 | 滑块自动识别未通过（换图重试后仍失败） |
| `CAPTCHA_UNAVAILABLE` | 503 | 服务端缺少 ddddocr |
| `LOGIN_REJECTED` / `LOGIN_PAGE_CHANGED` | 502 | 超星登录页结构变化或登录被拒 |
| `SHELF_TOKEN_FAILED` | 502 | 换取成绩域令牌失败 |
| `UPSTREAM_UNAVAILABLE` | 504 | 网络不通或超星不可用 |

## 四、设计要点

**为什么自签会话令牌，而不是复用论坛 JWT**：两者的凭据语义不同——论坛 JWT 证明“你是论坛用户”，
超星会话证明“你是某学号本人”。而且论坛前端收到 401 会自动登出，若共用一套，
超星会话过期会把用户从论坛踢出去。

**懒加载**：先取学期列表，点到哪个学期才拉哪个。十来个个学期若一次全拉，
要对超星发 30 多次请求，既慢又容易触发风控。每个学期结果缓存 10 分钟。

**快照兜底**：每次成功查询会把结果写入 `snapshots/<学号>/<学期>.json`。
超星不可用或令牌失效时自动回落快照，响应里带 `offline: true`，
前端会显示“当前展示的是离线缓存数据”。**这是为了避免演示当天因滑块识别失败而白屏。**

**绩点自己算**：超星 `getStudentAvgPoint` 返回的 `avgPoint` 实测恒为 `0.00`（学校没启用），
所以平均绩点按学分加权自行计算，否则页面上永远是 0。

## 五、凭据保存位置与持久化（默认关闭）

**默认情况下：磁盘上没有任何凭据。**

| 东西 | 存哪 |
|---|---|
| 超星 cookie、shelf token | **仅服务进程内存**（`app.py` 的 `SESSIONS`），服务重启即失效 |
| 学号和密码 | **不保存**。只在登录那一次用于换取 cookie，用完即弃 |
| 前端会话令牌 | 浏览器 `sessionStorage`，只是随机 uuid，不含超星凭据 |
| 成绩数据 | `snapshots/<学号>/<学期>.json`，**只有成绩，没有凭证** |

### 开启持久化以跳过滑块

滑块验证码**只在 CAS 密码登录这一步需要**。开启持久化后，服务会保存门户 cookie 与 shelf token，
下次登录时直接复用，**整条 CAS 登录与滑块流程都不会触发**。

实测有效期（来自真实抓包数据）：

- shelf token（成绩域 JWT）：**24 小时**
- 门户 cookie：`token` 这个 JWT 没有 `exp`，由服务端会话与 TGC 控制，通常更久

shelf token 过期但门户 cookie 仍有效时，服务会用 cookie 自动续期——**这条路同样不需要滑块**。

```powershell
$env:GRADE_PERSIST_CREDENTIAL = "1"
.\start.cmd
```

**代价与边界**：凭据以明文 bearer 形式落盘到 `.credentials/<学号>.json`，它等价于你账号的访问权。
文件已加入 `.gitignore`，请勿提交、勿公开。若要部署到服务器，请保持该开关关闭。
点击页面上的「退出超星账号」会同时删除落盘凭据。

## 六、已知限制

- **滑块识别率约 60%**，靠换图重试（默认最多 8 次，可用 `GRADE_CAPTCHA_ATTEMPTS` 调整）。
  连续失败时前端会提示重试，这是超星风控环节，无法做到 100% 成功。
- **单账号形态**：会话存在内存里，服务重启即失效，需要重新登录超星。
- **学号密码不落盘**，仅用于本次会话换取超星登录态；但**会话期间凭据以 cookie 形式驻留内存**。
- 会话与快照都在本机，**没有做多用户隔离与权限控制**，仅适合本地演示，不要直接暴露到公网。

## 七、环境变量

| 变量 | 默认 | 说明 |
|---|---|---|
| `GRADE_CAPTCHA_ATTEMPTS` | 8 | 滑块换图重试次数 |
| `GRADE_PERSIST_CREDENTIAL` | 0（关闭） | 置 1 时把超星凭据落盘以跳过滑块 |
| `GRADE_CREDENTIAL_DIR` | `./.credentials` | 凭据目录 |
| `GRADE_SESSION_TTL` | 7200 | 会话闲置过期秒数 |
| `GRADE_CACHE_TTL` | 600 | 单学期结果缓存秒数 |
| `GRADE_SNAPSHOT_DIR` | `./snapshots` | 快照目录 |
