from __future__ import annotations

from pathlib import Path
from typing import Any


class ScannerOrchestrator:
    """Minimal scanner abstraction for the Python-first control plane scaffold."""

    def scan_project(self, project_root: str) -> dict[str, Any]:

        root = Path(project_root).resolve()
        files = [path for path in root.rglob("*") if path.is_file()] if root.exists() else []

        frameworks: list[str] = []
        if (root / "package.json").exists():
            frameworks.append("Node.js")
        if (root / "pyproject.toml").exists():
            frameworks.append("Python")

        return {
            "projectMeta": {
                "name": root.name or "default",
                "frameworks": frameworks,
                "databases": [],
            },
            "fragments": [],
            "apiInventory": {"endpoints": []},
            "architectureGraph": {"components": [item.name for item in root.iterdir() if item.is_dir()][:10]} if root.exists() else {"components": []},
            "dependencyTree": {"packages": frameworks},
            "summary": {
                "fileCount": len(files),
                "topLevelDirectoryCount": len([item for item in root.iterdir() if item.is_dir()]) if root.exists() else 0,
            },
        }

