from functools import lru_cache

from pydantic_settings import BaseSettings, SettingsConfigDict


class Settings(BaseSettings):
    app_name: str = "AIPA Studio Unified Service"
    app_version: str = "1.0.0-SNAPSHOT"
    api_prefix: str = "/api/v1"
    host: str = "127.0.0.1"
    port: int = 18080
    environment: str = "development"
    database_url: str = "sqlite:///./.ai-project/python-control-plane.db"

    model_config = SettingsConfigDict(
        env_prefix="AIPA_",
        extra="ignore",
    )


@lru_cache(maxsize=1)
def get_settings() -> Settings:
    return Settings()

