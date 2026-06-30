from knowledge.aipa_knowledge.router import _build_graph_edges, _coerce_cache_ttl_seconds


def test_build_graph_edges_creates_same_parent_edges() -> None:
    items = [
        {
            "id": "a",
            "title": "a.py",
            "category": "PROJECT",
            "source_ref": "apps/api/main.py",
            "tags": ["project", "root:apps", "dir:api"],
        },
        {
            "id": "b",
            "title": "deps.py",
            "category": "PROJECT",
            "source_ref": "apps/api/dependencies.py",
            "tags": ["project", "root:apps", "dir:api"],
        },
    ]

    edges = _build_graph_edges(items)

    assert any(edge["relation"] == "EXPLICIT_PARENT" for edge in edges)
    assert any(edge["label"] == "apps/api" for edge in edges)
    assert all("weight" in edge for edge in edges)


def test_build_graph_edges_creates_shared_tag_edges() -> None:
    items = [
        {
            "id": "a",
            "title": "auth service",
            "category": "API",
            "source_ref": "apps/api/auth.py",
            "tags": ["api", "security", "root:apps"],
        },
        {
            "id": "b",
            "title": "security helper",
            "category": "PROJECT",
            "source_ref": "packages/core/security.py",
            "tags": ["project", "security", "root:packages"],
        },
    ]

    edges = _build_graph_edges(items)

    assert any(edge["relation"] == "SHARED_TAG" for edge in edges)
    assert any(edge["label"] == "security" for edge in edges)


def test_coerce_cache_ttl_seconds_handles_invalid_and_low_values() -> None:
    assert _coerce_cache_ttl_seconds("120") == 120.0
    assert _coerce_cache_ttl_seconds("0") == 1.0
    assert _coerce_cache_ttl_seconds("-5") == 1.0
    assert _coerce_cache_ttl_seconds("not-a-number") == 60.0


