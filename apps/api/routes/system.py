from fastapi import APIRouter

from apps.api.config import get_settings
from apps.api.schemas.common import utc_now_iso

router = APIRouter()


@router.get("/health")
def health() -> dict[str, str]:
    settings = get_settings()
    return {
        "status": "UP",
        "version": settings.app_version,
        "timestamp": utc_now_iso(),
        "phase": "Unified AIPA Studio Service",
    }


@router.get("/version")
def version() -> dict[str, str]:
    settings = get_settings()
    return {
        "version": settings.app_version,
        "buildTime": utc_now_iso(),
    }

