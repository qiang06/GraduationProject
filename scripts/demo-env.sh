#!/usr/bin/env bash
# 演示环境一键管理：启动 / 停止 / 状态 / 数据库快照 / 恢复 / 打开浏览器
#
#   bash scripts/demo-env.sh start      # 起后端 8080、前端 5173、成绩 8000
#   bash scripts/demo-env.sh status     # 端口 + 健康检查 + 演示数据条数
#   bash scripts/demo-env.sh stop       # 停掉本脚本启动的进程
#   bash scripts/demo-env.sh restart
#   bash scripts/demo-env.sh snapshot   # 备份当前数据库到 /data/demo-video/backup
#   bash scripts/demo-env.sh restore    # 恢复最近一次备份（或指定文件）
#   bash scripts/demo-env.sh open       # 用独立 Chrome 配置打开前端
#
# 说明：
# - 基础设施（MySQL/Redis/RabbitMQ/MinIO）由 Docker 提供，本脚本不启停它们。
# - 日志和 PID 在 ~/.cache/demo-video/run，录像出问题先看这里的 *.log。
# - 前端必须用 dev server：生产 dist 没有 /grade 代理和 /images 反代。
set -uo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
# 本地敏感配置（MAIL_* / WEATHER_API_KEY）放在根目录 .env，已在 .gitignore 中
if [[ -f "$ROOT/.env" ]]; then
    set -a
    # shellcheck disable=SC1090
    source "$ROOT/.env"
    set +a
fi
RUN_DIR="${DEMO_RUN_DIR:-$HOME/.cache/demo-video/run}"
BACKUP_DIR="${DEMO_BACKUP_DIR:-/data/demo-video/backup}"
JAVA="${DEMO_JAVA:-$HOME/.jdks/graalvm-jdk-17.0.12/bin/java}"
NODE="${DEMO_NODE:-$HOME/.nvm/versions/node/v22.23.3/bin/node}"
DB_PASS="${MYSQL_ROOT_PASSWORD:-root}"
CHROME_PROFILE="${DEMO_CHROME_PROFILE:-$HOME/.cache/demo-chrome}"

mkdir -p "$RUN_DIR" 2>/dev/null || { echo "无法创建 $RUN_DIR" >&2; exit 1; }
PID_DIR="$RUN_DIR"

say()  { printf '\033[1;36m[demo]\033[0m %s\n' "$*"; }
warn() { printf '\033[1;33m[demo]\033[0m %s\n' "$*"; }
die()  { printf '\033[1;31m[demo]\033[0m %s\n' "$*" >&2; exit 1; }

port_open() { (exec 3<>"/dev/tcp/127.0.0.1/$1") >/dev/null 2>&1; }

http_code() {
    local c
    c="$(curl -s -o /dev/null -m 3 -w '%{http_code}' "$1" 2>/dev/null)"
    [[ -n "$c" ]] || c=000
    printf '%s' "$c"
}

# 监听该端口的进程 pid（pid 文件丢了 / setsid 中转时兜底）
port_pid() {
    ss -H -ltnp "sport = :$1" 2>/dev/null | grep -o 'pid=[0-9]*' | head -1 | cut -d= -f2
}

# 服务就绪后，把真正的监听进程 pid 写回 pid 文件
resolve_pid() { # 名称, 端口；只处理本次脚本启动的服务
    [[ -f "$PID_DIR/$1.started" ]] || return 0
    local p
    p="$(port_pid "$2")"
    [[ -n "$p" ]] && echo "$p" >"$PID_DIR/$1.pid"
}

wait_http() { # url, 期望码, 超时秒, 名称
    local url="$1" want="$2" timeout="${3:-90}" name="$4" i=0 code
    while (( i < timeout )); do
        code="$(http_code "$url")"
        [[ "$code" == "$want" ]] && { say "$name 就绪（HTTP $code）"; return 0; }
        sleep 1; (( i++ ))
    done
    warn "$name 未在 ${timeout}s 内就绪（最后一次 HTTP $code），看 $PID_DIR/*.log"
    return 1
}

