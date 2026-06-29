package com.aipa.scanner;

import org.springframework.stereotype.Component;

import java.nio.file.Path;
import java.time.Instant;
import java.util.List;

/**
 * ScannerEngineImpl — Phase 2 完整實作
 */
@Component
public class ScannerEngineImpl implements ScannerEngine {

    private final List<SubScanner> subScanners;
    private final TechStackDetector techStackDetector;

    public ScannerEngineImpl(
            TechStackDetector techStackDetector,
            List<SubScanner> subScanners
    ) {
        this.techStackDetector = techStackDetector;
        this.subScanners = subScanners;
    }

    @Override
    public ScanResult scanProject(Path projectRoot) {
        return doScan(projectRoot);
    }

    @Override
    public ScanResult incrementalScan(Path projectRoot, Instant since) {
        return doScan(projectRoot);
    }

    @Override
    public TechStack detectTechStack(Path projectRoot) {
        return techStackDetector.detect(projectRoot);
    }

    private ScanResult doScan(Path projectRoot) {
        TechStack techStack = techStackDetector.detect(projectRoot);

        List<PartialScanResult.KnowledgeFragment> allFragments = subScanners.stream()
                .filter(s -> s.supports(projectRoot))
                .flatMap(s -> s.scan(projectRoot).getFragments().stream())
                .toList();

        var meta = new ScanResult.ProjectMeta(
                projectRoot.getFileName().toString(),
                techStack.javaVersion(),
                techStack.frameworks(),
                techStack.databases()
        );
        var api = new ScanResult.ApiInventory(
                allFragments.stream().filter(f -> "API".equals(f.category()))
                        .map(PartialScanResult.KnowledgeFragment::title).toList()
        );
        var db = new ScanResult.DatabaseSchema(
                allFragments.stream().filter(f -> "DATABASE".equals(f.category()))
                        .map(PartialScanResult.KnowledgeFragment::title).toList()
        );
        var arch = new ScanResult.ArchitectureGraph(
                allFragments.stream().filter(f -> "ARCHITECTURE".equals(f.category()))
                        .map(PartialScanResult.KnowledgeFragment::title).toList()
        );
        var deps = new ScanResult.DependencyTree(
                allFragments.stream().filter(f -> "DEPENDENCY".equals(f.category()))
                        .map(PartialScanResult.KnowledgeFragment::title).toList()
        );

        return new ScanResult(meta, api, db, arch, deps, Instant.now()).withFragments(allFragments);
    }
}
