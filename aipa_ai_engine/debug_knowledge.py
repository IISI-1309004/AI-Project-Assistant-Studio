#!/usr/bin/env python3
"""
診斷工具：檢查 Knowledge Engine 各個服務的初始化狀態
"""
import sys
import logging

logging.basicConfig(level=logging.DEBUG)
logger = logging.getLogger(__name__)

print("=" * 60)
print("AIPA Knowledge Engine 診斷工具")
print("=" * 60)

# 1. 檢查依賴
print("\n[1] 檢查 Python 依賴...")
try:
    import chromadb
    print("✅ chromadb 已安裝")
except ImportError as e:
    print(f"❌ chromadb 傳入錯誤: {e}")

try:
    import sqlalchemy
    print("✅ sqlalchemy 已安裝")
except ImportError as e:
    print(f"❌ sqlalchemy 安裝錯誤: {e}")

try:
    import sentence_transformers
    print("✅ sentence-transformers 已安裝")
except ImportError as e:
    print(f"⚠️  sentence-transformers 未安裝 (使用 dummy embedder): {e}")

# 2. 檢查 KnowledgeRepository 初始化
print("\n[2] 檢查 KnowledgeRepository...")
try:
    from aipa_knowledge.repository import KnowledgeRepository
    repo = KnowledgeRepository()
    print("✅ KnowledgeRepository 初始化成功")
except Exception as e:
    print(f"❌ KnowledgeRepository 初始化失敗: {e}")
    import traceback
    traceback.print_exc()

# 3. 檢查 EmbeddingService 初始化
print("\n[3] 檢查 EmbeddingService...")
try:
    from aipa_knowledge.embedding import EmbeddingService
    svc = EmbeddingService()
    test_vec = svc.embed("test")
    print(f"✅ EmbeddingService 初始化成功（向量維度: {len(test_vec)}）")
except Exception as e:
    print(f"❌ EmbeddingService 初始化失敗: {e}")
    import traceback
    traceback.print_exc()

# 4. 檢查 VectorStore 初始化
print("\n[4] 檢查 VectorStore...")
try:
    from aipa_knowledge.vector_store import VectorStore
    vs = VectorStore()
    # 嘗試獲取 collection 但不實際存儲任何內容
    _ = vs._get_collection()
    print("✅ VectorStore 初始化成功")
except Exception as e:
    print(f"❌ VectorStore 初始化失敗: {e}")
    import traceback
    traceback.print_exc()

# 5. 檢查 ScanResultIngestor 初始化
print("\n[5] 檢查 ScanResultIngestor...")
try:
    from aipa_knowledge.ingestor import ScanResultIngestor
    ingestor = ScanResultIngestor()
    print("✅ ScanResultIngestor 初始化成功")
except Exception as e:
    print(f"❌ ScanResultIngestor 初始化失敗: {e}")
    import traceback
    traceback.print_exc()

# 6. 模擬 _get_services() 呼叫
print("\n[6] 模擬 Knowledge Router _get_services()...")
try:
    from aipa_knowledge import router
    services = router._get_services()
    if services:
        repo, embedding, vector_store, ingestor = services
        print(f"✅ 所有服務都已正確初始化")
        print(f"   - KnowledgeRepository: {type(repo).__name__}")
        print(f"   - EmbeddingService: {type(embedding).__name__}")
        print(f"   - VectorStore: {type(vector_store).__name__}")
        print(f"   - ScanResultIngestor: {type(ingestor).__name__}")
    else:
        print("❌ _get_services() 返回 None")
except Exception as e:
    print(f"❌ _get_services() 呼叫失敗: {e}")
    import traceback
    traceback.print_exc()

print("\n" + "=" * 60)
print("診斷完成")
print("=" * 60)

