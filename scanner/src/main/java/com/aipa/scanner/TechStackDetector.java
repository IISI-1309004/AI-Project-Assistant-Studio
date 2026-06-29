package com.aipa.scanner;

import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * TechStackDetector — 自動偵測專案技術棧（Phase 2 實作）
 */
@Component
public class TechStackDetector {

    public TechStack detect(Path projectRoot) {
        String javaVersion = detectJavaVersion(projectRoot);
        String springBootVersion = detectSpringBootVersion(projectRoot);
        String buildTool = detectBuildTool(projectRoot);
        List<String> frameworks = detectFrameworks(projectRoot);
        List<String> databases = detectDatabases(projectRoot);

        return new TechStack(javaVersion, springBootVersion, buildTool, frameworks, databases);
    }

    private String detectJavaVersion(Path root) {
        // 1. 嘗試從 pom.xml 讀取
        try {
            Path pom = root.resolve("pom.xml");
            if (Files.exists(pom)) {
                String content = Files.readString(pom);
                Pattern p = Pattern.compile("<java\\.version>([^<]+)</java\\.version>");
                var m = p.matcher(content);
                if (m.find()) return m.group(1).trim();
                p = Pattern.compile("<maven\\.compiler\\.source>([^<]+)</maven\\.compiler\\.source>");
                m = p.matcher(content);
                if (m.find()) return m.group(1).trim();
            }
        } catch (IOException ignored) {}

        // 2. 嘗試從 build.gradle.kts 讀取
        try {
            Path gradle = root.resolve("build.gradle.kts");
            if (!Files.exists(gradle)) gradle = root.resolve("build.gradle");
            if (Files.exists(gradle)) {
                String content = Files.readString(gradle);
                Pattern p = Pattern.compile("JavaVersion\\.VERSION_(\\d+)|languageVersion\\.set\\(JavaLanguageVersion\\.of\\((\\d+)\\)\\)");
                var m = p.matcher(content);
                if (m.find()) return m.group(1) != null ? m.group(1) : m.group(2);
            }
        } catch (IOException ignored) {}

        // 3. fallback: 系統 Java 版本
        return System.getProperty("java.version", "unknown").split("\\.")[0];
    }

    private String detectSpringBootVersion(Path root) {
        try {
            // pom.xml
            Path pom = root.resolve("pom.xml");
            if (Files.exists(pom)) {
                String content = Files.readString(pom);
                Pattern p = Pattern.compile("spring-boot-starter-parent.*?<version>([^<]+)</version>", Pattern.DOTALL);
                var m = p.matcher(content);
                if (m.find()) return m.group(1).trim();
            }
            // build.gradle.kts
            Path gradle = root.resolve("build.gradle.kts");
            if (!Files.exists(gradle)) gradle = root.resolve("build.gradle");
            if (Files.exists(gradle)) {
                String content = Files.readString(gradle);
                Pattern p = Pattern.compile("id\\(\"org\\.springframework\\.boot\"\\)\\s+version\\s+\"([^\"]+)\"");
                var m = p.matcher(content);
                if (m.find()) return m.group(1).trim();
            }
        } catch (IOException ignored) {}
        return "unknown";
    }

    private String detectBuildTool(Path root) {
        if (Files.exists(root.resolve("build.gradle.kts")) || Files.exists(root.resolve("build.gradle"))) return "GRADLE";
        if (Files.exists(root.resolve("pom.xml"))) return "MAVEN";
        return "UNKNOWN";
    }

    private List<String> detectFrameworks(Path root) {
        List<String> frameworks = new ArrayList<>();
        try {
            // 從相依設定中偵測框架
            String depContent = readDependencyFile(root);
            if (depContent.contains("spring-boot-starter-web") || depContent.contains("spring-webmvc")) frameworks.add("Spring MVC");
            if (depContent.contains("spring-boot-starter-security")) frameworks.add("Spring Security");
            if (depContent.contains("spring-boot-starter-batch")) frameworks.add("Spring Batch");
            if (depContent.contains("mybatis")) frameworks.add("MyBatis");
            if (depContent.contains("hibernate") || depContent.contains("spring-data-jpa")) frameworks.add("JPA/Hibernate");
            if (depContent.contains("spring-boot-starter-data-redis")) frameworks.add("Redis");
            if (depContent.contains("spring-cloud")) frameworks.add("Spring Cloud");
            // 前端偵測
            if (Files.exists(root.resolve("src/main/resources/static"))
                    || Files.exists(root.resolve("frontend"))) {
                if (Files.exists(root.resolve("package.json"))) {
                    String pkg = Files.readString(root.resolve("package.json"));
                    if (pkg.contains("\"vue\"")) frameworks.add("Vue");
                    if (pkg.contains("\"react\"")) frameworks.add("React");
                }
            }
        } catch (IOException ignored) {}
        return frameworks;
    }

    private List<String> detectDatabases(Path root) {
        List<String> dbs = new ArrayList<>();
        try {
            String content = readDependencyFile(root);
            String appConfig = readApplicationConfig(root);
            String combined = content + appConfig;

            if (combined.contains("oracle") || combined.contains("ojdbc")) dbs.add("Oracle");
            if (combined.contains("postgresql") || combined.contains("postgres")) dbs.add("PostgreSQL");
            if (combined.contains("mysql")) dbs.add("MySQL");
            if (combined.contains("sqlserver") || combined.contains("mssql")) dbs.add("SQL Server");
            if (combined.contains("h2")) dbs.add("H2");
            if (combined.contains("sqlite")) dbs.add("SQLite");
        } catch (IOException ignored) {}
        return dbs;
    }

    private String readDependencyFile(Path root) throws IOException {
        Path gradle = root.resolve("build.gradle.kts");
        if (!Files.exists(gradle)) gradle = root.resolve("build.gradle");
        if (Files.exists(gradle)) return Files.readString(gradle).toLowerCase();
        Path pom = root.resolve("pom.xml");
        if (Files.exists(pom)) return Files.readString(pom).toLowerCase();
        return "";
    }

    private String readApplicationConfig(Path root) throws IOException {
        Path yml = root.resolve("src/main/resources/application.yml");
        if (Files.exists(yml)) return Files.readString(yml).toLowerCase();
        Path props = root.resolve("src/main/resources/application.properties");
        if (Files.exists(props)) return Files.readString(props).toLowerCase();
        return "";
    }
}