start_backend() {
    port_open 8080 && { say "后端 8080 已在运行，跳过"; return 0; }
    [[ -x "$JAVA" ]] || die "找不到 Java：$JAVA"
    local jar
    jar="$(ls -t "$ROOT"/my-project-backen/target/*.jar 2>/dev/null | head -1)"
    [[ -n "$jar" ]] || die "没有构建产物，先跑：cd my-project-backen && mvn -DskipTests package"
    say "启动后端：$(basename "$jar")"
    ( cd "$ROOT/my-project-backen" && setsid nohup "$JAVA" -jar "$jar" >"$PID_DIR/backend.log" 2>&1 & echo $! >"$PID_DIR/backend.pid" )
    : >"$PID_DIR/backend.started"
}

start_frontend() {
    port_open 5173 && { say "前端 5173 已在运行，跳过"; return 0; }
    [[ -x "$NODE" ]] || die "找不到 Node：$NODE"
    say "启动前端 dev server"
    ( cd "$ROOT/my-project-fronted" && setsid nohup "$NODE" node_modules/vite/bin/vite.js \
        --host 127.0.0.1 --port 5173 >"$PID_DIR/frontend.log" 2>&1 & echo $! >"$PID_DIR/frontend.pid" )
    : >"$PID_DIR/frontend.started"
}

start_grade() {
    port_open 8000 && { say "成绩服务 8000 已在运行，跳过"; return 0; }
    [[ -x "$ROOT/grade-service/start.sh" ]] || die "找不到 grade-service/start.sh"
    say "启动成绩服务（FastAPI）"
    ( cd "$ROOT/grade-service" && setsid nohup ./start.sh >"$PID_DIR/grade.log" 2>&1 & echo $! >"$PID_DIR/grade.pid" )
    : >"$PID_DIR/grade.started"
}

stop_one() { # 名称, 端口
    local name="$1" port="$2" pidfile="$PID_DIR/$1.pid" pid="" killed=""
    [[ -f "$pidfile" ]] && pid="$(cat "$pidfile" 2>/dev/null)"

    if [[ -n "$pid" ]] && kill -0 "$pid" 2>/dev/null; then
        kill -TERM "$pid" 2>/dev/null
        killed="$pid"
        for _ in $(seq 1 15); do kill -0 "$pid" 2>/dev/null || break; sleep 1; done
        kill -0 "$pid" 2>/dev/null && kill -KILL "$pid" 2>/dev/null
    fi
    rm -f "$pidfile" "$PID_DIR/$name.started"

    # 端口还在：setsid 中转或 pid 文件过期，按端口找真正监听的进程
    if port_open "$port"; then
        sleep 1
        pid="$(port_pid "$port")"
        if [[ -n "$pid" ]]; then
            warn "$name 仍占用 $port，按端口找到 pid $pid：$(ps -p "$pid" -o cmd= | cut -c1-90)"
            kill -TERM "$pid" 2>/dev/null
            killed="$pid"
            for _ in $(seq 1 10); do kill -0 "$pid" 2>/dev/null || break; sleep 1; done
            kill -0 "$pid" 2>/dev/null && kill -KILL "$pid" 2>/dev/null
        fi
    fi

    if port_open "$port"; then
        warn "$name 端口 $port 仍在监听，请手动确认（可能不是本项目的进程）"
    elif [[ -n "$killed" ]]; then
        say "已停止 $name（pid $killed）"
    else
        say "$name 未运行"
    fi
}

