# 校园论坛

Spring Boot 3 + Vue 3 的校园论坛项目，包含 JWT 登录、帖子/评论、点赞、收藏、消息通知、Redis 限流、RabbitMQ 邮件和 MinIO 图片存储。

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

已有项目数据库只执行增量脚本：

`my-project-backen/src/main/resources/p1-incremental.sql`

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
- 论坛：分类、分页列表、标题搜索、发帖、编辑、删除、详情
- 评论：主评论、一级回复、权限删除、分页
- 互动：点赞、收藏及计数
- 通知：列表、未读数、单条已读、全部已读
- 基础设施：Redis 限流、RabbitMQ 邮件、MinIO 图片

## 测试账号

不要把真实密码提交到仓库。测试账号可以通过现有注册流程创建，或在本地数据库中手动创建 BCrypt 密码记录。

## 验证清单

- `mvn test`
- `mvn package`
- `npm run build`
- 启动 MySQL/Redis 后验证登录、分页、发帖、评论
- 执行 P1 增量 SQL 后验证搜索、点赞、收藏、通知
