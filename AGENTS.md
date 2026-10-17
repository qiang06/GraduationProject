# AGENTS.md

> 校园论坛（毕业设计）——Spring Boot 3 + Vue 3 全栈项目，另含独立的 FastAPI 成绩查询服务。
>
> **本文件是本项目在当前 Linux 机器上的唯一权威说明**（2026-10-17 重写，取代 Windows 时期旧版）。
> `CLAUDE.md` 只是指向本文件的索引；`walkthrough.md` 是旧的架构分析（缺评论/点赞/收藏/通知/成绩查询），
> 只能当论文素材，**不要当运行依据**。与代码冲突时，以代码、`docker ps`、`git log` 的实际状态为准。

## 0. 项目定位

- 题目：**基于 Spring Boot 3 + Vue 3 的校园论坛系统设计与实现**
- 学生：侯学强（20223334）／物联网应用与技术；指导教师待填
- 任务书进度：开题 9.16–9.23 → 规划 9.23–10.13 → 实施 10.14–11.12 → 完善 11.13–11.23
- 仓库根 = 本目录；远程 `https://github.com/qiang06/GraduationProject.git`（分支 `master`）
- 开题报告范围：注册登录、资料与隐私、帖子、评论回复、点赞收藏、消息通知
- 实际多做：**超星成绩查询**（`grade-service` + `/index/grade`）、**管理端**（`/admin`）。开题报告里没有，答辩按「扩展模块」讲
- 三个可独立启动的部分：论坛后端 `:8080`、论坛前端 `:5173`、成绩服务 `:8000`

## 1. 在这台机器上跑起来

### 1.1 工具链（重要：java / mvn 不在 PATH）

| 工具 | 位置 | 说明 |
|---|---|---|
| JDK 17 | `~/.jdks/graalvm-jdk-17.0.12` | 系统没有装 `java`；这是 GraalVM 17 |
| Maven 3.9.16 | `<IDEA>/plugins/maven-plugin/lib/maven3` | 系统没有装 `mvn`，用 IDEA 自带的 |
| Node 22.23.3 / npm 10.9.9 | `~/.nvm/versions/node/v22.23.3/bin` | 已在 PATH |
| Python 3.12.3 | `/usr/bin/python3` | 成绩服务用自带 `.venv` |

```bash
# 终端里构建/测试前先执行（IDEA 路径随版本变化，升级 IDEA 后要同步改）
export JAVA_HOME="$HOME/.jdks/graalvm-jdk-17.0.12"
export IDEA_HOME="$HOME/.local/opt/idea-IU-262.10968.63"
export PATH="$JAVA_HOME/bin:$IDEA_HOME/plugins/maven-plugin/lib/maven3/bin:$PATH"
java -version && mvn -v
```

在 IDEA 里直接 Run 也可以（Project SDK = 17，指向上述 GraalVM）。

### 1.2 基础设施：已经在跑，不要重复 `up`

当前容器来自 `~/forum/docker-compose.yml`（compose project `forum`），数据从 Windows 迁移而来：

| 容器 | 端口 | 数据目录 | 不能动的点 |
|---|---|---|---|
| `mysql8` | 3306 | `/data/mysql-ci/mysql` | 必须 `--lower_case_table_names=2`，改了会 MY-011087 拒绝启动 |
| `redis_5.0` | 6385 | `/data/docker-data/redis_5.0` | 论坛连的是这个；`redis`(6379)、`redis_3.2`(6382) 是历史遗留 |
| `rabbitmq` | 5672 / 15672 | `/data/docker-data/rabbitmq` | hostname 固定 `f1ee050e5079`，改了 mnesia 不认旧队列 |
| `minio` | 9000 / 9001 | `/data/docker-data/miniodata` | bucket：`study`（图片）、`avatar`、`test` |

