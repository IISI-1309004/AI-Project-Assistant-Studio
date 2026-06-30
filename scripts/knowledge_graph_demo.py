#!/usr/bin/env python3
"""Simple CLI demo for querying knowledge graph edges from the engine API."""

from __future__ import annotations

import argparse
import json
import sys
from typing import Any
from urllib.error import HTTPError, URLError
from urllib.parse import urlencode
from urllib.request import urlopen


def build_graph_url(
    base_url: str,
    project_id: str,
    edge_type: str,
    min_weight: float,
    max_nodes: int = 0,
    max_edges: int = 0,
) -> str:
    params = {
        "project_id": project_id,
    }
    if edge_type:
        params["edge_type"] = edge_type
    if min_weight > 0:
        params["min_weight"] = str(min_weight)
    if max_nodes > 0:
        params["max_nodes"] = str(max_nodes)
    if max_edges > 0:
        params["max_edges"] = str(max_edges)

    base = base_url.rstrip("/")
    return f"{base}/engine/knowledge/graph?{urlencode(params)}"


def fetch_graph(url: str, timeout_seconds: float) -> dict[str, Any]:
    with urlopen(url, timeout=timeout_seconds) as response:
        payload = response.read().decode("utf-8")
        return json.loads(payload)


def relation_breakdown(edges: list[dict[str, Any]]) -> dict[str, int]:
    counts: dict[str, int] = {}
    for edge in edges:
        relation = str(edge.get("relation", "UNKNOWN"))
        counts[relation] = counts.get(relation, 0) + 1
    return dict(sorted(counts.items(), key=lambda item: item[0]))


def parse_args(argv: list[str]) -> argparse.Namespace:
    parser = argparse.ArgumentParser(description="Knowledge graph API demo client")
    parser.add_argument("--base-url", default="http://127.0.0.1:8000", help="API base URL")
    parser.add_argument("--project-id", required=True, help="Project ID")
    parser.add_argument("--edge-type", default="", help="Optional edge relation filter")
    parser.add_argument("--min-weight", type=float, default=0.0, help="Optional minimum edge weight (0-1)")
    parser.add_argument("--max-nodes", type=int, default=0, help="Max nodes to return (0 = server default)")
    parser.add_argument("--max-edges", type=int, default=0, help="Max edges to return (0 = server default)")
    parser.add_argument("--timeout", type=float, default=10.0, help="HTTP timeout seconds")
    parser.add_argument("--dry-run", action="store_true", help="Only print request URL")
    parser.add_argument("--raw", action="store_true", help="Print full JSON payload")
    return parser.parse_args(argv)


def main(argv: list[str] | None = None) -> int:
    args = parse_args(argv or sys.argv[1:])

    if args.min_weight < 0 or args.min_weight > 1:
        print("min-weight must be between 0 and 1", file=sys.stderr)
        return 2

    url = build_graph_url(
        args.base_url,
        args.project_id,
        args.edge_type,
        args.min_weight,
        max_nodes=args.max_nodes,
        max_edges=args.max_edges,
    )
    print(f"GET {url}")

    if args.dry_run:
        return 0

    try:
        data = fetch_graph(url, args.timeout)
    except HTTPError as exc:
        print(f"HTTP error: {exc.code} {exc.reason}", file=sys.stderr)
        return 1
    except URLError as exc:
        print(f"Connection error: {exc.reason}", file=sys.stderr)
        return 1
    except json.JSONDecodeError as exc:
        print(f"Invalid JSON response: {exc}", file=sys.stderr)
        return 1

    nodes = data.get("nodes", [])
    edges = data.get("edges", [])
    returned_nodes = data.get("returned_nodes", len(nodes))
    returned_edges = data.get("returned_edges", len(edges))
    total = data.get("total", len(nodes))
    edge_total = data.get("edge_total", len(edges))
    print(f"nodes={returned_nodes}/{total} edges={returned_edges}/{edge_total}")
    print("relation_breakdown=", relation_breakdown(edges))

    if args.raw:
        print(json.dumps(data, ensure_ascii=True, indent=2))

    return 0


if __name__ == "__main__":
    raise SystemExit(main())

