# CLAUDE.md

本项目的完整说明在 **[AGENTS.md](AGENTS.md)**，请先读那一份。这里只保留最短索引，避免两份文档各自漂移。

- 项目：校园论坛（毕业设计），Spring Boot 3 + Vue 3，另含独立 FastAPI 成绩查询服务
- 仓库根：本文件所在目录；后端 `my-project-backen`（:8080）、前端 `my-project-fronted`（:5173）、成绩 `grade-service`（:8000）
- 本机工具链不在 PATH：JDK 用 `~/.jdks/graalvm-jdk-17.0.12`，Maven 用 IDEA 自带的 `plugins/maven-plugin/lib/maven3`，完整命令见 AGENTS.md 1.1
- 基础设施（MySQL / Redis 6385 / RabbitMQ / MinIO）已在 Docker 运行，**不要重复 `docker compose up`**
- `walkthrough.md` 是旧版架构分析，只当论文素材，不作为运行依据