- 本目录的 `docker-compose.yml` 是可移植版本，端口与上面**完全相同**。基础设施在跑时再 `docker compose up` 会端口冲突。
- 数据库 `test` 已有演示数据：4 账号 / 20 帖 / 37 评论 / 9 赞 / 7 收藏 / **13 分类**（含失物招领/校园活动/表白墙，以及用户自建的 java 分类）。
- 确实要重启：`cd ~/forum && docker compose up -d`（数据在 `/data`，不会丢）。

### 1.3 启动顺序

```bash
# 1) 后端（先 export 1.1 里的 JAVA_HOME / PATH）
cd my-project-backen && mvn spring-boot:run          # :8080

# 2) 前端
cd my-project-fronted && npm install && npm run dev   # :5173

# 3) 成绩查询（可选，只影响 /index/grade）
cd grade-service && ./start.sh                        # :8000
```

### 1.4 健康检查

```bash
curl -s http://127.0.0.1:8080/api/forum/types        # 401 + RestBean JSON = 后端活着（未带 token 就该 401）
curl -s http://127.0.0.1:5173/ -o /dev/null -w '%{http_code}\n'
curl -s http://127.0.0.1:8000/grade/api/health       # {"ok":true,"ddddocr":true,...}
```

## 2. 目录

```
my-project/                         ← git 仓库根（本目录）
├── my-project-backen/              Spring Boot 3.5 后端
├── my-project-fronted/             Vue 3 + Vite 前端
├── grade-service/                  FastAPI 超星成绩服务（自带 .venv，不进 git）
├── docker-compose.yml              可移植基础设施（当前未用于运行，见 1.2）
├── .env.example                    后端环境变量样例
├── test.sql / p1-p4 增量          见第 4 节
├── AGENTS.md / CLAUDE.md           本文档 / 指向本文档
└── walkthrough.md                  旧版架构分析（不作为运行依据）
```

## 3. 后端（`my-project-backen/`）

### 3.1 分层

```
com.myproject
├── MyProjectBackenApplication.java
├── config/       Security（含 login/logout 处理器）、Rabbit、MinIO、MyBatis、Web
├── controller/   Authorize、Account、Forum、Interaction、Notification、Image、Object、Ip、Test(空)、exception/、admin/
├── entity/       BaseData、RestBean、dto/、vo/request/、vo/response/
├── filter/       CorsFilter(-102) → FlowLimitingFilter(-101) → JwtAuthorizeFilter
├── listener/     MailQueueListener（RabbitMQ 发信）
├── mapper/       MyBatis-Plus 接口
├── service/      接口 + impl/
└── utils/        Const、JwtUtils、FlowUtils、RedisCacheUtils、ContentSanitizer、ContentPreview、ControllerUtils
```

### 3.2 统一响应 `RestBean<T>`

所有接口返回 `{ "code": 200, "data": ..., "message": "..." }`。
`200` 成功、`400` 参数/业务错误、`401` 未认证、`403` 无权限、`429` 限流。
认证、限流、异常处理器里都是直接写 `RestBean.xxx(...).asJsonString()` 到 response。

### 3.3 DTO ↔ VO

DTO 实现 `BaseData`，`asViewObject(VO.class)` 用反射**只拷贝同名字段**：
VO 必须有无参构造函数，字段缺失静默跳过，不做类型转换。新增字段两边都要加。

### 3.4 JWT

1. `POST /api/auth/login`（表单）→ Spring Security 认证 → 签 HMAC256，claims `id/name/authorities`，UUID 作 JTI
2. 返回 `AuthorizeVO{token, expire}`，前端存 localStorage（记住我）或 sessionStorage
3. 后续请求 `Authorization: Bearer <token>`，`JwtAuthorizeFilter` 解析后写 `SecurityContext` 和 `request.attribute("userId")`
4. 登出：把 JTI 按剩余 TTL 写入 Redis `jwt:blacklist:<uuid>`
5. **`spring.security.jwt.expire` 单位是天**（`Calendar.add(HOUR, expire*24)`），默认 7 → 7 天