cmd_start() {
    rm -f "$PID_DIR"/*.started
    start_backend; start_frontend; start_grade
    wait_http "http://127.0.0.1:8080/api/forum/types" 200 90 "后端" || true
    wait_http "http://127.0.0.1:5173/" 200 40 "前端" || true
    wait_http "http://127.0.0.1:8000/grade/api/health" 200 40 "成绩服务" || true
    resolve_pid backend 8080
    resolve_pid frontend 5173
    resolve_pid grade 8000
    say "前端：http://127.0.0.1:5173/    管理端：/admin    成绩：/index/grade"
    say "日志目录：$PID_DIR"
}

cmd_stop() {
    stop_one grade 8000
    stop_one frontend 5173
    stop_one backend 8080
}

cmd_status() {
    local code
    printf '\n== 三个服务 ==\n'
    for pair in "后端 8080 http://127.0.0.1:8080/api/forum/types" \
                "前端 5173 http://127.0.0.1:5173/" \
                "成绩 8000 http://127.0.0.1:8000/grade/api/health"; do
        set -- $pair
        code="$(http_code "$3")"
        printf '  %-6s :%-5s HTTP %s\n' "$1" "$2" "$code"
    done
    printf '\n== 基础设施 ==\n'
    docker ps --format '  {{.Names}}\t{{.Status}}' 2>/dev/null | grep -E 'mysql8|redis_5.0|rabbitmq|minio' || warn "docker 不可用？"
    printf '\n== 演示数据 ==\n'
    if port_open 3306; then
        docker exec mysql8 mysql -uroot -p"$DB_PASS" --default-character-set=utf8mb4 -N -e "
            select concat('  账号 ', count(*)) from test.db_account
            union all select concat('  帖子 ', count(*)) from test.db_topic
            union all select concat('  评论 ', count(*)) from test.db_topic_comment
            union all select concat('  分类 ', count(*)) from test.db_topic_type
            union all select concat('  未读通知 ', count(*)) from test.db_notification where is_read=0
            union all select concat('  课程 ', count(*)) from test.db_course
            union all select concat('  图书馆 ', count(*)) from test.db_library_resource;" 2>/dev/null
    else
        warn "MySQL 3306 不通"
    fi
    printf '\n== 磁盘 ==\n'
    df -h / /data | sed 's/^/  /'
}

cmd_snapshot() {
    mkdir -p "$BACKUP_DIR" 2>/dev/null || die "无法创建 $BACKUP_DIR（/data 不可写？改成 DEMO_BACKUP_DIR=~/demo-backup）"
    local file="$BACKUP_DIR/test-$(date +%Y%m%d-%H%M%S).sql"
    say "备份数据库到 $file"
    docker exec mysql8 mysqldump -uroot -p"$DB_PASS" --single-transaction --routines --triggers \
        --default-character-set=utf8mb4 test >"$file" || die "备份失败"
    ls -lh "$file" | sed 's/^/  /'
    find "$BACKUP_DIR" -name 'test-*.sql' -type f | sort | head -n -5 | xargs -r rm -f
}

cmd_restore() { # 可选：备份文件路径
    local file="${2:-}"
    if [[ -z "$file" ]]; then
        file="$(find "$BACKUP_DIR" -name 'test-*.sql' -type f 2>/dev/null | sort | tail -1)"
    fi
    [[ -n "$file" && -f "$file" ]] || die "找不到备份文件"
    warn "即将用 $file 覆盖当前 test 库，Ctrl+C 取消；5 秒后继续"
    sleep 5
    docker exec -i mysql8 mysql -uroot -p"$DB_PASS" --default-character-set=utf8mb4 test <"$file" \
        && say "恢复完成（后端无需重启；列表缓存最多 2 分钟后自然过期）"
}

cmd_open() {
    port_open 5173 || die "前端没起，先 demo-env.sh start"
    say "独立 Chrome 配置：$CHROME_PROFILE"
    google-chrome \
        --user-data-dir="$CHROME_PROFILE" \
        --no-first-run --no-default-browser-check \
        --disable-features=Translate \
        --start-maximized \
        "http://127.0.0.1:5173/" >/dev/null 2>&1 &
    say "已打开；建议 F11 全屏，页面缩放 125%–150%"
}

case "${1:-}" in
    start)    cmd_start ;;
    stop)     cmd_stop ;;
    restart)  cmd_stop; sleep 2; cmd_start ;;
    status)   cmd_status ;;
    snapshot) cmd_snapshot ;;
    restore)  cmd_restore "$@" ;;
    open)     cmd_open ;;
    *) sed -n '2,16p' "$0"; exit 1 ;;
esac
