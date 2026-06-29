"""
ScanResultIngestor — 將 Scanner Engine 的 ScanResult 轉換為 KnowledgeItems（Phase 2 實作）
"""
from __future__ import annotations

import uuid
import logging
from typing import Any

logger = logging.getLogger(__name__)


class ScanResultIngestor:
    """將 Scanner 結果轉換為結構化 KnowledgeItems"""

    def ingest(self, project_id: str, scan_result: dict[str, Any]) -> list[dict]:
        """
        接收 Runtime 傳來的 ScanResult JSON，轉換為 KnowledgeItem 清單
        """
        items = []

        # 1. 處理知識片段（由子掃描器生成的原始片段）
        fragments = scan_result.get("fragments", [])
        for fragment in fragments:
            item = {
                "id": str(uuid.uuid4()),
                "project_id": project_id,
                "category": fragment.get("category", "PROJECT"),
                "title": fragment.get("title", "Unknown"),
                "content": fragment.get("content", ""),
                "source_type": "SCANNER",
                "source_ref": fragment.get("sourceFile", ""),
                "tags": [fragment.get("category", "").lower()],
                "confidence": 75,
            }
            items.append(item)

        # 2. 生成技術棧摘要知識項目
        project_meta = scan_result.get("projectMeta", {})
        if project_meta:
            items.append({
                "id": str(uuid.uuid4()),
                "project_id": project_id,
                "category": "PROJECT",
                "title": "技術棧總覽",
                "content": self._build_tech_stack_summary(project_meta),
                "source_type": "SCANNER",
                "source_ref": "TechStackDetector",
                "tags": ["tech-stack", "project"],
                "confidence": 90,
            })

        # 3. API 清單知識項目
        api_endpoints = scan_result.get("apiInventory", {}).get("endpoints", [])
        if api_endpoints:
            items.append({
                "id": str(uuid.uuid4()),
                "project_id": project_id,
                "category": "API",
                "title": "REST API 端點清單",
                "content": "已發現的 API 端點：\n" + "\n".join(f"  - {ep}" for ep in api_endpoints[:50]),
                "source_type": "SCANNER",
                "source_ref": "OpenApiScanner",
                "tags": ["api", "endpoints"],
                "confidence": 85,
            })

        # 4. 資料庫 Schema 知識項目
        db_tables = scan_result.get("databaseSchema", {}).get("tables", [])
        if db_tables:
            items.append({
                "id": str(uuid.uuid4()),
                "project_id": project_id,
                "category": "DATABASE",
                "title": "資料庫 Schema 總覽",
                "content": f"已掃描到 {len(db_tables)} 個資料表：\n" + "\n".join(f"  - {t}" for t in db_tables),
                "source_type": "SCANNER",
                "source_ref": "SqlDdlScanner",
                "tags": ["database", "schema"],
                "confidence": 85,
            })

        logger.info(f"Ingested {len(items)} knowledge items for project {project_id}")
        return items

    def _build_tech_stack_summary(self, meta: dict) -> str:
        lines = [f"專案名稱：{meta.get('name', 'unknown')}"]
        if meta.get("javaVersion"):
            lines.append(f"Java 版本：{meta['javaVersion']}")
        if meta.get("frameworks"):
            lines.append(f"框架：{', '.join(meta['frameworks'])}")
        if meta.get("databases"):
            lines.append(f"資料庫：{', '.join(meta['databases'])}")
        return "\n".join(lines)
