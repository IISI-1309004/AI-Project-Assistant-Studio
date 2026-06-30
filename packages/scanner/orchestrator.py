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

    _PRIORITY_ROOTS = {
        "apps",
        "src",
        "services",
        "service",
        "api",
        "domain",
        "backend",
        "controller",
        "controllers",
        "route",
        "routes",
        "db",
        "database",
        "migrations",
        "config",
    }

    _PRIORITY_FILES = {
        "package.json",
        "pyproject.toml",
        "pom.xml",
        "build.gradle",
        "build.gradle.kts",
        "settings.gradle",
        "settings.gradle.kts",
    }

    def __init__(self) -> None:
        # Keep v1 scan bounded while still producing a meaningful knowledge base.
        self.max_fragments = max(50, int(os.getenv("AIPA_SCAN_MAX_FRAGMENTS", "5000")))
        self.max_content_chars = max(200, int(os.getenv("AIPA_SCAN_SNIPPET_CHARS", "1500")))
        self.priority_roots = self._load_priority_roots()

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

        fragments: list[dict[str, Any]] = []
        candidates: list[dict[str, Any]] = []
        file_count = 0
        manifest_hits = {
            "package.json": False,
            "pyproject.toml": False,
            "pom.xml": False,
            "build.gradle": False,
            "build.gradle.kts": False,
            "settings.gradle": False,
            "settings.gradle.kts": False,
        }

        for dirpath, dirnames, filenames in os.walk(root):
            # Skip heavy/generated folders.
            dirnames[:] = [d for d in dirnames if d not in self._IGNORED_DIRS]

            for filename in filenames:
                file_count += 1

                path = Path(dirpath) / filename
                rel_path = str(path.relative_to(root)).replace("\\", "/")
                if filename in manifest_hits:
                    manifest_hits[filename] = True

                ext = path.suffix.lower()
                if ext not in self._SOURCE_EXTS and filename not in self._PRIORITY_FILES:
                    continue

                content = self._read_snippet(path)
                if not content:
                    continue

                candidates.append(
                    {
                        "category": self._infer_category(rel_path, ext),
                        "title": f"{path.name} 檔案摘要",
                        "content": content,
                        "sourceFile": rel_path,
                        "priority_score": self._priority_score(rel_path),
                        "path_depth": len(Path(rel_path).parts),
                        "tags": self._build_tags(rel_path, ext),
                        "parentRef": self._parent_ref(rel_path),
                        "relatedRefs": self._related_refs(rel_path),
                    }
                )

                if progress_callback and file_count % 500 == 0:
                    progress_callback(file_count, len(candidates))

        candidates.sort(key=lambda item: (item["priority_score"], item["path_depth"], item["sourceFile"]))

        for candidate in candidates[: self.max_fragments]:
            fragments.append(candidate)
            if progress_callback and len(fragments) % 100 == 0:
                progress_callback(file_count, len(fragments))

        if progress_callback:
            progress_callback(file_count, len(fragments))

        return {
            "projectMeta": {
                "name": root.name or "default",
                "frameworks": self._merge_frameworks(frameworks, manifest_hits),
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

    def _load_priority_roots(self) -> list[str]:
        configured = os.getenv("AIPA_SCAN_PRIORITY_PATHS", "")
        roots = [item.strip().strip("/") for item in configured.split(",") if item.strip()]
        if roots:
            return roots
        return sorted(self._PRIORITY_ROOTS)

    def _priority_score(self, rel_path: str) -> tuple[int, int, int]:
        normalized = rel_path.replace("\\", "/").lower()
        parts = [part for part in normalized.split("/") if part]

        for idx, root in enumerate(self.priority_roots):
            root_lower = root.lower().strip("/")
            if normalized.startswith(f"{root_lower}/") or f"/{root_lower}/" in normalized or parts[:1] == [root_lower]:
                return (idx, len(parts), self._file_rank(parts[-1] if parts else normalized))

        fallback_rank = len(self.priority_roots) + 1
        return (fallback_rank, len(parts), self._file_rank(parts[-1] if parts else normalized))

    def _build_tags(self, rel_path: str, ext: str) -> list[str]:
        normalized = rel_path.replace("\\", "/").lower()
        parts = [part for part in normalized.split("/") if part]
        tags = [self._infer_category(rel_path, ext).lower()]
        if parts:
            tags.append(f"root:{parts[0]}")
            if len(parts) > 1:
                tags.append(f"dir:{parts[1]}")
        if len(parts) > 2:
            tags.append(f"path:{'/'.join(parts[:-1])}")
        return list(dict.fromkeys(tags))

    @staticmethod
    def _parent_ref(rel_path: str) -> str | None:
        parts = [part for part in rel_path.replace("\\", "/").split("/") if part]
        if len(parts) <= 1:
            return None
        return "/".join(parts[:-1])

    def _related_refs(self, rel_path: str) -> list[str]:
        normalized = rel_path.replace("\\", "/")
        parent = self._parent_ref(rel_path)
        if not parent:
            return []

        siblings = []
        for priority_root in self.priority_roots:
            root_prefix = f"{priority_root.lower().strip('/')}/"
            if normalized.lower().startswith(root_prefix):
                siblings.append(parent)
                break
        return list(dict.fromkeys(siblings))

    def _file_rank(self, filename: str) -> int:
        priority_order = [
            "package.json",
            "pyproject.toml",
            "pom.xml",
            "build.gradle.kts",
            "build.gradle",
            "settings.gradle.kts",
            "settings.gradle",
        ]
        lowered = filename.lower()
        for idx, item in enumerate(priority_order):
            if lowered == item:
                return idx
        return len(priority_order)

    @staticmethod
    def _merge_frameworks(frameworks: list[str], manifest_hits: dict[str, bool]) -> list[str]:
        merged = list(dict.fromkeys(frameworks))
        if manifest_hits.get("package.json") and "Node.js" not in merged:
            merged.append("Node.js")
        if manifest_hits.get("pyproject.toml") and "Python" not in merged:
            merged.append("Python")
        if any(manifest_hits.get(name) for name in ["pom.xml", "build.gradle", "build.gradle.kts", "settings.gradle", "settings.gradle.kts"]) and "JVM" not in merged:
            merged.append("JVM")
        return merged

