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
 * GradleScanner — 掃描 build.gradle / build.gradle.kts 取得相依關係（Phase 2 實作）
 */
@Component
public class GradleScanner implements SubScanner {

    @Override
    public String scannerName() { return "GradleScanner"; }

    @Override
    public boolean supports(Path projectRoot) {
        return Files.exists(projectRoot.resolve("build.gradle"))
                || Files.exists(projectRoot.resolve("build.gradle.kts"));
    }

    @Override
    public PartialScanResult scan(Path projectRoot, ScanConfig config) {
        PartialScanResult result = new PartialScanResult();

        Path gradleFile = Files.exists(projectRoot.resolve("build.gradle.kts"))
                ? projectRoot.resolve("build.gradle.kts")
                : projectRoot.resolve("build.gradle");

        try {
            String content = Files.readString(gradleFile);
            List<String> dependencies = extractDependencies(content);
            String javaVersion = extractJavaVersion(content);

            StringBuilder sb = new StringBuilder("Gradle 專案相依關係分析：\n");
            if (!javaVersion.isEmpty()) sb.append("Java 版本：").append(javaVersion).append("\n");
            sb.append("主要相依（").append(dependencies.size()).append(" 個）：\n");
            dependencies.forEach(d -> sb.append("  - ").append(d).append("\n"));

            result.addFragment(new PartialScanResult.KnowledgeFragment(
                    "DEPENDENCY",
                    "Gradle 相依關係",
                    sb.toString(),
                    gradleFile.toString()
            ));
        } catch (IOException ignored) {}

        return result;
    }

    private List<String> extractDependencies(String gradle) {
        List<String> deps = new ArrayList<>();
        Pattern p = Pattern.compile("(?:implementation|api|compileOnly|runtimeOnly|testImplementation)\\s*[\\(\"']([^\"'\\)]+)[\"'\\)]");
        var m = p.matcher(gradle);
        while (m.find()) deps.add(m.group(1).trim());
        return deps;
    }

    private String extractJavaVersion(String gradle) {
        Pattern p = Pattern.compile("(?:JavaVersion\\.VERSION_(\\d+)|languageVersion\\.set\\(JavaLanguageVersion\\.of\\((\\d+)\\))");
        var m = p.matcher(gradle);
        return m.find() ? (m.group(1) != null ? m.group(1) : m.group(2)) : "";
    }
}
