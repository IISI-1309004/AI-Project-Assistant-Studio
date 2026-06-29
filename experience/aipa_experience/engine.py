"""
Experience Engine — Phase 6 完整實作
建立/查詢 ExperienceCase + 向量化語意搜尋（相似度 > 0.6）
"""
from __future__ import annotations

import logging
import os
import sys
import uuid
from typing import Optional

logger = logging.getLogger(__name__)

# 共用 EmbeddingService 和 VectorStore（從 knowledge 模組取得）
_embedding = None
_vector_store = None


def _get_embedding():
    global _embedding
    if _embedding is None:
        try:
            sys.path.insert(0, os.path.join(os.path.dirname(__file__), "..", "..", "knowledge"))
            from aipa_knowledge.embedding import EmbeddingService
            _embedding = EmbeddingService()
        except Exception as e:
            logger.warning(f"Could not load EmbeddingService: {e}; using stub")
            _embedding = _StubEmbedding()
    return _embedding


def _get_vector_store():
    global _vector_store
    if _vector_store is None:
        try:
            sys.path.insert(0, os.path.join(os.path.dirname(__file__), "..", "..", "knowledge"))
            from aipa_knowledge.vector_store import VectorStore
            _vector_store = VectorStore(
                collection_name="aipa_experience",
                persist_path=".ai-project/vector/experience"
            )
        except Exception as e:
            logger.warning(f"Could not load VectorStore: {e}; using stub")
            _vector_store = _StubVectorStore()
    return _vector_store


class _StubEmbedding:
    def embed(self, text: str) -> list[float]:
        return [0.0] * 384

    def embed_batch(self, texts: list[str]) -> list[list[float]]:
        return [[0.0] * 384 for _ in texts]

    def similarity(self, v1: list[float], v2: list[float]) -> float:
        return 0.0


class _StubVectorStore:
    def upsert(self, *a, **kw): return True
    def search(self, *a, **kw): return []
    def delete(self, *a, **kw): return True
    def count(self): return 0


SIMILARITY_THRESHOLD = 0.6


class ExperienceEngine:
    """
    Phase 6 — ExperienceCase CRUD + 語意搜尋
    """

    def __init__(self):
        from .repository import ExperienceRepository
        self.repo = ExperienceRepository()

    # ------------------------------------------------------------------ #
    # CRUD
    # ------------------------------------------------------------------ #
    def create_case(self, case: dict) -> dict:
        """建立 ExperienceCase 並向量化"""
        case_id = case.get("id", str(uuid.uuid4()))
        case["id"] = case_id

        # 向量化
        text = self._case_to_text(case)
        embedding = _get_embedding()
        vector = embedding.embed(text)

        # 存入向量庫
        vector_id = f"exp_{case_id}"
        metadata = {
            "project_id": case.get("project_id", ""),
            "spec_type": case.get("spec_type", "FEATURE"),
            "title": case.get("title", ""),
        }
        _get_vector_store().upsert(vector_id, vector, metadata, text)
        case["vector_id"] = vector_id

        saved = self.repo.save(case)
        logger.info(f"Created ExperienceCase {case_id}: {case.get('title', '')}")
        return saved

    def get_case(self, case_id: str) -> Optional[dict]:
        return self.repo.find_by_id(case_id)

    def list_cases(self, project_id: str) -> list[dict]:
        return self.repo.find_all(project_id)

    def update_case(self, case_id: str, updates: dict) -> Optional[dict]:
        existing = self.repo.find_by_id(case_id)
        if not existing:
            return None
        existing.update(updates)
        existing["id"] = case_id

        # 重新向量化
        text = self._case_to_text(existing)
        embedding = _get_embedding()
        vector = embedding.embed(text)
        vector_id = existing.get("vector_id", f"exp_{case_id}")
        _get_vector_store().upsert(vector_id, vector, {
            "project_id": existing.get("project_id", ""),
            "spec_type": existing.get("spec_type", "FEATURE"),
            "title": existing.get("title", ""),
        }, text)

        return self.repo.save(existing)

    def delete_case(self, case_id: str) -> bool:
        case = self.repo.find_by_id(case_id)
        if case and case.get("vector_id"):
            _get_vector_store().delete(case["vector_id"])
        return self.repo.delete(case_id)

    # ------------------------------------------------------------------ #
    # 語意搜尋（Phase 6 核心）
    # ------------------------------------------------------------------ #
    def search_similar(self, query: str, project_id: str = "", top_k: int = 5) -> list[dict]:
        """
        語意搜尋相似 ExperienceCase，只回傳相似度 > 0.6 的結果
        """
        if not query:
            return []

        embedding = _get_embedding()
        query_vector = embedding.embed(query)

        where: Optional[dict] = {"project_id": project_id} if project_id else None
        raw_results = _get_vector_store().search(query_vector, top_k=top_k * 2, where=where)

        results = []
        for vr in raw_results:
            score = float(vr.get("score", 0.0))
            if score < SIMILARITY_THRESHOLD:
                continue
            vector_id = vr["id"]
            case_id = vector_id[4:] if vector_id.startswith("exp_") else vector_id
            case = self.repo.find_by_id(case_id)
            if case:
                case["_similarity"] = round(score, 4)
                results.append(case)
                # 記錄引用
                self.repo.increment_reference(case_id)

        # fallback：如果向量搜尋無結果，用關鍵字搜尋
        if not results:
            all_cases = self.repo.find_all(project_id)
            query_lower = query.lower()
            for c in all_cases:
                if (query_lower in c.get("title", "").lower()
                        or query_lower in c.get("requirement", "").lower()
                        or query_lower in c.get("solution_summary", "").lower()):
                    c["_similarity"] = 0.0
                    results.append(c)

        return sorted(results, key=lambda x: x.get("_similarity", 0), reverse=True)[:top_k]

    # ------------------------------------------------------------------ #
    # Helpers
    # ------------------------------------------------------------------ #
    @staticmethod
    def _case_to_text(case: dict) -> str:
        parts = [
            case.get("title", ""),
            case.get("requirement", ""),
            case.get("solution_summary", ""),
            " ".join(case.get("patterns_used", [])),
            " ".join(case.get("knowledge_topics", [])),
            " ".join(case.get("key_decisions", [])),
        ]
        return " ".join(p for p in parts if p).strip()

