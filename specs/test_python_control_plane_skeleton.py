import time
from pathlib import Path

from fastapi.testclient import TestClient

from apps.api.dependencies import reset_skeleton_state
from apps.api.main import app
from packages.core.checkpoints.service import SqlAlchemyCheckpointService
from packages.core.sessions.service import SqlAlchemySessionService

client = TestClient(app)


def setup_function() -> None:
    reset_skeleton_state()


def test_health_endpoint_returns_python_control_plane_metadata() -> None:
    response = client.get("/api/v1/health")
    assert response.status_code == 200
    body = response.json()
    assert body["status"] == "UP"
    assert body["phase"] == "Unified AIPA Studio Service"


def test_engine_health_endpoint_is_served_by_same_app() -> None:
    response = client.get("/engine/health")
    assert response.status_code == 200
    body = response.json()
    assert body["status"] == "UP"
    assert body["engines"]["knowledge"] == "active"


def test_project_init_returns_completed_job_with_knowledge_ingest(tmp_path: Path) -> None:
    (tmp_path / "package.json").write_text('{"name":"demo"}', encoding="utf-8")

    response = client.post(
        "/api/v1/project/init",
        json={
            "projectRoot": str(tmp_path),
            "projectId": "demo-project",
        },
    )

    assert response.status_code == 200
    body = response.json()
    assert body["status"] in {"STARTED", "RUNNING", "COMPLETED"}
    assert body["jobId"].startswith("job-")

    status_body = _poll_job_until_done(body["jobId"])
    assert status_body["jobId"] == body["jobId"]
    assert status_body["status"] == "COMPLETED"
    assert status_body["summary"]["projectName"] == tmp_path.name
    assert "Node.js" in status_body["summary"]["frameworks"]
    assert status_body["summary"]["knowledgeIngestStatus"] == "COMPLETED"
    assert status_body["summary"]["knowledgeItemCount"] >= 1


def test_session_and_checkpoint_flow_advances_to_task_pending() -> None:
    response = client.post(
        "/api/v1/session",
        json={
            "projectId": "demo",
            "projectRoot": ".",
            "requirement": "新增付款提醒功能",
        },
    )
    assert response.status_code == 200
    session = response.json()
    assert session["status"] == "SPEC_PENDING"
    checkpoint_id = session["currentCheckpointId"]
    assert checkpoint_id is not None

    checkpoints = client.get("/api/v1/checkpoint")
    assert checkpoints.status_code == 200
    assert checkpoints.json()[0]["checkpointId"] == checkpoint_id

    approved = client.post(
        f"/api/v1/checkpoint/{checkpoint_id}/approve",
        json={"reviewer": "pytest", "comment": "approved"},
    )
    assert approved.status_code == 200
    approved_body = approved.json()
    assert approved_body["checkpoint"]["status"] == "APPROVED"
    assert approved_body["session"]["status"] == "TASK_PENDING"

    fresh_session_service = SqlAlchemySessionService()
    persisted_session = fresh_session_service.get_session(session["sessionId"])
    assert persisted_session is not None
    assert persisted_session["status"] == "TASK_PENDING"

    fresh_checkpoint_service = SqlAlchemyCheckpointService()
    persisted_checkpoint = fresh_checkpoint_service.get_checkpoint(checkpoint_id)
    assert persisted_checkpoint is not None
    assert persisted_checkpoint["status"] == "APPROVED"

    stream = client.get(f"/api/v1/session/{session['sessionId']}/stream")
    assert stream.status_code == 200
    assert "session-status" in stream.text


def _poll_job_until_done(job_id: str, timeout_seconds: float = 10.0) -> dict:
    deadline = time.time() + timeout_seconds
    last = None
    while time.time() < deadline:
        status_response = client.get(f"/api/v1/project/init/{job_id}/status")
        assert status_response.status_code == 200
        last = status_response.json()
        if last["status"] in {"COMPLETED", "FAILED"}:
            return last
        time.sleep(0.05)
    assert last is not None
    return last


