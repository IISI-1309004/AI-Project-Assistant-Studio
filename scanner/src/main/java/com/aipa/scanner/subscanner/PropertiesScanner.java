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
 * PropertiesScanner — 掃描 application.yml / application.properties
 * 提取設定屬性清單（Phase 2 實作）
 */
@Component
public class PropertiesScanner implements SubScanner {

    @Override
    public String scannerName() { return "PropertiesScanner"; }

    @Override
    public boolean supports(Path projectRoot) {
        return Files.exists(projectRoot.resolve("src/main/resources/application.yml"))
                || Files.exists(projectRoot.resolve("src/main/resources/application.properties"));
    }

    @Override
    public PartialScanResult scan(Path projectRoot, ScanConfig config) {
        PartialScanResult result = new PartialScanResult();
        Path resourcesDir = projectRoot.resolve("src/main/resources");

        if (!Files.exists(resourcesDir)) return result;

        try {
            Files.walk(resourcesDir, 2).filter(p ->
                    p.getFileName().toString().startsWith("application") &&
                    (p.toString().endsWith(".yml") || p.toString().endsWith(".yaml") || p.toString().endsWith(".properties"))
            ).forEach(configFile -> {
                try {
                    String content = Files.readString(configFile);
                    String summary = buildConfigSummary(configFile.getFileName().toString(), content);
                    result.addFragment(new PartialScanResult.KnowledgeFragment(
                            "PROJECT",
                            "設定檔：" + configFile.getFileName(),
                            summary,
                            configFile.toString()
                    ));
                } catch (IOException ignored) {}
            });
        } catch (IOException ignored) {}

        return result;
    }

    private String buildConfigSummary(String fileName, String content) {
        List<String> keys = new ArrayList<>();
        // 提取 server.port、spring.datasource.url、spring.application.name 等關鍵設定
        Pattern ymlKey = Pattern.compile("^(\\s*)(\\w[\\w.\\-]*)\\s*:", Pattern.MULTILINE);
        var m = ymlKey.matcher(content);
        while (m.find() && keys.size() < 30) {
            String key = m.group(2);
            if (isSignificantKey(key)) keys.add(key);
        }

        // 提取特定值
        String port = extractValue(content, "port");
        String appName = extractValue(content, "name");
        String dbUrl = extractValue(content, "url");

        StringBuilder sb = new StringBuilder("設定檔：").append(fileName).append("\n");
        if (!port.isEmpty()) sb.append("Server Port：").append(port).append("\n");
        if (!appName.isEmpty()) sb.append("Application Name：").append(appName).append("\n");
        if (!dbUrl.isEmpty()) sb.append("DB URL Pattern：").append(maskDbUrl(dbUrl)).append("\n");
        if (!keys.isEmpty()) sb.append("主要設定項目：").append(String.join(", ", keys));
        return sb.toString();
    }

    private boolean isSignificantKey(String key) {
        return key.matches("port|name|url|driver|platform|dialect|enabled|active|level|path|timeout|size|max|min");
    }

    private String extractValue(String content, String key) {
        Pattern p = Pattern.compile(key + "\\s*[=:]\\s*([^\\n]+)");
        var m = p.matcher(content);
        return m.find() ? m.group(1).trim() : "";
    }

    private String maskDbUrl(String url) {
        // 遮罩密碼資訊
        return url.replaceAll("password=[^&\\s]+", "password=***")
                  .replaceAll(":[^@]+@", ":***@");
    }
}