账号可用**用户名或邮箱**登录（`loadUserByUsername` → `findAccountByNameOrEmail`）。

### 3.5 权限

- 公开：`/api/auth/**`、`/error`、`/images/**`、`/swagger-ui/**`、`/v3/api-docs/**`
- `/api/admin/**` 需要 `ROLE_admin`（字符串 `admin`），其余需要 `ROLE_user`；管理员也能使用普通用户接口
- 登录时被封禁的账号（`db_account.banned=1`）直接返回 403
- **Swagger 实际不可用**：pom 没有 springdoc 依赖，访问 `/v3/api-docs` 是 404。别在论文里写「已集成 Swagger」

### 3.6 限流

`FlowLimitingFilter` 对所有方法计数：默认 2 秒窗口 50 次，超限封 30 秒（Redis `flow:counter:` / `flow:block:`）。
`spring.web.flow.trust-proxy` 默认 false，只有部署在可信反代后面才可打开，否则客户端能伪造 `X-Forwarded-For` 绕过。
另有业务级频率限制：发帖 10 秒/次、评论 3 秒/次（`limitOnceCheck`），图片上传 20 次/小时，验证码邮件 60 秒。

### 3.7 Redis key

`jwt:blacklist:`、`jwt:frequency:`、`flow:counter:`、`flow:block:`、`verify:email:limit:`、`verify:email:data:`、
`weather:cache:`、`forum:image:`、`forum:topic:create:`、`forum:topic:comment:`、`topic:preview:`（帖子列表缓存 2 分钟）。

## 4. 数据库

10 张表，实体都在 `entity/dto/`：

| 表 | 实体 | 用途 |
|---|---|---|
| `db_account` | Account | 账号：username / password(BCrypt) / email / role / **mute / banned** / avatar / register_time |
| `db_account_details` | AccountDetails | 性别、手机、QQ、微信、简介 |
| `db_account_privacy` | AccountPrivacy | 字段级隐私开关（boolean，false = 隐藏） |
| `db_image_store` | StoreImage | 图片元数据（uid / name / time） |
| `db_notification` | Notification | 通知（含 `is_read`） |
| `db_topic` | Topic | 帖子（title / content / **extra(JSON)** / uid / type / time / top / **locked / invisible**） |
| `db_topic_comment` | Comment | 评论（`quote` 为直接父评论，`root_id` 为顶层评论；多级树） |
| `db_topic_interact_like` | TopicInteractLike | 点赞，`(tid,uid)` 唯一 |
| `db_topic_interact_collect` | TopicInteractCollect | 收藏，`(tid,uid)` 唯一 |
| `db_topic_type` | TopicType | 分类（**code / anonymous** / name / desc / color） |

脚本：

- `test.sql`：**全量建表**，含 `DROP TABLE`。只用于空库，别在已有数据的库上跑
- `p1-incremental.sql`：老库增量（建点赞/收藏/通知表，`IF NOT EXISTS`）
- `p2-topic-extra.sql`：失物招领/校园活动/表白墙的增量（`db_topic.extra`、`db_topic_type.code/anonymous`、两个新分类）。**只执行一次**，执行前会自动建 `db_topic_bak_p2`、`db_topic_type_bak_p2` 两张备份表
- `p3-admin.sql`：管理端字段（`db_topic.locked/invisible`、`db_account.mute/banned`），并把 1 号账号设为管理员。**只执行一次**，会建 `db_topic_bak_p3`、`db_account_bak_p3` 备份表
- `p4-comment-tree.sql`：多级评论（`db_topic_comment.root_id` + 索引）。**只执行一次**，会建 `db_topic_comment_bak_p4`；同时兼容旧数据 `quote=-1`、父评论已丢失的孤儿回复，并递归回填 `root_id`
- 老库若 `db_notification` 缺列：`ALTER TABLE db_notification ADD COLUMN is_read TINYINT(1) NOT NULL DEFAULT 0 AFTER time;`
- `test.sql` 现在自带 12 个分类的种子数据；`p1`/`p2` 是纯增量脚本。新环境跑完 `test.sql` 就能直接发帖
- 没有 Flyway/Liquibase。容器的 `lower_case_table_names=2` 是**启动硬条件**（迁移数据的数据字典要求），不要改

