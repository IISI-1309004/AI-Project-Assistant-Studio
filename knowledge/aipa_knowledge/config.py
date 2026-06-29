"""
Knowledge Engine 設定
"""
from pydantic_settings import BaseSettings
import os


class Settings(BaseSettings):
    db_url: str = f"sqlite:///{os.path.expanduser('~')}/.aipa/aipa.db"
    chromadb_url: str = "http://localhost:18083"
    chromadb_path: str = ".ai-project/vector/chromadb"
    embedding_model: str = "all-MiniLM-L6-v2"

    class Config:
        env_prefix = "AIPA_"


settings = Settings()
