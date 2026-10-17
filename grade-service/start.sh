#!/usr/bin/env bash
# 启动成绩查询服务（FastAPI），监听 127.0.0.1:8000。
# 前端 Vite 通过 /grade 代理到这里，同源、无 CORS。
#
# 依赖装在项目内的 .venv（不进 git）。首次准备：
#     python3 -m venv --without-pip .venv
#     curl -sSL https://bootstrap.pypa.io/get-pip.py -o /tmp/get-pip.py
#     .venv/bin/python /tmp/get-pip.py -i https://pypi.tuna.tsinghua.edu.cn/simple
#     .venv/bin/pip install fastapi uvicorn requests ddddocr
#
# 想跳过滑块验证码（复用 .credentials/ 里的超星凭据）：
#     GRADE_PERSIST_CREDENTIAL=1 ./start.sh
set -euo pipefail
cd "$(dirname "$0")"

PY=".venv/bin/python"
if [ ! -x "$PY" ]; then
    echo "错误：找不到 $PY" >&2
    echo "请先按本文件顶部的说明创建虚拟环境并安装依赖。" >&2
    exit 1
fi

echo "成绩查询服务 → http://127.0.0.1:8000/grade/api/health"
if [ "${GRADE_PERSIST_CREDENTIAL:-0}" != "0" ]; then
    echo "凭据持久化：已开启（可跳过滑块）"
else
    echo "凭据持久化：关闭（登录超星需要过滑块）"
    echo "  用 GRADE_PERSIST_CREDENTIAL=1 ./start.sh 开启"
fi

exec "$PY" -m uvicorn app:app --host 127.0.0.1 --port 8000 --app-dir .
