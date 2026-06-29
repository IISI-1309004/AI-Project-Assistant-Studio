# 功能規格模板（Freemarker）— Phase 1 佔位
# Phase 3 實作 SpecFactory 後使用此模板
# 以下為規格文件的標準結構

# Feature Spec: ${title}

**規格 ID**：${specId}  
**建立時間**：${createdAt}  
**類型**：FEATURE  
**狀態**：${status}  
**信心分數**：${confidenceScore} / 100  

---

## 1. 需求說明

${requirement.summary}

### 驗收標準
<#list requirement.acceptanceCriteria as criteria>
- ${criteria}
</#list>

### 範圍外
<#list requirement.outOfScope as item>
- ${item}
</#list>

---

## 2. 知識上下文

<#list context.knowledgeRefs as ref>
- **${ref.title}**：${ref.summary}
</#list>

---

## 3. 影響分析

- **風險等級**：${impactAnalysis.riskLevel}
- **影響模組**：${impactAnalysis.affectedModules?join(", ")}
- **影響 API**：${impactAnalysis.affectedAPIs?join(", ")}
- **影響資料表**：${impactAnalysis.affectedTables?join(", ")}

---

## 4. 回滾計劃

${rollbackPlan}

---

## 5. 測試計劃

### 單元測試
<#list testPlan.unitTests as test>
- ${test}
</#list>

### 整合測試
<#list testPlan.integrationTests as test>
- ${test}
</#list>
