@echo off
REM Start the grade query FastAPI service on 127.0.0.1:8000.
REM Uses the py2 virtualenv interpreter on purpose: ddddocr / onnxruntime / opencv live there.
REM
REM To skip the slider captcha on later starts, enable credential persistence first:
REM     set GRADE_PERSIST_CREDENTIAL=1
REM then run this script again.
set PY=C:\Users\qiang\Desktop\1\py2\.venv\Scripts\python.exe
if not exist "%PY%" set PY=python
"%PY%" -m uvicorn app:app --host 127.0.0.1 --port 8000 --app-dir "%~dp0"
