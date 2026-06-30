from contextlib import asynccontextmanager
import logging

from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware

from apps.api.config import get_settings
from apps.api.dependencies import shutdown_background_workers
from apps.api.logging_config import get_audit_logger, setup_json_logging
from apps.api.project_context_middleware import ProjectContextMiddleware
from apps.api.routes import checkpoints, experience, knowledge, learning, memory, project_init, sessions, system, wisdom
from experience.aipa_experience.router import router as experience_engine_router
from knowledge.aipa_knowledge.router import router as knowledge_engine_router
from learning.aipa_learning.router import router as learning_engine_router
from memory.aipa_memory.router import router as memory_engine_router
from wisdom.aipa_wisdom.engine import WisdomEngine
from wisdom.aipa_wisdom.router import router as wisdom_engine_router

settings = get_settings()


class _AccessLogFilter(logging.Filter):
    def filter(self, record: logging.LogRecord) -> bool:
        message = record.getMessage()
        # Hide repetitive polling requests from CLI init progress loop.
        if "GET /api/v1/project/init/" in message and "/status HTTP/" in message:
            return False
        return True

setup_json_logging(
    service_name="aipa-studio",
    version=settings.app_version,
    enable_console=True,
    enable_file=True,
    enable_audit=True,
)

logging.getLogger("uvicorn.access").addFilter(_AccessLogFilter())


@asynccontextmanager
async def lifespan(app: FastAPI):
    try:
        loaded = WisdomEngine().load_default_rules()
        if loaded > 0:
            get_audit_logger().info(f"Loaded {loaded} default wisdom rules during startup")
    except Exception as exc:
        get_audit_logger().warning(f"Could not load default wisdom rules: {exc}")

    yield
    shutdown_background_workers()


app = FastAPI(
    title=settings.app_name,
    version=settings.app_version,
    description="Unified AIPA Studio service (control plane + AI engines)",
    lifespan=lifespan,
)

app.add_middleware(ProjectContextMiddleware)
app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_methods=["*"],
    allow_headers=["*"],
)

# Control plane API
app.include_router(system.router, prefix=settings.api_prefix)
app.include_router(project_init.router, prefix=settings.api_prefix)
app.include_router(sessions.router, prefix=settings.api_prefix)
app.include_router(checkpoints.router, prefix=settings.api_prefix)
app.include_router(knowledge.router, prefix=settings.api_prefix)
app.include_router(memory.router, prefix=settings.api_prefix)
app.include_router(experience.router, prefix=settings.api_prefix)
app.include_router(wisdom.router, prefix=settings.api_prefix)
app.include_router(learning.router, prefix=settings.api_prefix)

# Backward-compatible engine API routes on the same service/port.
app.include_router(knowledge_engine_router, prefix="/engine/knowledge", tags=["engine-knowledge"])
app.include_router(memory_engine_router, prefix="/engine/memory", tags=["engine-memory"])
app.include_router(learning_engine_router, prefix="/engine/learning", tags=["engine-learning"])
app.include_router(experience_engine_router, prefix="/engine/experience", tags=["engine-experience"])
app.include_router(wisdom_engine_router, prefix="/engine/wisdom", tags=["engine-wisdom"])


@app.get("/engine/health")
async def engine_health() -> dict:
    return {
        "status": "UP",
        "version": settings.app_version,
        "phase": "Unified AIPA Studio Service",
        "engines": {
            "knowledge": "active",
            "memory": "active",
            "learning": "active",
            "experience": "active",
            "wisdom": "active",
        },
    }

