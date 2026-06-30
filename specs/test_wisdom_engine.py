"""
Phase 6 — WisdomEngine 單元測試
"""
import os
import sys
import tempfile
import pytest

# 將 wisdom module 加入 path
sys.path.insert(0, os.path.join(os.path.dirname(__file__), "..", "wisdom"))
sys.path.insert(0, os.path.join(os.path.dirname(__file__), "..", "templates"))

os.environ["AIPA_DB_URL"] = "sqlite://"   # 使用記憶體 SQLite


@pytest.fixture
def engine():
    """每個測試使用獨立的 WisdomEngine（記憶體 DB）"""
    import importlib
    # 清理 singleton 避免測試間互相影響
    import aipa_wisdom.repository as repo_mod
    repo_mod._engine = None
    repo_mod._SessionLocal = None

    from aipa_wisdom.engine import WisdomEngine
    e = WisdomEngine()
    e._defaults_loaded = False  # 強制重新載入
    return e


class TestWisdomRuleCRUD:
    def test_add_and_get_rule(self, engine):
        rule = engine.add_rule({
            "title": "Test Rule",
            "description": "A test rule",
            "severity": "WARN",
            "scope": {"global": True},
            "trigger_conditions": ["test condition"],
        })
        assert rule["id"] is not None
        assert rule["title"] == "Test Rule"
        assert rule["severity"] == "WARN"

        fetched = engine.get_rule(rule["id"])
        assert fetched is not None
        assert fetched["description"] == "A test rule"

    def test_update_rule(self, engine):
        rule = engine.add_rule({
            "title": "Old Title",
            "description": "desc",
            "severity": "WARN",
        })
        updated = engine.update_rule(rule["id"], {"title": "New Title", "severity": "BLOCK"})
        assert updated["title"] == "New Title"
        assert updated["severity"] == "BLOCK"

    def test_delete_rule(self, engine):
        rule = engine.add_rule({
            "title": "To Delete",
            "description": "will be deleted",
            "severity": "WARN",
        })
        deleted = engine.delete_rule(rule["id"])
        assert deleted is True
        assert engine.get_rule(rule["id"]) is None

    def test_list_rules_returns_enabled_only(self, engine):
        engine.add_rule({"title": "Active", "description": "d", "severity": "WARN", "enabled": True})
        engine.add_rule({"title": "Disabled", "description": "d", "severity": "BLOCK", "enabled": False})
        rules = engine.list_rules(enabled_only=True)
        titles = [r["title"] for r in rules]
        assert "Active" in titles
        assert "Disabled" not in titles


class TestWisdomRuleMatching:
    def test_match_block_rule_on_sql_keyword(self, engine):
        engine.add_rule({
            "id": "TEST-BLOCK-001",
            "title": "No UPDATE without WHERE",
            "description": "Dangerous SQL",
            "severity": "BLOCK",
            "scope": {"global": True},
            "trigger_conditions": ["update without where"],
        })
        context = {
            "code_diff": "execute: UPDATE users SET status = 0",
            "file_names": ["UserMapper.xml"],
            "spec_type": "FEATURE",
        }
        matched = engine.match_rules(context)
        block_ids = [r["id"] for r in matched if r["severity"] == "BLOCK"]
        assert "TEST-BLOCK-001" in block_ids

    def test_no_match_on_clean_code(self, engine):
        engine.add_rule({
            "id": "TEST-WARN-001",
            "title": "Avoid N+1",
            "description": "No DB in loop",
            "severity": "WARN",
            "scope": {"global": True},
            "trigger_conditions": ["findbyid loop"],
        })
        context = {
            "code_diff": "void greet() { System.out.println(\"hello\"); }",
            "file_names": ["GreetService.py"],
            "spec_type": "FEATURE",
        }
        matched = engine.match_rules(context)
        assert len(matched) == 0

    def test_has_block_rules(self, engine):
        engine.add_rule({
            "title": "Block rule",
            "description": "d",
            "severity": "BLOCK",
            "trigger_conditions": ["secret keyword xyz"],
        })
        assert engine.has_block_rules({
            "code_diff": "secret keyword xyz in code",
            "file_names": [],
        }) is True
        assert engine.has_block_rules({
            "code_diff": "totally safe code here",
            "file_names": [],
        }) is False

    def test_feature_type_scope_filter(self, engine):
        engine.add_rule({
            "title": "Only BUG type",
            "description": "d",
            "severity": "WARN",
            "scope": {"global": False, "featureTypes": ["BUG"]},
            "trigger_conditions": ["trigger word"],
        })
        # Should not match for FEATURE
        matched_feature = engine.match_rules({
            "code_diff": "trigger word here",
            "spec_type": "FEATURE",
        })
        assert len(matched_feature) == 0

        # Should match for BUG
        matched_bug = engine.match_rules({
            "code_diff": "trigger word here",
            "spec_type": "BUG",
        })
        assert len(matched_bug) == 1


class TestWisdomDefaultRulesLoader:
    def test_load_default_rules(self, engine):
        """載入預設規則（至少 6 條）"""
        count = engine.load_default_rules()
        assert count >= 6

    def test_default_rules_include_block_severity(self, engine):
        engine.load_default_rules()
        rules = engine.list_rules()
        block_rules = [r for r in rules if r["severity"] == "BLOCK"]
        assert len(block_rules) >= 3  # WIS-ENT-001, WIS-ENT-004, WIS-DB-001, WIS-SEC-001

    def test_default_rules_not_duplicated(self, engine):
        engine.load_default_rules()
        count1 = engine.repo.count()
        engine.load_default_rules()  # 再呼叫一次
        count2 = engine.repo.count()
        assert count1 == count2  # 不應重複建立

