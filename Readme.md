# 校园论坛

Spring Boot 3 + Vue 3 的校园论坛项目，包含 JWT 登录、帖子/评论、点赞、收藏、消息通知、Redis 限流、RabbitMQ 邮件和 MinIO 图片存储。

帖子广场之外还提供三个复用同一内容模型的分类模块：**失物招领**（结构化字段 + 已找回状态）、**校园活动**（时间/地点/主办方）、**表白墙**（匿名发布，作者以外看不到用户名）。
另有「我的收藏」和 `/admin` 管理端（帖子置顶/锁定/屏蔽/删除、分类增删改、用户禁言/封禁/改密）。

## 启动依赖

推荐使用根目录 Compose 启动基础设施：

```bash
docker compose up -d mysql redis rabbitmq minio
```

也可以手动启动：

- MySQL: `localhost:3306`, database `test`
- Redis: `localhost:6385`
- RabbitMQ: `localhost:5672`
- MinIO: `localhost:9000`

## 数据库

新数据库先执行 `my-project-backen/src/main/resources/test.sql`。

已有项目数据库按顺序执行增量脚本：

1. `my-project-backen/src/main/resources/p1-incremental.sql`
2. `my-project-backen/src/main/resources/p2-topic-extra.sql`（失物招领/校园活动/表白墙；**只执行一次**，会先建两张备份表）
3. `my-project-backen/src/main/resources/p3-admin.sql`（管理端字段；**只执行一次**，会先把 1 号账号设为管理员）
4. `my-project-backen/src/main/resources/p4-comment-tree.sql`（多级评论 `root_id`；**只执行一次**，会建 `db_topic_comment_bak_p4`，并兼容 `quote=-1` 的旧数据）

管理端入口 `/admin` 需要 `db_account.role = 'admin'`。把自己提权：

```sql
UPDATE db_account SET role = 'admin' WHERE username = '你的用户名';
```

改完角色要重新登录，前端才会出现「管理后台」入口。

如果旧的 `db_notification` 表没有 `is_read` 字段，再执行：

```sql
ALTER TABLE db_notification ADD COLUMN is_read TINYINT(1) NOT NULL DEFAULT 0 AFTER time;
```

不要在已有数据的环境直接执行 `test.sql`，它包含 DROP TABLE。

## 后端

```bash
cd my-project-backen
mvn test
mvn spring-boot:run
```

后端配置在 `src/main/resources/application.yml`，敏感配置支持环境变量覆盖。可复制根目录 `.env.example` 并在启动进程中导出对应变量。

## 前端

```bash
cd my-project-fronted
npm install
npm run dev
```

复制 `.env.example` 为 `.env.local`，通过 `VITE_API_BASE_URL` 配置后端地址。生产构建：`npm run build`。

## 已实现接口范围

- 认证：登录、注册、验证码、重置密码、退出
- 用户：资料、隐私、邮箱、密码、头像
- 论坛：分类、无限滚动列表、标题搜索、发帖、编辑、删除、详情
- 分类模块：失物招领（物品/地点/联系方式 + 已找回状态）、校园活动（时间/地点/主办方）、表白墙（匿名，含楼主评论匿名）
- 我的收藏：按收藏时间倒序，可取消收藏
- 详情页：左侧作者面板（联系方式按隐私设置过滤，隐藏/未填写统一提示）、置顶/锁定/已找回标签、作者卡片、标题滚出视口后的吸顶导航条
- 管理端：帖子列表/搜索/置顶/锁定/屏蔽/删除/改分类；分类增删改（code/颜色/匿名）；用户列表/禁言/封禁/改密
- 评论：Quill 富文本（格式/图片）、多级回复（最多 10 层，视觉缩进 3 层）、权限删除（父评论删整棵子树）、顶层分页 + 全部评论数、旧 Delta/纯文本兼容
- 互动：点赞、收藏及计数
- 通知：列表、未读数、单条已读、全部已读；管理端置顶/锁定/屏蔽/禁言会写系统通知
- 基础设施：Redis 限流、RabbitMQ 邮件、MinIO 图片

## 测试账号

不要把真实密码提交到仓库。测试账号可以通过现有注册流程创建，或在本地数据库中手动创建 BCrypt 密码记录。

## 验证清单

- `mvn test`
- `mvn package`
- `npm run build`
- 启动 MySQL/Redis 后验证登录、分页、发帖、评论
- 执行 P1 增量 SQL 后验证搜索、点赞、收藏、通知
- 执行 P2 增量 SQL 后验证失物招领 extra 与「已找回」、活动字段、表白墙匿名
- 执行 P3 增量 SQL 后验证 `/admin` 帖子/分类/用户管理，普通用户访问 `/api/admin/**` 返回 403
- 执行 P4 增量 SQL 后验证多级评论、10 层上限、父评论删除整棵子树、旧评论 Delta 正常显示
