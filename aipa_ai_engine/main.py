"""
AIPA Studio AI Engine — FastAPI 主進程入口
整合所有 Python Engine 模組（Knowledge / Memory / Learning / Experience / Wisdom）
"""
from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware

from aipa_knowledge.router import router as knowledge_router
from aipa_memory.router import router as memory_router
from aipa_learning.router import router as learning_router
from aipa_experience.router import router as experience_router
from aipa_wisdom.router import router as wisdom_router

app = FastAPI(
    title="AIPA Studio AI Engine",
    description="Knowledge, Memory, Learning, Experience, Wisdom Engines",
    version="1.0.0-SNAPSHOT",
)

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


@app.get("/engine/health")
async def health():
    return {
        "status": "UP",
        "version": "1.0.0-SNAPSHOT",
        "phase": "Phase 1 — Skeleton",
        "engines": {
            "knowledge": "skeleton",
            "memory": "skeleton",
            "learning": "skeleton",
            "experience": "skeleton",
            "wisdom": "skeleton",
        },
    }
