package com.aipa.workflow.spec;

import com.fasterxml.jackson.databind.ObjectMapper;
import freemarker.cache.StringTemplateLoader;
import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * SpecEngineImpl — Phase 3 規格生成實作
 */
@Service
public class SpecEngineImpl implements SpecEngine {

    private static final String TEMPLATE_NAME = "feature-spec.ftl";
    private static final String FALLBACK_TEMPLATE = "# Feature Spec: ${title}\n\n"
            + "**規格 ID**：${specId}  \n"
            + "**建立時間**：${createdAt}  \n"
            + "**類型**：FEATURE  \n"
            + "**狀態**：${status}  \n"
            + "**信心分數**：${confidenceScore} / 100  \n\n"
            + "## 1. 需求說明\n${requirement.summary}\n\n"
            + "### 驗收標準\n<#list requirement.acceptanceCriteria as criteria>- ${criteria}\n</#list>\n"
            + "### 範圍外\n<#list requirement.outOfScope as item>- ${item}\n</#list>\n\n"
            + "## 2. 知識上下文\n<#list context.knowledgeRefs as ref>- **${ref.title}**：${ref.summary}\n</#list>\n\n"
            + "## 3. 影響分析\n"
            + "- **風險等級**：${impactAnalysis.riskLevel}\n"
            + "- **影響模組**：${impactAnalysis.affectedModules?join(', ')}\n"
            + "- **影響 API**：${impactAnalysis.affectedAPIs?join(', ')}\n"
            + "- **影響資料表**：${impactAnalysis.affectedTables?join(', ')}\n\n"
            + "## 4. 回滾計劃\n${rollbackPlan}\n\n"
            + "## 5. 測試計劃\n"
            + "### 單元測試\n<#list testPlan.unitTests as test>- ${test}\n</#list>\n"
            + "### 整合測試\n<#list testPlan.integrationTests as test>- ${test}\n</#list>\n";

    private final ObjectMapper objectMapper;
    private final ConcurrentMap<String, Specification> specs = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, Path> specProjectRoots = new ConcurrentHashMap<>();
    private final Configuration freemarker;

    public SpecEngineImpl() {
        this(new ObjectMapper().findAndRegisterModules());
    }

    public SpecEngineImpl(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper.findAndRegisterModules();
        this.freemarker = buildFreemarker();
    }

    @Override
    public Specification generateSpec(SpecRequest request) {
        try {
            String specId = UUID.randomUUID().toString();
            String title = buildTitle(request.rawRequirement());
            Map<String, Object> model = buildModel(specId, title, request);
            String content = renderTemplate(model);

            Specification spec = new Specification(
                    specId,
                    request.projectId(),
                    request.sessionId(),
                    request.type(),
                    SpecStatus.PENDING_APPROVAL,
                    title,
                    request.rawRequirement(),
                    content,
                    request.initialConfidenceScore(),
                    Instant.now()
            );
            specs.put(spec.id(), spec);
            specProjectRoots.put(spec.id(), Path.of(request.projectRoot()));
            persist(spec, Path.of(request.projectRoot()));
            return spec;
        } catch (IOException | TemplateException ex) {
            throw new IllegalStateException("Failed to generate specification: " + ex.getMessage(), ex);
        }
    }

    @Override
    public Specification approveSpec(String specId, String approvedBy, String comments) {
        return updateStatus(specId, SpecStatus.APPROVED, approvedBy, comments);
    }

    @Override
    public Specification rejectSpec(String specId, String rejectedBy, String reason) {
        return updateStatus(specId, SpecStatus.REJECTED, rejectedBy, reason);
    }

    @Override
    public Specification getSpec(String specId) {
        return specs.get(specId);
    }

    private Specification updateStatus(String specId, SpecStatus status, String actor, String note) {
        Specification current = specs.get(specId);
        if (current == null) {
            throw new IllegalArgumentException("Specification not found: " + specId);
        }
        String updatedContent = current.content()
                + "\n\n---\n"
                + "審核狀態：" + status + "\n"
                + "審核者：" + actor + "\n"
                + "備註：" + (note == null ? "" : note) + "\n";
        Specification updated = new Specification(
                current.id(),
                current.projectId(),
                current.sessionId(),
                current.type(),
                status,
                current.title(),
                current.rawRequirement(),
                updatedContent,
                current.confidenceScore(),
                current.createdAt()
        );
        specs.put(specId, updated);
        Path projectRoot = specProjectRoots.get(specId);
        if (projectRoot != null) {
            try {
                persist(updated, projectRoot);
            } catch (IOException ex) {
                throw new IllegalStateException("Failed to persist specification update", ex);
            }
        }
        return updated;
    }

