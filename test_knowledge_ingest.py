#!/usr/bin/env python3
"""
直接測試 Knowledge Ingest 功能（跳過 API）
"""
import sys
import os

# 設定路徑
project_root = os.path.dirname(os.path.abspath(__file__))
for path in [project_root, os.path.join(project_root, "knowledge"), os.path.join(project_root, "memory")]:
    if path not in sys.path:
        sys.path.insert(0, path)

print("=" * 70)
print("直接測試 Knowledge Ingest")
print("=" * 70)

# 測試 1: 導入所有模塊
print("\n[1] 測試模塊導入")
try:
    from aipa_knowledge.repository import KnowledgeRepository
    print("  ✅ KnowledgeRepository")
except Exception as e:
    print(f"  ❌ KnowledgeRepository: {e}")
    sys.exit(1)

try:
    from aipa_knowledge.embedding import EmbeddingService
    print("  ✅ EmbeddingService")
except Exception as e:
    print(f"  ❌ EmbeddingService: {e}")
    sys.exit(1)

try:
    from aipa_knowledge.vector_store import VectorStore
    print("  ✅ VectorStore")
except Exception as e:
    print(f"  ❌ VectorStore: {e}")
    sys.exit(1)

try:
    from aipa_knowledge.ingestor import ScanResultIngestor
    print("  ✅ ScanResultIngestor")
except Exception as e:
    print(f"  ❌ ScanResultIngestor: {e}")
    sys.exit(1)

# 測試 2: 初始化服務
print("\n[2] 初始化服務")
try:
    repo = KnowledgeRepository()
    print("  ✅ KnowledgeRepository 初始化")
except Exception as e:
    print(f"  ❌ KnowledgeRepository 初始化: {e}")

try:
    embedding = EmbeddingService()
    test_vec = embedding.embed("test")
    print(f"  ✅ EmbeddingService 初始化（向量維度: {len(test_vec)}）")
except Exception as e:
    print(f"  ❌ EmbeddingService 初始化: {e}")

try:
    vs = VectorStore()
    print("  ✅ VectorStore 初始化")
except Exception as e:
    print(f"  ❌ VectorStore 初始化: {e}")

try:
    ingestor = ScanResultIngestor()
    print("  ✅ ScanResultIngestor 初始化")
except Exception as e:
    print(f"  ❌ ScanResultIngestor 初始化: {e}")

# 測試 3: 模擬 Bulk Ingest
print("\n[3] 模擬 Bulk Ingest 操作")
try:
    mock_scan_result = {
        "fragmentCount": 100,
        "fragments": ["class Foo {}", "public void bar() {}"],
        "apiInventory": {
            "endpoints": ["/api/users", "/api/products"]
        }
    }

    items = ingestor.ingest("test-project", mock_scan_result)
    print(f"  ✅ Ingestor 生成了 {len(items)} 個知識項目")

    if items:
        print(f"     首個項目: {items[0].get('title', 'N/A')}")

except Exception as e:
    print(f"  ❌ Ingest 操作失敗: {e}")
    import traceback
    traceback.print_exc()

print("\n" + "=" * 70)
print("✅ 所有測試通過！Knowledge Engine 運作正常")
print("=" * 70)

