"""
Wisdom Engine — Phase 6 完整實作
規則 CRUD、規則匹配（WARN/BLOCK）、預設規則載入
"""
from __future__ import annotations

import logging
import os
import uuid
from typing import Optional

import yaml

logger = logging.getLogger(__name__)

# 預設規則模板路徑（相對於 project root）
_DEFAULT_RULES_PATH = os.path.join(
    os.path.dirname(__file__), "..", "..", "templates", "wisdom", "enterprise-rules.yml"
)


class WisdomEngine:
    """
    Phase 6 — 智慧規則管理 + 規則匹配
    """

    def __init__(self):
        from .repository import WisdomRepository
        self.repo = WisdomRepository()
        self._defaults_loaded = False

    # ------------------------------------------------------------------ #
    # CRUD
    # ------------------------------------------------------------------ #
    def add_rule(self, rule: dict) -> dict:
        rule_id = rule.get("id", str(uuid.uuid4()))
        rule["id"] = rule_id
        return self.repo.save(rule)

    def list_rules(self, project_id: str = "", enabled_only: bool = True) -> list[dict]:
        """列出規則；首次呼叫時自動載入預設規則"""
        self._ensure_defaults_loaded()
        return self.repo.find_all(project_id=project_id, enabled_only=enabled_only)

    def get_rule(self, rule_id: str) -> Optional[dict]:
        return self.repo.find_by_id(rule_id)

    def update_rule(self, rule_id: str, updates: dict) -> Optional[dict]:
        existing = self.repo.find_by_id(rule_id)
        if not existing:
            return None
        existing.update(updates)
        existing["id"] = rule_id
        return self.repo.save(existing)

    def delete_rule(self, rule_id: str) -> bool:
        return self.repo.delete(rule_id)

    # ------------------------------------------------------------------ #
    # 規則匹配（Phase 6 核心）
    # ------------------------------------------------------------------ #
    def match_rules(self, context: dict) -> list[dict]:
        """
        對程式碼變更內容進行規則匹配。
        context 包含：
          - code_diff: str       程式碼 diff
          - file_names: list     異動檔案
          - spec_type: str       FEATURE / BUG / REFACTOR
          - modules: list        異動模組
        回傳所有命中的規則（含 severity WARN 和 BLOCK）
        """
        self._ensure_defaults_loaded()
        code_diff = (context.get("code_diff", "") or "").lower()
        file_names = [f.lower() for f in context.get("file_names", [])]
        spec_type = context.get("spec_type", "FEATURE").upper()
        modules = [m.lower() for m in context.get("modules", [])]

        all_rules = self.repo.find_all(enabled_only=True)
        matched = []

        for rule in all_rules:
            # 範圍過濾
            rule_scope = rule.get("scope", {})
            rule_feature_types = rule_scope.get("featureTypes", [])
            if rule_feature_types and spec_type not in [f.upper() for f in rule_feature_types]:
                continue

            rule_modules = [m.lower() for m in rule_scope.get("modules", [])]
            if rule_modules and not any(
                any(rm in fn or rm in mod for fn in file_names for mod in (modules + [""]))
                for rm in rule_modules
            ):
                continue

            # 觸發條件匹配
            trigger_conditions = rule.get("trigger_conditions", [])
            hit = False
            hit_reason = ""
            for condition in trigger_conditions:
                condition_lower = condition.lower()
                if ("without where" in condition_lower or ("缺少" in condition_lower and "where" in condition_lower)) and (
                    ("update" in code_diff or "delete" in code_diff) and "where" not in code_diff
                ):
                    hit = True
                    hit_reason = condition
                    break
                # 簡單關鍵字匹配（Phase 6 基礎版，Phase 9 可換 LLM 增強）
                keywords = self._extract_keywords(condition_lower)
                if all(kw in code_diff for kw in keywords if kw):
                    hit = True
                    hit_reason = condition
                    break
                # 也對檔案名稱匹配
                if any(kw in fn for kw in keywords if kw for fn in file_names):
                    hit = True
                    hit_reason = condition
                    break

            if hit:
                matched.append({
                    **rule,
                    "hit_reason": hit_reason,
                })
                self.repo.increment_hit(rule["id"])

        # BLOCK 規則排前面
        matched.sort(key=lambda r: (0 if r["severity"] == "BLOCK" else 1, r["id"]))
        return matched

    def has_block_rules(self, context: dict) -> bool:
        """快速檢查是否有 BLOCK 級規則命中"""
        return any(r["severity"] == "BLOCK" for r in self.match_rules(context))

    # ------------------------------------------------------------------ #
    # 預設規則載入（Phase 6）
    # ------------------------------------------------------------------ #
    def load_default_rules(self) -> int:
        """從模板檔案載入預設企業開發規則，回傳載入數量"""
        rules_path = os.path.abspath(_DEFAULT_RULES_PATH)
        if not os.path.exists(rules_path):
            logger.warning(f"Default rules file not found: {rules_path}")
            return 0

        with open(rules_path, encoding="utf-8") as f:
            data = yaml.safe_load(f)

        rules = data.get("rules", [])
        loaded = 0
        for rule in rules:
            existing = self.repo.find_by_id(rule["id"])
            if not existing:
                # 轉換 YAML 結構 → WisdomRule dict
                normalized = {
                    "id": rule["id"],
                    "title": rule["title"],
                    "description": rule["description"],
                    "severity": rule.get("severity", "WARN"),
                    "scope": rule.get("scope", {"global": True}),
                    "trigger_conditions": rule.get("triggerConditions", []),
                    "examples": rule.get("examples", {}),
                    "enabled": True,
                }
                self.repo.save(normalized)
                loaded += 1

        logger.info(f"Loaded {loaded} default wisdom rules from {rules_path}")
        return loaded

    def _ensure_defaults_loaded(self):
        if not self._defaults_loaded:
            if self.repo.count() == 0:
                self.load_default_rules()
            self._defaults_loaded = True

    # ------------------------------------------------------------------ #
    # Helpers
    # ------------------------------------------------------------------ #
    @staticmethod
    def _extract_keywords(condition: str) -> list[str]:
        """從條件描述中提取關鍵字（去除常見停用詞）"""
        stop_words = {
            "中", "有", "或", "的", "在", "和", "與", "但", "類別", "方法", "語句",
            "不得", "必須", "應該", "禁止", "不可", "需要", "缺少", "包含",
            "a", "an", "the", "is", "are", "in", "of", "or", "and", "with",
            "has", "have", "without", "no", "not",
        }
        # 取出英文/中文關鍵詞
        import re
        tokens = re.findall(r"[a-zA-Z_@#.]+|[\u4e00-\u9fa5]+", condition)
        keywords = [t.lower() for t in tokens if t.lower() not in stop_words and len(t) > 1]
        return keywords[:5]  # 最多取 5 個關鍵字做 AND 匹配

