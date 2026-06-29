package com.aipa.scanner;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * ScanResult — Scanner Engine 的輸出結果（Phase 2 完整版）
 */
public final class ScanResult {

    private final ProjectMeta projectMeta;
    private final ApiInventory apiInventory;
    private final DatabaseSchema databaseSchema;
    private final ArchitectureGraph architectureGraph;
    private final DependencyTree dependencyTree;
    private final Instant scannedAt;
    private final List<PartialScanResult.KnowledgeFragment> fragments;

    public ScanResult(
            ProjectMeta projectMeta,
            ApiInventory apiInventory,
            DatabaseSchema databaseSchema,
            ArchitectureGraph architectureGraph,
            DependencyTree dependencyTree,
            Instant scannedAt
    ) {
        this(projectMeta, apiInventory, databaseSchema, architectureGraph, dependencyTree, scannedAt, List.of());
    }

    public ScanResult(
            ProjectMeta projectMeta,
            ApiInventory apiInventory,
            DatabaseSchema databaseSchema,
            ArchitectureGraph architectureGraph,
            DependencyTree dependencyTree,
            Instant scannedAt,
            List<PartialScanResult.KnowledgeFragment> fragments
    ) {
        this.projectMeta = projectMeta;
        this.apiInventory = apiInventory;
        this.databaseSchema = databaseSchema;
        this.architectureGraph = architectureGraph;
        this.dependencyTree = dependencyTree;
        this.scannedAt = scannedAt;
        this.fragments = List.copyOf(fragments);
    }

    public ScanResult withFragments(List<PartialScanResult.KnowledgeFragment> fragments) {
        return new ScanResult(projectMeta, apiInventory, databaseSchema,
                architectureGraph, dependencyTree, scannedAt, fragments);
    }

    public static ScanResult empty() {
        return new ScanResult(
                new ProjectMeta("unknown", "unknown", List.of(), List.of()),
                new ApiInventory(List.of()),
                new DatabaseSchema(List.of()),
                new ArchitectureGraph(List.of()),
                new DependencyTree(List.of()),
                Instant.now()
        );
    }

    // Getters
    public ProjectMeta getProjectMeta() { return projectMeta; }
    public ApiInventory getApiInventory() { return apiInventory; }
    public DatabaseSchema getDatabaseSchema() { return databaseSchema; }
    public ArchitectureGraph getArchitectureGraph() { return architectureGraph; }
    public DependencyTree getDependencyTree() { return dependencyTree; }
    public Instant getScannedAt() { return scannedAt; }
    public List<PartialScanResult.KnowledgeFragment> getFragments() { return fragments; }
    public int getFragmentCount() { return fragments.size(); }

    public record ProjectMeta(
            String name,
            String javaVersion,
            List<String> frameworks,
            List<String> databases
    ) {}

    public record ApiInventory(List<String> endpoints) {}
    public record DatabaseSchema(List<String> tables) {}
    public record ArchitectureGraph(List<String> layers) {}
    public record DependencyTree(List<String> dependencies) {}
}
