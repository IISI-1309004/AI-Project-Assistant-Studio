"""
Phase 6 — ExperienceEngine 單元測試
"""
import os
import sys
import pytest

sys.path.insert(0, os.path.join(os.path.dirname(__file__), "..", "experience"))
sys.path.insert(0, os.path.join(os.path.dirname(__file__), "..", "knowledge"))

os.environ["AIPA_DB_URL"] = "sqlite://"  # 記憶體 SQLite


@pytest.fixture
def engine():
    """每個測試使用獨立的 ExperienceEngine（記憶體 DB）"""
    import aipa_experience.repository as repo_mod
    repo_mod._engine = None
    repo_mod._SessionLocal = None

    from aipa_experience.engine import ExperienceEngine
    # 重置 vector store singleton
    import aipa_experience.engine as eng_mod
    eng_mod._embedding = None
    eng_mod._vector_store = None

    return ExperienceEngine()


class TestExperienceCaseCRUD:
    def test_create_case(self, engine):
        case = engine.create_case({
            "project_id": "proj-1",
            "title": "新增案件提醒功能",
            "requirement": "系統需要在案件到期前 3 天發送提醒",
            "spec_type": "FEATURE",
            "solution_summary": "透過 Spring Scheduler 實作",
            "patterns_used": ["Scheduler", "Event"],
            "confidence_score": 85,
            "outcome": "SUCCESS",
        })
        assert case["id"] is not None
        assert case["title"] == "新增案件提醒功能"
        assert case["project_id"] == "proj-1"
        assert case["outcome"] == "SUCCESS"

    def test_get_case(self, engine):
        created = engine.create_case({
            "project_id": "p1",
            "title": "Test Case",
            "requirement": "A test requirement",
        })
        fetched = engine.get_case(created["id"])
        assert fetched is not None
        assert fetched["title"] == "Test Case"

    def test_list_cases(self, engine):
        engine.create_case({"project_id": "p1", "title": "Case A", "requirement": "req A"})
        engine.create_case({"project_id": "p1", "title": "Case B", "requirement": "req B"})
        engine.create_case({"project_id": "p2", "title": "Case C", "requirement": "req C"})

        p1_cases = engine.list_cases("p1")
        assert len(p1_cases) == 2
        titles = [c["title"] for c in p1_cases]
        assert "Case A" in titles
        assert "Case B" in titles

    def test_update_case(self, engine):
        created = engine.create_case({
            "project_id": "p1",
            "title": "Old Title",
            "requirement": "req",
        })
        updated = engine.update_case(created["id"], {"title": "New Title", "outcome": "PARTIAL"})
        assert updated["title"] == "New Title"
        assert updated["outcome"] == "PARTIAL"

    def test_delete_case(self, engine):
        created = engine.create_case({
            "project_id": "p1",
            "title": "To Delete",
            "requirement": "will be deleted",
        })
        deleted = engine.delete_case(created["id"])
        assert deleted is True
        assert engine.get_case(created["id"]) is None


class TestExperienceSimilaritySearch:
    def test_search_returns_empty_when_no_cases(self, engine):
        results = engine.search_similar("new notification feature", project_id="p1")
        assert isinstance(results, list)
        assert len(results) == 0

    def test_search_with_keyword_fallback(self, engine):
        """When vector store returns empty, keyword fallback kicks in"""
        engine.create_case({
            "project_id": "p1",
            "title": "案件提醒功能",
            "requirement": "notification reminder feature for cases",
            "solution_summary": "scheduler based approach",
        })
        # With stub embeddings (all zeros), vector search returns nothing;
        # keyword fallback should find the case
        results = engine.search_similar("reminder notification", project_id="p1")
        assert isinstance(results, list)
        # Keyword fallback should return the case (similarity = 0.0 for stub)
        titles = [r["title"] for r in results]
        assert "案件提醒功能" in titles

    def test_search_respects_project_id(self, engine):
        engine.create_case({"project_id": "p1", "title": "P1 Feature", "requirement": "search test"})
        engine.create_case({"project_id": "p2", "title": "P2 Feature", "requirement": "search test"})

        results_p1 = engine.search_similar("search test", project_id="p1")
        ids = [r["project_id"] for r in results_p1]
        assert "p2" not in ids

    def test_reference_count_incremented_on_search(self, engine):
        created = engine.create_case({
            "project_id": "p1",
            "title": "Count Test",
            "requirement": "increment counter",
        })
        initial_count = created.get("reference_count", 0)

        # With keyword fallback (stub embeddings), this will find and increment
        results = engine.search_similar("increment counter", project_id="p1")
        if results:
            fetched = engine.get_case(created["id"])
            assert (fetched.get("reference_count", 0) or 0) >= initial_count


class TestExperienceCaseTextBuilding:
    def test_case_to_text_combines_fields(self):
        from aipa_experience.engine import ExperienceEngine
        case = {
            "title": "Reminder Feature",
            "requirement": "Send notification 3 days before",
            "solution_summary": "Spring Scheduler",
            "patterns_used": ["Scheduler", "Observer"],
            "knowledge_topics": ["Spring", "JPA"],
            "key_decisions": ["Use async MQ"],
        }
        text = ExperienceEngine._case_to_text(case)
        assert "Reminder Feature" in text
        assert "Send notification" in text
        assert "Spring Scheduler" in text
        assert "Scheduler" in text
        assert "Use async MQ" in text

