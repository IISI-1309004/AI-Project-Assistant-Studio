#!/usr/bin/env python3
"""Legacy launcher updated to start the unified AIPA Studio service."""
import subprocess
import sys
import os
import time

def main():
    # 設定環境
    project_root = os.path.dirname(os.path.abspath(__file__))
    os.chdir(project_root)

    # 確保 pyproject.toml 所在目錄在 PYTHONPATH 中
    env = os.environ.copy()
    env["PYTHONPATH"] = project_root + os.pathsep + env.get("PYTHONPATH", "")

    print("=" * 70)
    print("AIPA Studio Unified Service - 啟動腳本")
    print("=" * 70)
    print(f"項目根目錄: {project_root}")
    print(f"PYTHONPATH: {env['PYTHONPATH']}")
    print()

    # 使用 uvicorn 啟動 FastAPI
    cmd = [
        sys.executable,
        "-m", "uvicorn",
        "apps.api.main:app",
        "--host", "0.0.0.0",
        "--port", "18080",
        "--reload",
        "--timeout-keep-alive", "60",
    ]

    print(f"執行命令: {' '.join(cmd)}")
    print("=" * 70)
    print()

    try:
        subprocess.run(cmd, env=env, cwd=project_root)
    except KeyboardInterrupt:
        print("\n\n已停止 AI Engine")
        sys.exit(0)
    except Exception as e:
        print(f"錯誤: {e}")
        sys.exit(1)

if __name__ == "__main__":
    main()
