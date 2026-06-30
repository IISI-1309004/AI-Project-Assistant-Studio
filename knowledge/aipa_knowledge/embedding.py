"""
EmbeddingService — 使用 sentence-transformers 本地模型進行文字向量化（Phase 2 實作）
"""
from __future__ import annotations

import logging
import os
import hashlib
from functools import lru_cache
from typing import Optional

logger = logging.getLogger(__name__)

# 延遲載入：避免 import 時就載入大型模型
_model = None


def _get_model(model_name: str = "all-MiniLM-L6-v2"):
    """延遲載入模型：預設使用本地快速 embedder，必要時才啟用 sentence-transformers。"""
    global _model
    if _model is None:
        enable_st = os.getenv("AIPA_ENABLE_SENTENCE_TRANSFORMERS", "0") == "1"
        if not enable_st:
            logger.info("AIPA_ENABLE_SENTENCE_TRANSFORMERS is not set; using fast local hash embeddings")
            _model = FastHashEmbedder()
            return _model
        try:
            from sentence_transformers import SentenceTransformer
            logger.info(f"Loading embedding model: {model_name}")
            _model = SentenceTransformer(model_name)
            logger.info("Embedding model loaded successfully")
        except ImportError:
            logger.warning("sentence-transformers not installed, using dummy embeddings")
            _model = DummyEmbedder()
        except Exception as e:
            logger.error(f"Failed to load embedding model: {e}")
            _model = DummyEmbedder()
    return _model


class EmbeddingService:
    """文字向量嵌入服務（Phase 2 實作）"""

    def __init__(self, model_name: str = "all-MiniLM-L6-v2"):
        self.model_name = model_name

    def embed(self, text: str) -> list[float]:
        """將單一文字轉換為向量"""
        if not text or not text.strip():
            return [0.0] * 384  # MiniLM 維度
        model = _get_model(self.model_name)
        result = model.encode(text, show_progress_bar=False)
        return result.tolist() if hasattr(result, "tolist") else list(result)

    def embed_batch(self, texts: list[str]) -> list[list[float]]:
        """批量向量化（效率更高）"""
        if not texts:
            return []
        model = _get_model(self.model_name)
        results = model.encode(texts, show_progress_bar=False, batch_size=32)
        return [r.tolist() if hasattr(r, "tolist") else list(r) for r in results]

    def similarity(self, vec1: list[float], vec2: list[float]) -> float:
        """計算兩個向量的餘弦相似度"""
        if not vec1 or not vec2:
            return 0.0
        try:
            import numpy as np
            v1, v2 = np.array(vec1), np.array(vec2)
            norm = np.linalg.norm(v1) * np.linalg.norm(v2)
            return float(np.dot(v1, v2) / norm) if norm > 0 else 0.0
        except ImportError:
            # 純 Python fallback
            dot = sum(a * b for a, b in zip(vec1, vec2))
            n1 = sum(a * a for a in vec1) ** 0.5
            n2 = sum(b * b for b in vec2) ** 0.5
            return dot / (n1 * n2) if n1 * n2 > 0 else 0.0


class DummyEmbedder:
    """sentence-transformers 不可用時的備用實作（回傳零向量）"""

    def encode(self, texts, **kwargs):
        import numpy as np
        if isinstance(texts, str):
            return np.zeros(384)
        return np.zeros((len(texts), 384))


class FastHashEmbedder:
    """Lightweight deterministic embedder that avoids external model downloads."""

    def _encode_one(self, text: str) -> list[float]:
        if not text or not text.strip():
            return [0.0] * 384
        vec = [0.0] * 384
        for token in text.lower().split():
            digest = hashlib.sha256(token.encode("utf-8")).digest()
            idx = int.from_bytes(digest[:2], "little") % 384
            sign = 1.0 if (digest[2] & 1) == 0 else -1.0
            vec[idx] += sign
        norm = sum(v * v for v in vec) ** 0.5
        if norm > 0:
            vec = [v / norm for v in vec]
        return vec

    def encode(self, texts, **kwargs):
        if isinstance(texts, str):
            return self._encode_one(texts)
        return [self._encode_one(t) for t in texts]

