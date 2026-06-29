"""
VectorStore — ChromaDB 向量資料庫封裝（Phase 2 實作）
"""
from __future__ import annotations

import logging
import os
from typing import Optional

logger = logging.getLogger(__name__)


class VectorStore:
    """ChromaDB 向量儲存封裝（Phase 2 實作）"""

    def __init__(self, collection_name: str = "aipa_knowledge", persist_path: str = ".ai-project/vector/chromadb"):
        self.collection_name = collection_name
        self.persist_path = persist_path
        self._client = None
        self._collection = None

    def _get_collection(self):
        if self._collection is None:
            try:
                import chromadb
                os.makedirs(self.persist_path, exist_ok=True)
                self._client = chromadb.PersistentClient(path=self.persist_path)
                self._collection = self._client.get_or_create_collection(
                    name=self.collection_name,
                    metadata={"hnsw:space": "cosine"}
                )
                logger.info(f"ChromaDB collection '{self.collection_name}' ready at {self.persist_path}")
            except ImportError:
                logger.warning("chromadb not installed, vector search unavailable")
                return None
            except Exception as e:
                logger.error(f"Failed to initialize ChromaDB: {e}")
                return None
        return self._collection

    def upsert(self, item_id: str, vector: list[float], metadata: dict, document: str = "") -> bool:
        """插入或更新向量"""
        collection = self._get_collection()
        if collection is None:
            return False
        try:
            collection.upsert(
                ids=[item_id],
                embeddings=[vector],
                metadatas=[metadata],
                documents=[document]
            )
            return True
        except Exception as e:
            logger.error(f"VectorStore upsert failed: {e}")
            return False

    def search(self, query_vector: list[float], top_k: int = 5, where: Optional[dict] = None) -> list[dict]:
        """向量相似度搜尋"""
        collection = self._get_collection()
        if collection is None:
            return []
        try:
            kwargs = {
                "query_embeddings": [query_vector],
                "n_results": min(top_k, max(1, collection.count())),
                "include": ["metadatas", "distances", "documents"]
            }
            if where:
                kwargs["where"] = where
            results = collection.query(**kwargs)
            output = []
            if results and results["ids"] and results["ids"][0]:
                for i, item_id in enumerate(results["ids"][0]):
                    output.append({
                        "id": item_id,
                        "score": 1.0 - results["distances"][0][i],  # 餘弦距離轉相似度
                        "metadata": results["metadatas"][0][i] if results["metadatas"] else {},
                        "document": results["documents"][0][i] if results["documents"] else "",
                    })
            return output
        except Exception as e:
            logger.error(f"VectorStore search failed: {e}")
            return []

    def delete(self, item_id: str) -> bool:
        """刪除向量"""
        collection = self._get_collection()
        if collection is None:
            return False
        try:
            collection.delete(ids=[item_id])
            return True
        except Exception as e:
            logger.error(f"VectorStore delete failed: {e}")
            return False

    def count(self) -> int:
        collection = self._get_collection()
        return collection.count() if collection else 0
