"""
AIPA Studio AI Engine — FastAPI 主進程入口
整合所有 Python Engine 模組（Knowledge / Memory / Learning / Experience / Wisdom）
"""
from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware
import logging

# Phase 9: 結構化 JSON 日誌配置
from aipa_ai_engine.logging_config import setup_json_logging, get_audit_logger

# 初始化日誌
setup_json_logging(
    service_name="ai-engine",
    version="1.0.0-SNAPSHOT",
    enable_console=True,
    enable_file=True,
    enable_audit=True,
)

logger = logging.getLogger("aipa_ai_engine")
audit_logger = get_audit_logger()

from aipa_knowledge.router import router as knowledge_router
from aipa_memory.router import router as memory_router
from aipa_learning.router import router as learning_router
from aipa_experience.router import router as experience_router
from aipa_wisdom.router import router as wisdom_router

# 多租戶支持（一對多架構）
from aipa_ai_engine.project_context_middleware import ProjectContextMiddleware

app = FastAPI(
    title="AIPA Studio AI Engine",
    description="Knowledge, Memory, Learning, Experience, Wisdom Engines",
    version="1.0.0-SNAPSHOT",
)

# 添加多租戶上下文中間件（第一個執行，提取 project_id）
app.add_middleware(ProjectContextMiddleware)

app.add_middleware(
    CORSMiddleware,
    allow_origins=["http://localhost:18080"],  # Runtime Service only
    allow_methods=["*"],
    allow_headers=["*"],
)

# 掛載所有 Engine Router
app.include_router(knowledge_router, prefix="/engine/knowledge", tags=["knowledge"])
app.include_router(memory_router, prefix="/engine/memory", tags=["memory"])
app.include_router(learning_router, prefix="/engine/learning", tags=["learning"])
app.include_router(experience_router, prefix="/engine/experience", tags=["experience"])
app.include_router(wisdom_router, prefix="/engine/wisdom", tags=["wisdom"])


@app.on_event("startup")
async def on_startup():
    """啟動時自動載入預設智慧規則（如果資料庫為空）"""
    try:
        from aipa_wisdom.engine import WisdomEngine
        engine = WisdomEngine()
        loaded = engine.load_default_rules()
        if loaded > 0:
            import logging
            logging.getLogger(__name__).info(f"Loaded {loaded} default wisdom rules on startup")
    except Exception as e:
        import logging
        logging.getLogger(__name__).warning(f"Could not load default wisdom rules: {e}")


@app.get("/engine/health")
async def health():
    return {
        "status": "UP",
        "version": "1.0.0-SNAPSHOT",
        "phase": "Phase 6 — Experience + Wisdom Engines",
        "engines": {
            "knowledge": "active",
            "memory": "active",
            "learning": "active",
            "experience": "active",
            "wisdom": "active",
        },
    }
