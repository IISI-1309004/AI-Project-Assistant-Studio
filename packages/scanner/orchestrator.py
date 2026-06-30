from __future__ import annotations

import os
from pathlib import Path
from typing import Any, Callable


class ScannerOrchestrator:
    """Project scanner v1: framework detection + lightweight fragment extraction."""

    _IGNORED_DIRS = {
        ".git",
        ".idea",
        ".vscode",
        ".venv",
        "venv",
        "node_modules",
        "dist",
        "build",
        "target",
        "out",
        "__pycache__",
        ".pytest_cache",
        ".mypy_cache",
    }

    _SOURCE_EXTS = {
        ".py", ".ts", ".tsx", ".js", ".jsx", ".java", ".kt", ".kts", ".go", ".rs", ".cs",
        ".sql", ".yaml", ".yml", ".json", ".toml", ".xml", ".md", ".properties", ".ini",
    }

    def __init__(self) -> None:
        # Keep v1 scan bounded while still producing a meaningful knowledge base.
        self.max_fragments = max(50, int(os.getenv("AIPA_SCAN_MAX_FRAGMENTS", "5000")))
        self.max_content_chars = max(200, int(os.getenv("AIPA_SCAN_SNIPPET_CHARS", "1500")))

    def scan_project(
        self,
        project_root: str,
        progress_callback: Callable[[int, int], None] | None = None,
    ) -> dict[str, Any]:
        root = Path(project_root).resolve()
        if not root.exists():
            return {
                "projectMeta": {"name": "default", "frameworks": [], "databases": []},
                "fragments": [],
                "apiInventory": {"endpoints": []},
                "architectureGraph": {"components": []},
                "dependencyTree": {"packages": []},
                "summary": {"fileCount": 0, "topLevelDirectoryCount": 0},
            }

        frameworks: list[str] = []
        if (root / "package.json").exists() or any(root.glob("**/package.json")):
            frameworks.append("Node.js")
        if (root / "pyproject.toml").exists() or any(root.glob("**/pyproject.toml")):
            frameworks.append("Python")
        if any(root.glob("**/pom.xml")) or any(root.glob("**/build.gradle*")):
            frameworks.append("JVM")

        fragments: list[dict[str, str]] = []
        file_count = 0

        for dirpath, dirnames, filenames in os.walk(root):
            # Skip heavy/generated folders.
            dirnames[:] = [d for d in dirnames if d not in self._IGNORED_DIRS]

            for filename in filenames:
                file_count += 1
                if progress_callback and file_count % 500 == 0:
                    progress_callback(file_count, len(fragments))
                if len(fragments) >= self.max_fragments:
                    continue

                path = Path(dirpath) / filename
                ext = path.suffix.lower()
                if ext not in self._SOURCE_EXTS:
                    continue

                rel_path = str(path.relative_to(root)).replace("\\", "/")
                content = self._read_snippet(path)
                if not content:
                    continue

                fragments.append(
                    {
                        "category": self._infer_category(rel_path, ext),
                        "title": f"{path.name} 檔案摘要",
                        "content": content,
                        "sourceFile": rel_path,
                    }
                )

                if progress_callback and len(fragments) % 100 == 0:
                    progress_callback(file_count, len(fragments))

        if progress_callback:
            progress_callback(file_count, len(fragments))

        return {
            "projectMeta": {
                "name": root.name or "default",
                "frameworks": frameworks,
                "databases": [],
            },
            "fragments": fragments,
            "apiInventory": {"endpoints": []},
            "architectureGraph": {"components": [item.name for item in root.iterdir() if item.is_dir()][:10]},
            "dependencyTree": {"packages": frameworks},
            "summary": {
                "fileCount": file_count,
                "topLevelDirectoryCount": len([item for item in root.iterdir() if item.is_dir()]),
            },
        }

    def _read_snippet(self, path: Path) -> str:
        try:
            text = path.read_text(encoding="utf-8", errors="ignore").strip()
            if not text:
                return ""
            return text[: self.max_content_chars]
        except OSError:
            return ""

    @staticmethod
    def _infer_category(rel_path: str, ext: str) -> str:
        lowered = rel_path.lower()
        if "/api/" in lowered or "controller" in lowered or "route" in lowered:
            return "API"
        if ext in {".sql", ".ddl"} or "migration" in lowered:
            return "DATABASE"
        if "docker" in lowered or "deploy" in lowered or "k8s" in lowered:
            return "INFRASTRUCTURE"
        if ext in {".md", ".txt"}:
            return "DOCUMENTATION"
        if ext in {".yaml", ".yml", ".json", ".toml", ".ini", ".properties", ".xml"}:
            return "CONFIGURATION"
        return "PROJECT"

