package com.aipa.scanner.subscanner;

import com.aipa.scanner.PartialScanResult;
import com.aipa.scanner.ScanConfig;
import com.aipa.scanner.SubScanner;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * OpenApiScanner — 掃描 OpenAPI / Swagger 規格檔案（Phase 2 實作）
 */
@Component
public class OpenApiScanner implements SubScanner {

    @Override
    public String scannerName() { return "OpenApiScanner"; }

    @Override
    public boolean supports(Path projectRoot) {
        try {
            return Files.walk(projectRoot, 5).anyMatch(p -> {
                String name = p.getFileName().toString().toLowerCase();
                return name.contains("openapi") || name.contains("swagger") || name.contains("api-docs");
            });
        } catch (IOException e) { return false; }
    }

    @Override
    public PartialScanResult scan(Path projectRoot, ScanConfig config) {
        PartialScanResult result = new PartialScanResult();

        try {
            Files.walk(projectRoot, 5)
                .filter(p -> {
                    String name = p.getFileName().toString().toLowerCase();
                    return (name.contains("openapi") || name.contains("swagger"))
                            && (name.endsWith(".yml") || name.endsWith(".yaml") || name.endsWith(".json"));
                })
                .forEach(specFile -> {
                    try {
                        String content = Files.readString(specFile);
                        List<String> endpoints = extractEndpoints(content);
                        String summary = buildApiSummary(specFile.getFileName().toString(), endpoints);
                        result.addFragment(new PartialScanResult.KnowledgeFragment(
                                "API",
                                "API 規格：" + specFile.getFileName(),
                                summary,
                                specFile.toString()
                        ));
                    } catch (IOException ignored) {}
                });
        } catch (IOException ignored) {}

        // 也掃描 @RequestMapping / @GetMapping 等 Spring MVC 端點
        try {
            List<String> springEndpoints = scanSpringMvcEndpoints(projectRoot, config);
            if (!springEndpoints.isEmpty()) {
                result.addFragment(new PartialScanResult.KnowledgeFragment(
                        "API",
                        "Spring MVC API 端點",
                        "從 @RequestMapping/@GetMapping 等注解掃描到的 API 端點：\n" +
                        String.join("\n", springEndpoints),
                        projectRoot.toString()
                ));
            }
        } catch (IOException ignored) {}

        return result;
    }

    private List<String> extractEndpoints(String content) {
        List<String> endpoints = new ArrayList<>();
        Pattern p = Pattern.compile("(get|post|put|delete|patch)\\s*:\\s*\\n\\s+(/.+)", Pattern.CASE_INSENSITIVE);
        var m = p.matcher(content);
        while (m.find() && endpoints.size() < 50) {
            endpoints.add(m.group(1).toUpperCase() + " " + m.group(2).trim());
        }
        return endpoints;
    }

    private List<String> scanSpringMvcEndpoints(Path projectRoot, ScanConfig config) throws IOException {
        List<String> endpoints = new ArrayList<>();
        Pattern mapping = Pattern.compile("@(GetMapping|PostMapping|PutMapping|DeleteMapping|RequestMapping)\\s*\\(\\s*(?:value\\s*=\\s*)?\"([^\"]+)\"");

        Files.walk(projectRoot)
            .filter(p -> p.toString().endsWith(".java") && !isExcluded(p, config))
            .forEach(javaFile -> {
                try {
                    String content = Files.readString(javaFile);
                    var m = mapping.matcher(content);
                    while (m.find() && endpoints.size() < 100) {
                        String method = m.group(1).replace("Mapping", "").toUpperCase();
                        if (method.equals("REQUEST")) method = "ANY";
                        endpoints.add(method + " " + m.group(2));
                    }
                } catch (IOException ignored) {}
            });

        return endpoints;
    }

    private String buildApiSummary(String fileName, List<String> endpoints) {
        StringBuilder sb = new StringBuilder("OpenAPI 規格：").append(fileName).append("\n");
        sb.append("端點數量：").append(endpoints.size()).append("\n");
        if (!endpoints.isEmpty()) {
            sb.append("端點清單：\n");
            endpoints.stream().limit(20).forEach(e -> sb.append("  ").append(e).append("\n"));
        }
        return sb.toString();
    }

    private boolean isExcluded(Path path, ScanConfig config) {
        String pathStr = path.toString().replace("\\", "/");
        return config.excludePatterns().stream().anyMatch(pattern -> {
            String regex = pattern.replace("**", ".*").replace("*", "[^/]*");
            return pathStr.matches(".*" + regex + ".*");
        });
    }
}
