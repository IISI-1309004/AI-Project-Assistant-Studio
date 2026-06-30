from packages.scanner.orchestrator import ScannerOrchestrator


def test_priority_roots_move_core_files_first(tmp_path):
    (tmp_path / "docs").mkdir()
    (tmp_path / "src").mkdir()
    (tmp_path / "scripts").mkdir()
    (tmp_path / "src" / "app.py").write_text("print('core')", encoding="utf-8")
    (tmp_path / "docs" / "readme.md").write_text("docs", encoding="utf-8")
    (tmp_path / "scripts" / "tool.py").write_text("tool", encoding="utf-8")

    scanner = ScannerOrchestrator()
    result = scanner.scan_project(str(tmp_path))

    assert result["fragments"], "should emit fragments"
    assert result["fragments"][0]["sourceFile"].startswith("src/")
    assert result["fragments"][0]["tags"]
    assert result["fragments"][0]["parentRef"] == "src"
    assert result["fragments"][0]["relatedRefs"] == ["src"]