## 5. 接口清单

### 认证 `/api/auth`（公开）

| 方法 | 路径 | 说明 |
|---|---|---|
| POST | `/login` | 表单 `username`+`password`，由 Spring Security 处理 |
| GET | `/logout` | JWT 进黑名单 |
| GET | `/ask-code?email=&type=` | `type` ∈ `register\|reset\|modify`，60 秒间隔 |
| POST | `/register` | `{username,email,password,code}` |
| POST | `/reset-confirm` | `{email,code}` 校验验证码 |
| POST | `/reset-password` | `{email,password,code}` |

### 用户 `/api/user`（需登录）

`GET /info`、`GET /details`、`POST /save-details`、`POST /modify-email`、`POST /change-password`、
`POST /save-privacy`、`GET /privacy`

### 论坛 `/api/forum`（需登录）

| 方法 | 路径 | 说明 |
|---|---|---|
| GET | `/weather?longitude=&latitude=` | 按坐标取天气，Redis 缓存 1 小时 |
| GET | `/weather/city?id=` | 按和风城市 ID 取天气（手动选城市用） |
| GET | `/weather/search?keyword=` | 城市检索，最多 10 条（中文/拼音都行） |
| GET | `/types` | 分类列表 |
| POST | `/create` | 发帖（正文经 ContentSanitizer 白名单过滤） |
| GET | `/list?page=&size=&type=&typeCode=&keyword=` | 分页 + 分类 + 标题搜索，结果缓存 2 分钟；`typeCode` 与 `type` 二选一 |
| GET | `/detail?id=` | 帖子详情（含作者/隐私过滤后的联系方式、分类、extra、locked/invisible、点赞/收藏数） |
| GET | `/collects` | 我的收藏，按收藏时间倒序 |
| GET | `/top` | 置顶帖子，最多 10 条 |
| POST | `/update`、`/delete` | 仅作者可改/删，删帖级联清评论与互动 |
| POST | `/status` | body `{id,status:open\|resolved}`，仅失物招领作者可标记「已找回」 |
| POST | `/comment` | 富文本评论/回复；`content` 为 HTML（服务端 sanitize），纯文本 ≤2000 字、HTML ≤20000 字符；`quote` 为直接父评论，最多 10 层 |
| GET | `/comments?tid=&page=&size=` | 顶层评论分页，返回多级 `replies` 树；`CommentPageVO` 的 `total` 是顶层数（分页用），`allTotal` 是含所有回复的总数；匿名帖里楼主的评论对别人显示「匿名同学」 |
| POST | `/comment-delete` | 评论作者或楼主可删 |

### 互动 `/api/forum/interaction`（需登录）

`GET ?tid=` 取计数与当前用户状态；`POST /like`、`POST /collect`（body `{id}`）切换，取消不通知，新增时给楼主发通知。

### 通知 `/api/notification`（需登录）

`GET /list?page=&size=`、`GET /unread`、`POST /read`（body `{id}`）、`POST /read-all`。
评论、回复、点赞、收藏会写 `db_notification`，并且不会给自己发。
管理端置顶/锁定/屏蔽、禁言/封禁会给作者/用户写一条 `system` 通知，消息列表里能看到。

### 图片

`POST /api/image/cache`（≤5MB）、`POST /api/image/avatar`（≤100KB），表单字段名 `file`；
返回 MinIO 相对路径。`GET /images/**` 从 MinIO 读并带 30 天 Cache-Control。
上传限流 20 次/小时，bucket `study`，路径 `/avatar/<uuid>` 或 `/cache/yyyyMMdd/<uuid>`。

### 管理端 `/api/admin/**`（需 ROLE_admin）

