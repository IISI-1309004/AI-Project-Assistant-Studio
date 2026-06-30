package com.aipa.scanner;

import com.aipa.scanner.subscanner.JavaSourceScanner;

import java.util.ArrayList;
import java.util.List;

/**
 * PartialScanResult — 子掃描器的部分結果，最終合併為 ScanResult
 */
public class PartialScanResult {

    private final List<KnowledgeFragment> fragments = new ArrayList<>();

    public void addFragment(KnowledgeFragment fragment) {
        fragments.add(fragment);
    }

    public List<KnowledgeFragment> getFragments() {
        return List.copyOf(fragments);
    }

    public boolean isEmpty() {
        return fragments.isEmpty();
    }

    /**
     * 從 ClassInfo 清單建構 PartialScanResult（JavaSourceScanner 使用）
     */
    public static PartialScanResult ofClasses(List<JavaSourceScanner.ClassInfo> classes) {
        PartialScanResult result = new PartialScanResult();
        if (classes.isEmpty()) return result;

        // 逐類別建立知識片段
        for (JavaSourceScanner.ClassInfo cls : classes) {
            String desc = buildClassDescription(cls);
            result.addFragment(new KnowledgeFragment("ARCHITECTURE", cls.simpleName(), desc, cls.relativePath()));
        }

        // 整體套件結構摘要
        result.addFragment(new KnowledgeFragment("PROJECT", "Java 套件結構", buildPackageSummary(classes), "src/main/java"));
        return result;
    }

    private static String buildClassDescription(JavaSourceScanner.ClassInfo cls) {
        StringBuilder sb = new StringBuilder();
        sb.append(cls.isInterface() ? "介面" : "類別").append("：").append(cls.fqn()).append("\n");
        if (!cls.annotations().isEmpty()) {
            sb.append("注解：").append(String.join(", ", cls.annotations())).append("\n");
        }
        if (!cls.methods().isEmpty()) {
            sb.append("方法（").append(cls.methods().size()).append("）：");
            sb.append(String.join(", ", cls.methods().stream().map(JavaSourceScanner.MethodInfo::name).limit(10).toList()));
        }
        return sb.toString();
    }

    private static String buildPackageSummary(List<JavaSourceScanner.ClassInfo> classes) {
        long controllers = classes.stream().filter(c ->
                c.annotations().stream().anyMatch(a -> a.contains("Controller"))).count();
        long services = classes.stream().filter(c ->
                c.annotations().contains("Service")).count();
        long repositories = classes.stream().filter(c ->
                c.annotations().contains("Repository")).count();
        long entities = classes.stream().filter(c ->
                c.annotations().contains("Entity")).count();
        long packages = classes.stream().map(JavaSourceScanner.ClassInfo::packageName).distinct().count();

        return String.format(
                "共 %d 個類別，%d 個套件\nController: %d, Service: %d, Repository: %d, Entity: %d",
                classes.size(), packages, controllers, services, repositories, entities
        );
    }

    public record KnowledgeFragment(
            String category,
            String title,
            String content,
            String sourceFile
    ) {}
}
