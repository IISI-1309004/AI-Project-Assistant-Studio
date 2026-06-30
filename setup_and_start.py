#!/usr/bin/env python3
"""快速安装依赖并启动 AI Engine"""
import subprocess
import sys
import time

deps = [
    "fastapi",
    "uvicorn",
    "pydantic",
    "pydantic-settings",
    "sqlalchemy",
    "chromadb",
    "httpx",
]

print("=" * 60)
print("正在安裝依賴...")
print("=" * 60)

for dep in deps:
    try:
        print(f"✓ {dep} ", end="", flush=True)
        subprocess.run([sys.executable, "-m", "pip", "install", "-q", dep],
                       timeout=120, check=False)
        print("OK")
    except Exception as e:
        print(f"SKIP ({e})")

print("\n" + "=" * 60)
print("準備啟動 AI Engine...")
print("=" * 60)

# 為了安全起見，只設定環境變數而不實際啟動
import os
os.environ["PYTHONPATH"] = os.getcwd()

print(f"♦ PYTHONPATH={os.getcwd()}")
print(f"♦ 當前目錄={os.getcwd()}")

print("\n現在執行以啟動 Unified Service:")
print("  uvicorn apps.api.main:app --host localhost --port 18080 --reload")
print("\n或者（推薦）：")
print("  python -m uvicorn apps.api.main:app --host localhost --port 18080")