| 方法 | 路径 | 说明 |
|---|---|---|
| GET | `/forum/list?page=&size=&keyword=` | 全部帖子（含屏蔽帖，作者不匿名） |
| POST | `/forum/delete`、`/top`、`/locked`、`/invisible` | body `{id}` / `{tid,status}` |
| POST | `/forum/change-type` | body `{tid,type}` |
| POST | `/forum/type/create`、`/type/update`、`/type/delete` | 分类 CRUD；code 创建后不可改，分类下有帖子时不能删 |
| GET | `/user/list?page=&size=&keyword=` | 用户列表（含 mute/banned/role） |
| POST | `/user/save` | body `{id,role,mute,banned}`；不能封禁/降级当前管理员 |
| POST | `/user/change-password` | body `{id,newPassword}` |

### 其他

`GET /api/ip` 返回调用方 IP（前端首页显示用）；`TestController` 是空文件。

## 6. 前端（`my-project-fronted/`）

### 6.1 路由

`/`（WelcomeView）→ 登录 `/`、注册 `/register`、找回 `/forget`；
`/index`（IndexView）→ 帖子广场 `/index`、详情 `/index/topic/:id`、
失物招领 `/index/lost`、校园活动 `/index/activity`、表白墙 `/index/confession`、
我的收藏 `/index/collects`、成绩 `/index/grade`、通知 `/index/notifications`、
资料 `/index/user-setting`、安全 `/index/privacy-setting`。
三个分类页复用 `TopicList.vue`，标题/提示/发帖预设类型来自路由 `meta`。

管理端：`/admin`（默认跳 `/admin/forum`）、`/admin/forum`、`/admin/user`。

守卫：`/welcome*` 仅游客、`/index*` 仅登录、`/admin*` 要求 `isRoleAdmin()`（登录时存下的 role === 'admin'）。

### 6.2 网络层

- `src/net/index.js`：底层 `post/get`、`login`、`logout`、`unauthorized`、`isRoleAdmin`；`code=401` 自动清 token 并跳登录。
  登录时把 `role` 一起写进 localStorage/sessionStorage，做管理端守卫时不用再查一次用户信息
- `src/net/api/`：按模块封装的业务接口（`auth/user/forum/notification/admin`，由 `api/index.js` 统一导出），
  页面里只 import `@/net/api`，不要再把 `/api/...` 字符串散落在组件里
- `src/net/grade.js`：成绩服务专用，**故意不复用论坛 JWT**。超星的 401 不能把用户从论坛踢出去；
  用 `sessionStorage.gradeSession` + 请求头 `X-Grade-Session`
- `main.js`：`axios.defaults.baseURL = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080'`
- `store` 缓存 `forum.types`，`findTypeById()` / `avatarUserUrl()` 供 `TopicTag`、编辑器、列表共用

### 6.3 环境与构建

```bash
cp .env.example .env.local     # 需要改后端地址时
npm run dev                    # :5173
npm run build                  # dist/
```

- Vite 别名 `@` → `src/`
- `vite.config.js` 里 `/grade` 代理到 `127.0.0.1:8000`，**只在 dev server 生效**
- 生产构建产物没有 `/api`、`/images`、`/grade` 的反代，部署时要么 nginx 配三条，要么直接演示 `npm run dev`

### 6.4 组件

