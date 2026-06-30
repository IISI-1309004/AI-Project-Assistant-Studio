from fastapi.testclient import TestClient

from apps.api.dependencies import reset_skeleton_state
from apps.api.main import app

client = TestClient(app)


def setup_function() -> None:
    reset_skeleton_state()


def test_knowledge_list_without_project_id_returns_empty_list() -> None:
    response = client.get("/api/v1/knowledge")
    assert response.status_code == 200
    assert response.json() == []


def test_experience_search_requires_query() -> None:
    response = client.post("/api/v1/experience/search", json={})
    assert response.status_code == 400


def test_wisdom_check_returns_runtime_compatible_shape() -> None:
    response = client.post(
        "/api/v1/wisdom/check",
        json={
            "code_diff": "",
            "file_names": [],
            "spec_type": "FEATURE",
            "modules": [],
        },
    )
    assert response.status_code == 200
    body = response.json()
    assert "hasBlockViolation" in body
    assert "blockCount" in body
    assert "warnCount" in body
    assert "matchedRules" in body
    assert isinstance(body["matchedRules"], list)


def test_memory_list_endpoint_accepts_runtime_query_fields() -> None:
    response = client.get("/api/v1/memory", params={"projectId": "demo", "type": ""})
    assert response.status_code == 200
    assert isinstance(response.json(), list)


def test_learning_endpoints_are_available() -> None:
    analyze = client.post("/api/v1/learn", json={"session_id": "s-demo"})
    assert analyze.status_code == 200
    assert "learning_id" in analyze.json()

    result = client.get("/api/v1/learn/skeleton")
    assert result.status_code == 200
    assert result.json()["learning_id"] == "skeleton"

    rollback = client.post("/api/v1/learn/skeleton/rollback")
    assert rollback.status_code == 200


def test_learning_write_back_returns_runtime_compatible_payload() -> None:
    created = client.post(
        "/api/v1/session",
        json={"projectId": "demo", "projectRoot": ".", "requirement": "learning write back"},
    )
    assert created.status_code == 200
    session_id = created.json()["sessionId"]

    write_back = client.post("/api/v1/learn/skeleton/write-back", params={"sessionId": session_id})
    assert write_back.status_code == 200
    payload = write_back.json()
    assert payload["message"] == "Learning result written back"
    assert payload["learningId"] == "skeleton"
    assert "result" in payload
    assert "session" in payload
    assert payload["session"]["sessionId"] == session_id
    assert payload["session"]["learningResult"]["learningId"] == "skeleton"
    assert "result" in payload["session"]["learningResult"]
    assert payload["session"]["learningResult"]["source"] == "learning-engine"
    assert payload["session"]["learningResult"]["version"] == "v1"
    assert "writtenAt" in payload["session"]["learningResult"]