    private Map<String, Object> buildModel(String specId, String title, SpecRequest request) {
        List<Map<String, Object>> knowledgeRefs = new ArrayList<>();
        for (Map<String, Object> item : request.knowledgeRefs()) {
            knowledgeRefs.add(Map.of(
                    "title", String.valueOf(item.getOrDefault("title", "Untitled")),
                    "summary", summarize(String.valueOf(item.getOrDefault("content", "")))
            ));
        }

        List<String> affectedApis = request.knowledgeRefs().stream()
                .filter(item -> "API".equals(String.valueOf(item.get("category"))))
                .map(item -> String.valueOf(item.get("title")))
                .toList();
        List<String> affectedTables = request.knowledgeRefs().stream()
                .filter(item -> "DATABASE".equals(String.valueOf(item.get("category"))))
                .map(item -> String.valueOf(item.get("title")))
                .toList();
        List<String> affectedModules = request.knowledgeRefs().stream()
                .filter(item -> "ARCHITECTURE".equals(String.valueOf(item.get("category"))) || "PROJECT".equals(String.valueOf(item.get("category"))))
                .map(item -> String.valueOf(item.get("title")))
                .distinct()
                .limit(6)
                .toList();

        return new LinkedHashMap<>(Map.of(
                "title", title,
                "specId", specId,
                "createdAt", Instant.now().toString(),
                "status", SpecStatus.PENDING_APPROVAL.name(),
                "confidenceScore", request.initialConfidenceScore(),
                "requirement", Map.of(
                        "summary", request.rawRequirement(),
                        "acceptanceCriteria", buildAcceptanceCriteria(request.rawRequirement()),
                        "outOfScope", buildOutOfScopeItems(request.rawRequirement())
                ),
                "context", Map.of(
                        "knowledgeRefs", knowledgeRefs
                ),
                "impactAnalysis", Map.of(
                        "riskLevel", determineRiskLevel(request.rawRequirement()),
                        "affectedModules", affectedModules.isEmpty() ? List.of("待補充") : affectedModules,
                        "affectedAPIs", affectedApis.isEmpty() ? List.of("無直接 API 影響") : affectedApis,
                        "affectedTables", affectedTables.isEmpty() ? List.of("無直接資料表影響") : affectedTables
                ),
                "rollbackPlan", "若上線後出現異常，先回滾至前一版部署，停用新功能開關，並依據影響 API / 資料表進行資料校正。",
                "testPlan", Map.of(
                        "unitTests", buildUnitTests(title),
                        "integrationTests", buildIntegrationTests(title, affectedApis)
                )
        ));
    }

    private Configuration buildFreemarker() {
        Configuration configuration = new Configuration(Configuration.VERSION_2_3_32);
        StringTemplateLoader loader = new StringTemplateLoader();
        loader.putTemplate(TEMPLATE_NAME, loadTemplateContent());
        configuration.setTemplateLoader(loader);
        configuration.setDefaultEncoding("UTF-8");
        return configuration;
    }

    private String renderTemplate(Map<String, Object> model) throws IOException, TemplateException {
        Template template = freemarker.getTemplate(TEMPLATE_NAME);
        StringWriter writer = new StringWriter();
        template.process(model, writer);
        return writer.toString();
    }

    private String loadTemplateContent() {
        for (String candidate : List.of(
                "templates/specs/feature-spec.ftl",
                "../templates/specs/feature-spec.ftl",
                "../../templates/specs/feature-spec.ftl"
        )) {
            Path path = Path.of(candidate).normalize();
            if (Files.exists(path)) {
                try {
                    return Files.readString(path);
                } catch (IOException ignored) {
                    // fall through to fallback
                }
            }
        }
        return FALLBACK_TEMPLATE;
    }

    private void persist(Specification spec, Path projectRoot) throws IOException {
        Path specsDir = projectRoot.resolve(".ai-project/specs");
        Files.createDirectories(specsDir);
        Files.writeString(specsDir.resolve(spec.id() + ".md"), spec.content());
        objectMapper.writerWithDefaultPrettyPrinter().writeValue(specsDir.resolve(spec.id() + ".json").toFile(), spec);
    }

    private String buildTitle(String requirement) {
        String normalized = requirement == null ? "Untitled Requirement" : requirement.trim();
        if (normalized.isEmpty()) {
            return "Untitled Requirement";
        }
        return normalized.length() > 32 ? normalized.substring(0, 32) + "..." : normalized;
    }

    private String summarize(String content) {
        if (content == null || content.isBlank()) {
            return "無摘要";
        }
        String normalized = content.replace('\n', ' ').trim();
        return normalized.length() > 96 ? normalized.substring(0, 96) + "..." : normalized;
    }

    private List<String> buildAcceptanceCriteria(String requirement) {
        return List.of(
                "系統可支援需求：" + requirement,
                "變更需與既有知識庫內容保持一致",
                "需附帶至少一項單元測試與一項整合測試計畫"
        );
    }

    private List<String> buildOutOfScopeItems(String requirement) {
        return List.of(
                "不包含 Phase 4 的 AI 自動 Coding 流程",
                "不包含與需求無直接相關的跨模組重構：" + requirement
        );
    }

    private List<String> buildUnitTests(String title) {
        return List.of(
                "為「" + title + "」新增主要服務邏輯單元測試",
                "覆蓋成功路徑與主要錯誤分支"
        );
    }

    private List<String> buildIntegrationTests(String title, List<String> affectedApis) {
        if (!affectedApis.isEmpty()) {
            return List.of(
                    "驗證 API 契約維持相容：" + String.join(", ", affectedApis),
                    "驗證資料流與主要整合點在「" + title + "」情境下正常運作"
            );
        }
        return List.of(
                "驗證主要工作流程在「" + title + "」情境下正常運作",
                "驗證資料持久化或跨模組互動未被破壞"
        );
    }

    private String determineRiskLevel(String requirement) {
        String text = requirement == null ? "" : requirement.toLowerCase();
        if (text.contains("付款") || text.contains("支付") || text.contains("刪除") || text.contains("遷移") || text.contains("migration")) {
            return "HIGH";
        }
        if (text.contains("通知") || text.contains("提醒") || text.contains("查詢") || text.contains("api")) {
            return "MEDIUM";
        }
        return "LOW";
    }
}