`Card`、`LightCard`、`ColorDot`、`TopicTag`、`TopicEditor`（VueQuill + 图片缩放/超分插件）、`Weather`。
管理端页面在 `views/admin/`：`AdminForum`（帖子 + 分类）、`ForumTopicAdmin`、`ForumTopicTypeAdmin`、`AdminUser`；布局在 `views/AdminView.vue`。
`TopicTag` 从 `store.forum.types` 取颜色，列表和详情统一用它，不要再写成灰色 `el-tag`。
帖子广场顶部有分类筛选 chips（「全部」+ `store.forum.types`），点击只改 `?type=id` 并复用同一个列表，不跳分类路由；
`/index/lost` 等分类路由仍保留各自的标题、提示和发帖预设类型。
「全部」是 **id=0 的伪分类**，由 `IndexView` 插在 `store.forum.types` 最前面，颜色是渐变
`linear-gradient(45deg, white, red, orange, gold, green, blue)`；`TopicEditor` 的下拉框必须过滤 `id > 0`，
否则「全部」会被当成可发布类型。
`GradeView.vue` 有 855 行，是最大的组件，改之前先读完整 script。
天气卡片右上角「切换城市」：搜索走 `/weather/search`，选择后走 `/weather/city`，结果记在 `forum:weatherCity`。
`App.vue` 用 `el-config-provider` 把 Element Plus 切成 zh-cn；`IndexView` 的 `router-view` 对 `TopicList` 开了 `keep-alive`（切分类页保留滚动/筛选状态），并加了 `el-backtop`。
详情页评论数用 `commentTotal`（即 `CommentPageVO.allTotal`，含所有层级回复），分页用 `commentRootTotal`（顶层评论数），不要用 `comments.length`（只是当前页条数）。
详情页左侧是作者面板：头像/用户名/性别/邮箱/微信/QQ/手机/简介；后端按 `db_account_privacy` 过滤后的 `null`
统一显示「已隐藏或未填写」（`HIDDEN_TEXT`），不要显示成空白。列表卡片左上角也放作者头像/名字/时间（jwt-1.0 风格）。
评论用 `CommentEditor.vue`（底部居中卡片，Quill HTML，和发帖共用 toolbar/图片上传）和 `CommentItem.vue`（递归组件，`replies` 嵌套）；
卡片桌面端 820×420、16px 圆角、距底部 24px，移动端宽度 `calc(100vw - 24px)`。
视觉缩进最多 3 层，第 4 层起不再缩进但保留「回复 @xxx」。顶层评论分页，子回复随顶层一次返回。

`index.html` 只额外引了和风天气图标字体（jsdelivr）；Element Plus 基础样式由 auto-import 注入，`main.js` 只引深色变量，**没有重复加载**。

## 7. 成绩服务（`grade-service/`）

独立进程，`:8000`，前端经 Vite 的 `/grade` 代理访问，同源无 CORS。

- 技术：FastAPI + requests + ddddocr（滑块识别）+ onnxruntime
- 登录：学号密码 → 过滑块（约 60% 识别率，默认换图重试 8 次）→ 换 shelf token（约 24h）
- 会话：自签随机 UUID，服务端内存 `SESSIONS`，请求头 `X-Grade-Session`，闲置 TTL 默认 7200 秒
- 懒加载：先 `/terms` 拿学期列表，点哪个拉哪个；单学期结果缓存 10 分钟
- 快照兜底：成功查询写 `snapshots/<学号>/<学期>.json`，超星挂了自动回落并返回 `offline: true`
- 平均绩点按学分加权自算（超星 `avgPoint` 实测恒 0）
- 凭据默认**不落盘**；`GRADE_PERSIST_CREDENTIAL=1` 才写 `.credentials/<学号>.json`（已 gitignore），用于跳过滑块
- 接口：`/grade/api/health`、`/login`、`/resume`、`/saved`、`/logout`、`/me`、`/terms`、`/term?term=`
- 环境变量：`GRADE_CAPTCHA_ATTEMPTS`、`GRADE_PERSIST_CREDENTIAL`、`GRADE_CREDENTIAL_DIR`、
  `GRADE_SESSION_TTL`、`GRADE_CACHE_TTL`、`GRADE_SNAPSHOT_DIR`
- **边界**：单账号、无多用户隔离、`/login` 不校验论坛身份，只适合本地演示，不能暴露公网

## 8. 已知坑 / 真实约束

