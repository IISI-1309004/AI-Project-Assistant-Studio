import pytest
from fastapi.testclient import TestClient

from apps.api.main import app
from knowledge.aipa_knowledge import router as knowledge_router

client = TestClient(app)


@pytest.fixture(autouse=True)
def _reset_graph_cache_state() -> None:
    knowledge_router._reset_graph_caches_for_tests()


class _FakeRepo:
    def __init__(self, items: list[dict]):
        self._items = items

    def find_all(self, project_id: str, category: str | None = None) -> list[dict]:
        filtered = [item for item in self._items if item.get("project_id") == project_id]
        if category:
            filtered = [item for item in filtered if item.get("category") == category]
        return filtered


def _sample_items() -> list[dict]:
    return [
        {
            "id": "a",
            "project_id": "graph-test-project",
            "category": "API",
            "title": "auth.py",
            "content": "",
            "source_ref": "apps/api/auth.py",
            "parent_ref": "apps/api",
            "related_refs": ["packages/core"],
            "tags": ["api", "security"],
        },
        {
            "id": "b",
            "project_id": "graph-test-project",
            "category": "API",
            "title": "main.py",
            "content": "",
            "source_ref": "apps/api/main.py",
            "parent_ref": "apps/api",
            "related_refs": [],
            "tags": ["api"],
        },
        {
            "id": "c",
            "project_id": "graph-test-project",
            "category": "PROJECT",
            "title": "security.py",
            "content": "",
            "source_ref": "packages/core/security.py",
            "parent_ref": "packages/core",
            "related_refs": [],
            "tags": ["project", "security"],
        },
    ]


def test_engine_graph_returns_explicit_edges(monkeypatch) -> None:
    fake_repo = _FakeRepo(_sample_items())

    monkeypatch.setattr(knowledge_router, "_get_services", lambda: (fake_repo, None, None, None))

    response = client.get("/engine/knowledge/graph", params={"project_id": "graph-test-project"})
    assert response.status_code == 200

    body = response.json()
    assert body["total"] == 3
    assert body["edge_total"] >= 2

    relations = {edge["relation"] for edge in body["edges"]}
    assert "EXPLICIT_PARENT" in relations
    assert "EXPLICIT_RELATED" in relations
    assert all("weight" in edge for edge in body["edges"])


def test_engine_graph_filters_by_edge_type(monkeypatch) -> None:
    fake_repo = _FakeRepo(_sample_items())

    monkeypatch.setattr(knowledge_router, "_get_services", lambda: (fake_repo, None, None, None))

    response = client.get(
        "/engine/knowledge/graph",
        params={"project_id": "graph-test-project", "edge_type": "EXPLICIT_RELATED"},
    )
    assert response.status_code == 200

    body = response.json()
    assert body["edge_total"] >= 1
    assert all(edge["relation"] == "EXPLICIT_RELATED" for edge in body["edges"])


def test_engine_graph_filters_by_min_weight(monkeypatch) -> None:
    fake_repo = _FakeRepo(_sample_items())

    monkeypatch.setattr(knowledge_router, "_get_services", lambda: (fake_repo, None, None, None))

    response = client.get(
        "/engine/knowledge/graph",
        params={"project_id": "graph-test-project", "min_weight": 0.96},
    )
    assert response.status_code == 200

    body = response.json()
    assert body["edge_total"] >= 1
    assert all(float(edge["weight"]) >= 0.96 for edge in body["edges"])
    assert all(edge["relation"] == "EXPLICIT_PARENT" for edge in body["edges"])


def test_engine_graph_max_nodes_limits_returned_nodes(monkeypatch) -> None:
    fake_repo = _FakeRepo(_sample_items())
    monkeypatch.setattr(knowledge_router, "_get_services", lambda: (fake_repo, None, None, None))

    response = client.get(
        "/engine/knowledge/graph",
        params={"project_id": "graph-test-project", "max_nodes": 2},
    )
    assert response.status_code == 200

    body = response.json()
    assert body["returned_nodes"] == 2
    assert body["total"] == 3
    # All returned edges must reference only visible nodes.
    visible_ids = {node["id"] for node in body["nodes"]}
    for edge in body["edges"]:
        assert edge["source"] in visible_ids
        assert edge["target"] in visible_ids


def test_engine_graph_max_edges_limits_returned_edges(monkeypatch) -> None:
    fake_repo = _FakeRepo(_sample_items())
    monkeypatch.setattr(knowledge_router, "_get_services", lambda: (fake_repo, None, None, None))

    response = client.get(
        "/engine/knowledge/graph",
        params={"project_id": "graph-test-project", "max_edges": 1},
    )
    assert response.status_code == 200

    body = response.json()
    assert body["returned_edges"] <= 1
    assert body["edge_total"] >= body["returned_edges"]
    # Best edge (highest weight) should come first.
    if body["returned_edges"] > 0:
        assert body["edges"][0]["weight"] >= 0.9


def test_engine_graph_returned_edges_sorted_by_weight_desc(monkeypatch) -> None:
    fake_repo = _FakeRepo(_sample_items())
    monkeypatch.setattr(knowledge_router, "_get_services", lambda: (fake_repo, None, None, None))

    response = client.get("/engine/knowledge/graph", params={"project_id": "graph-test-project"})
    assert response.status_code == 200

    edges = response.json()["edges"]
    weights = [float(e["weight"]) for e in edges]
    assert weights == sorted(weights, reverse=True)


def test_engine_graph_uses_cached_edges_until_project_changes(monkeypatch) -> None:
    fake_repo = _FakeRepo(_sample_items())
    monkeypatch.setattr(knowledge_router, "_get_services", lambda: (fake_repo, None, None, None))

    call_counter = {"count": 0}
    original = knowledge_router._build_graph_edges

    def _counted_build_graph_edges(items, max_edges=knowledge_router._MAX_GRAPH_EDGES):
        call_counter["count"] += 1
        return original(items, max_edges=max_edges)

    monkeypatch.setattr(knowledge_router, "_build_graph_edges", _counted_build_graph_edges)

    first = client.get("/engine/knowledge/graph", params={"project_id": "graph-test-project"})
    second = client.get("/engine/knowledge/graph", params={"project_id": "graph-test-project"})

    assert first.status_code == 200
    assert second.status_code == 200
    assert call_counter["count"] == 1

    knowledge_router._mark_graph_dirty("graph-test-project")
    third = client.get("/engine/knowledge/graph", params={"project_id": "graph-test-project"})
    assert third.status_code == 200
    assert call_counter["count"] == 2


