from scripts.knowledge_graph_demo import build_graph_url, relation_breakdown


def test_build_graph_url_without_optional_filters() -> None:
    url = build_graph_url(
        base_url="http://127.0.0.1:8000/",
        project_id="demo-project",
        edge_type="",
        min_weight=0.0,
    )
    assert "project_id=demo-project" in url
    assert "edge_type=" not in url
    assert "min_weight=" not in url


def test_build_graph_url_with_all_filters() -> None:
    url = build_graph_url(
        base_url="http://127.0.0.1:8000",
        project_id="demo-project",
        edge_type="EXPLICIT_RELATED",
        min_weight=0.95,
        max_nodes=500,
        max_edges=100,
    )
    assert "project_id=demo-project" in url
    assert "edge_type=EXPLICIT_RELATED" in url
    assert "min_weight=0.95" in url
    assert "max_nodes=500" in url
    assert "max_edges=100" in url


def test_relation_breakdown_counts_edges() -> None:
    edges = [
        {"relation": "EXPLICIT_PARENT"},
        {"relation": "EXPLICIT_PARENT"},
        {"relation": "SHARED_TAG"},
    ]
    breakdown = relation_breakdown(edges)
    assert breakdown["EXPLICIT_PARENT"] == 2
    assert breakdown["SHARED_TAG"] == 1