1. **密钥默认值里有真实凭据**：`application.yml` 用 `${ENV:默认值}`，邮件授权码、天气 API key 的默认值是真的，
   且该文件已被 git 跟踪。答辩前建议把默认值换成占位符，并去 163 重置授权码
2. **Swagger 是 404**（见 3.5）
3. **JWT 过期单位是天**（见 3.4）
4. **评论最多 10 层**：第 11 层会被后端拒绝（`回复层级最多 10 层`）；视觉缩进最多 3 层
5. **正文经过 jsoup 白名单**：新增标签/属性要先改 `ContentSanitizer`，否则会被静默剥掉
6. **BaseData 反射**只按同名字段拷贝，源/目标都要有对应字段和无参构造
7. **天气**：和风返回 GZIP，`WeatherServiceImpl` 手动解压；`RestTemplate` 设了 3s 连接 / 5s 读取超时。
   浏览器定位（Chrome 的定位服务）在国内经常不可用，**不能依赖它**：前端天气卡片右上角有「切换城市」，
   选过的城市存 `localStorage.forum:weatherCity`，下次直接按 id 取；都没选时用 `DEFAULT_CITY`（当前北京）。
   注意和风 geoapi 的 `location` 参数**不要自己 URLEncoder**，RestTemplate 会对 URL 模板再编码一次，中文会变成 `%25E9...`
8. **头像 100KB / 图片 5MB** 是硬编码在 Controller 里的
9. **GraalVM native 插件**在 pom 里但没有 reachability metadata，不能真的 native 编译
10. **没有 mvnw**，且系统无 mvn（见 1.1）
11. **根目录没有 .gitignore**：`node_modules/`、`_piptest/`、`.vscode/`、`snapshots/` 等当前是未跟踪状态
12. **分类扩展字段**：`db_topic.extra` 只接受 `EXTRA_KEYS` 白名单里的键（失物招领 item/place/happenedAt/contact/status，校园活动 startAt/endAt/place/organizer），值会经 jsoup 去标签并截断到 120 字。新增分类字段要先改 `TopicPostServiceImpl.EXTRA_KEYS`，前端 `TopicEditor.vue` 的 `extraFields` 也要同步
13. **匿名**由 `db_topic_type.anonymous` 控制，不是把 uid 置空：列表里匿名帖的 uid 一律为 null；详情/评论只对非作者隐藏 uid，作者本人仍能编辑/删除。改这块时别把 uid 从数据库里弄丢
14. **改状态/编辑/删除要清 `topic:preview:` 缓存**，否则列表上的「已找回」和标题不会变
15. **管理端角色**：`p3-admin.sql` 把 1 号账号设为 admin。角色是在登录时写进前端 token 存储的，
   改完角色必须**重新登录**才会出现「管理后台」入口；把别的账号提权用
   `UPDATE db_account SET role='admin' WHERE username='你的用户名';`
16. **屏蔽/锁定**：普通列表、收藏、置顶都会排除 `invisible=1`；详情和评论对非作者返回「帖子不存在」，管理员例外；
   `locked=1` 禁止评论和编辑（作者也不能编辑），管理员在列表里可以解锁
17. **分类 code** 创建后不可改（前端编辑时禁用），分类下还有帖子时不能删除；管理端帖子列表会把匿名帖的真实作者显示出来（审核需要）
18. **db_notification.is_read 对应实体字段必须叫 isRead**，不能叫 read：MyBatis-Plus 会生成 is_read AS read，而 READ 是 MySQL 保留字，消息列表直接 500。VO 里仍叫 read，在 Service 里手动 set
19. **禁言/封禁的即时提示**：写操作（发帖、编辑、评论、删帖、失物招领状态）在 Service 里检查 mute/banned 并返回 400，前端 defaultFailure 会弹 toast；登录时被封禁会在 onAuthenticationSuccess 直接 403。封禁不会让已签发 token 立刻失效，但下一次写操作会拦下
20. **多级评论**：`db_topic_comment.quote` 是直接父评论，`root_id` 是顶层评论。顶层 `root_id=id`，回复继承父的 `root_id`；`listComments` 按顶层分页后一次取本页所有 `root_id` 的后代建树。删除父评论会删整棵子树，不做软删除
21. **p4 历史数据**：jwt-1.0 迁移过来的评论用 `quote=-1` 表示顶层，且存在父评论已被删除的孤儿回复；`p4-comment-tree.sql` 会把 `-1` 改成 `NULL`、把孤儿提升为顶层，并递归回填 `root_id`。旧评论内容可能是 Quill Delta JSON，`CommentServiceImpl.normalizeContent` 在读取时转成安全 HTML，新评论直接存 sanitize 后的 HTML
22. **评论计数**：`CommentPageVO.total` 是顶层评论数（分页），`allTotal` 是含所有回复的总数（页面标题「评论 (x)」用后者）。前端 `commentRootTotal`/`commentTotal` 不要混用

