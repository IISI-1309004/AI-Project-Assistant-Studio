package com.aipa.scanner;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.*;
import com.github.javaparser.ast.expr.AnnotationExpr;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;

import java.io.IOException;
import java.nio.file.*;
import java.util.*;
import java.util.stream.Stream;

/**
 * JavaSourceScanner — 解析 .java 原始碼，建立類別清單與 Call Graph
 */
public class JavaSourceScanner implements SubScanner {

    @Override
    public String scannerName() {
        return "JavaSourceScanner";
    }

    @Override
    public boolean supports(Path projectRoot) {
        return Files.exists(projectRoot.resolve("src/main/java"));
    }

    @Override
    public PartialScanResult scan(Path projectRoot) {
        List<ClassInfo> classes = new ArrayList<>();
        Path javaRoot = projectRoot.resolve("src/main/java");

        try (Stream<Path> paths = Files.walk(javaRoot)) {
            paths.filter(p -> p.toString().endsWith(".java"))
                 .forEach(file -> {
                     try {
                         CompilationUnit cu = StaticJavaParser.parse(file);
                         cu.findAll(ClassOrInterfaceDeclaration.class).forEach(cls -> {
                             ClassInfo info = extractClassInfo(cu, cls, file, javaRoot);
                             classes.add(info);
                         });
                     } catch (IOException e) {
                         // 略過無法解析的檔案
                     }
                 });
        } catch (IOException e) {
            // 略過無法遍歷的目錄
        }

        return PartialScanResult.ofClasses(classes);
    }

    private ClassInfo extractClassInfo(CompilationUnit cu, ClassOrInterfaceDeclaration cls,
                                        Path file, Path javaRoot) {
        String packageName = cu.getPackageDeclaration()
                .map(pd -> pd.getNameAsString()).orElse("");
        String className = cls.getNameAsString();
        String fqn = packageName.isEmpty() ? className : packageName + "." + className;

        List<String> annotations = cls.getAnnotations().stream()
                .map(AnnotationExpr::getNameAsString)
                .toList();

        List<MethodInfo> methods = cls.getMethods().stream()
                .map(m -> new MethodInfo(
                        m.getNameAsString(),
                        m.getTypeAsString(),
                        m.getParameters().stream()
                                .map(p -> p.getTypeAsString()).toList(),
                        m.getAnnotations().stream()
                                .map(AnnotationExpr::getNameAsString).toList(),
                        extractMethodCalls(m)
                )).toList();

        String relativePath = javaRoot.relativize(file).toString().replace("\\", "/");

        return new ClassInfo(fqn, className, packageName, annotations, methods,
                cls.isInterface(), relativePath);
    }

    private List<String> extractMethodCalls(MethodDeclaration method) {
        List<String> calls = new ArrayList<>();
        method.accept(new VoidVisitorAdapter<Void>() {
            @Override
            public void visit(MethodCallExpr n, Void arg) {
                super.visit(n, arg);
                n.getScope().ifPresent(scope ->
                        calls.add(scope.toString() + "." + n.getNameAsString()));
            }
        }, null);
        return calls;
    }

    // ── 資料模型 ────────────────────────────────────────────────
    public record ClassInfo(
            String fqn,
            String simpleName,
            String packageName,
            List<String> annotations,
            List<MethodInfo> methods,
            boolean isInterface,
            String relativePath
    ) {}

    public record MethodInfo(
            String name,
            String returnType,
            List<String> parameterTypes,
            List<String> annotations,
            List<String> callees
    ) {}
}
