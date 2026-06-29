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
 * MavenScanner — 掃描 pom.xml 取得相依關係（Phase 2 實作）
 */
@Component
public class MavenScanner implements SubScanner {

    @Override
    public String scannerName() { return "MavenScanner"; }

    @Override
    public boolean supports(Path projectRoot) {
        return Files.exists(projectRoot.resolve("pom.xml"));
    }

    @Override
    public PartialScanResult scan(Path projectRoot, ScanConfig config) {
        PartialScanResult result = new PartialScanResult();
        Path pomFile = projectRoot.resolve("pom.xml");

        try {
            String content = Files.readString(pomFile);
            List<String> dependencies = extractDependencies(content);
            String javaVersion = extractJavaVersion(content);
            String springBootVersion = extractSpringBootVersion(content);

            StringBuilder sb = new StringBuilder("Maven 專案相依關係分析：\n");
            if (!javaVersion.isEmpty()) sb.append("Java 版本：").append(javaVersion).append("\n");
            if (!springBootVersion.isEmpty()) sb.append("Spring Boot 版本：").append(springBootVersion).append("\n");
            sb.append("主要相依（").append(dependencies.size()).append(" 個）：\n");
            dependencies.forEach(d -> sb.append("  - ").append(d).append("\n"));

            result.addFragment(new PartialScanResult.KnowledgeFragment(
                    "DEPENDENCY",
                    "Maven 相依關係",
                    sb.toString(),
                    pomFile.toString()
            ));
        } catch (IOException ignored) {}

        return result;
    }

    private List<String> extractDependencies(String pom) {
        List<String> deps = new ArrayList<>();
        Pattern p = Pattern.compile(
            "<dependency>\\s*<groupId>([^<]+)</groupId>\\s*<artifactId>([^<]+)</artifactId>(?:\\s*<version>([^<]+)</version>)?",
            Pattern.DOTALL
        );
        var m = p.matcher(pom);
        while (m.find()) {
            String dep = m.group(1).trim() + ":" + m.group(2).trim();
            if (m.group(3) != null) dep += ":" + m.group(3).trim();
            deps.add(dep);
        }
        return deps;
    }

    private String extractJavaVersion(String pom) {
        Pattern p = Pattern.compile("<java\\.version>([^<]+)</java\\.version>|<maven\\.compiler\\.source>([^<]+)</maven\\.compiler\\.source>");
        var m = p.matcher(pom);
        return m.find() ? (m.group(1) != null ? m.group(1) : m.group(2)).trim() : "";
    }

    private String extractSpringBootVersion(String pom) {
        Pattern p = Pattern.compile("spring-boot[^<]*<version>([^<]+)</version>|<version>([\\d.]+)</version>.*spring-boot");
        var m = p.matcher(pom);
        return m.find() ? (m.group(1) != null ? m.group(1) : m.group(2)).trim() : "";
    }
}