## 9. 构建与验证清单

```bash
# 后端
cd my-project-backen
mvn test                 # 29 个用例：PageVO、参数校验、XSS 白名单、正文摘要
mvn -DskipTests package  # target/*.jar

# 前端
cd my-project-fronted
npm run build            # 必须能过，这是答辩前的硬指标

# 冒烟
curl -s http://127.0.0.1:8000/grade/api/health
```

手工冒烟顺序：注册（需邮箱验证码）→ 登录 → 发帖（富文本+图片）→ 搜索 → 详情 → 评论/回复 → 点赞/收藏 →
换另一个账号验证通知 → 资料/隐私设置 → 我的收藏 → 成绩查询（可选）。

管理端：用 admin 账号登录 → 右上角「管理后台」→ 帖子置顶/锁定/屏蔽/删除/改分类 → 分类增删改 → 用户禁言/封禁/改密。
普通用户账号访问 `/admin` 会被路由守卫送回 `/index`，直接打 `/api/admin/**` 会 403。

2026-10-17 实测基线：`mvn test` 29/29 通过（14.6s）；`npm run build` 通过（8.1s，`element` chunk 约 940KB，有体积告警）；
P0 前端重构后（`net/api` 分层、store types、TopicTag、keep-alive/backtop、zh-cn、评论总数修复）构建仍通过。
`/grade/api/health` 返回 `{"ok":true,"ddddocr":true}`。
同日在 8081 单独起包冒烟 P2：失物招领 extra 保存/更新/已找回状态、表白墙列表与详情匿名、楼主评论对他人匿名、`typeCode=lost/confession` 筛选均通过（测试帖已删除，数据回到 19 帖/27 评论/0 通知）。
P1/P2 冒烟：`/api/forum/collects`、`/api/forum/top`、详情 author 隐私过滤；管理端帖子/分类/用户 CRUD、置顶/锁定/屏蔽、禁言、非管理员 403 全部通过。
消息列表 500 已修（isRead + MySQL 保留字）；管理端置顶/锁定/屏蔽/禁言会写 system 通知；禁言/封禁后写操作会返回明确提示。
评论已改富文本 + 多级：HTML sanitize、2000 字/20000 字符、最多 10 层、视觉 3 层、删除子树、旧 Delta/纯文本兼容。
`mvn test` 29/29；`npm run build` 通过（9.2s）；headless Chrome 冒烟通过（详情/作者面板/评论树/抽屉/回复标题，无 console error）。

## 10. 仓库卫生

- 当前工作区有大量未提交改动（`git status` 自查），演示前建议做一次干净提交
- 建议在仓库根加 `.gitignore`：`node_modules/`、`_piptest/`、`.vscode/`、`*.iml`、`.env*`（保留 `.example`）、
  `grade-service/.credentials/`、`grade-service/snapshots/`
- `application.yml` 的凭据外部化后再提交，或至少确认没有新增明文
- `p2`/`p3` 迁移会建 `db_*_bak_p2`、`db_*_bak_p3` 备份表，确认数据无误后可删
