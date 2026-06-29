package com.aipa.runtime.service;

import com.aipa.scanner.ScanResult;
import com.aipa.scanner.ScannerEngine;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Service
public class ProjectInitService {

    private final ScannerEngine scannerEngine;
    private final KnowledgeEngineClient knowledgeEngineClient;
    private final ObjectMapper objectMapper;
    private final ConcurrentMap<String, InitJobStatus> jobs = new ConcurrentHashMap<>();

    public ProjectInitService(
            ScannerEngine scannerEngine,
            KnowledgeEngineClient knowledgeEngineClient,
            ObjectMapper objectMapper
    ) {
        this.scannerEngine = scannerEngine;
        this.knowledgeEngineClient = knowledgeEngineClient;
        this.objectMapper = objectMapper;
    }

    public InitJobStatus startInitJob(Path projectRoot, String projectId) {
        String jobId = "job-" + UUID.randomUUID().toString().substring(0, 8);
        String resolvedProjectId = normalizeProjectId(projectRoot, projectId);

        InitJobStatus started = new InitJobStatus(
                jobId,
                "STARTED",
                5,
                "Initializing project workspace",
                projectRoot.toAbsolutePath().toString(),
                resolvedProjectId,
                Instant.now().toString(),
                null
        );
        jobs.put(jobId, started);

        CompletableFuture.runAsync(() -> runInit(jobId, projectRoot, resolvedProjectId));
        return started;
    }

    public InitJobStatus getStatus(String jobId) {
        return jobs.get(jobId);
    }

    private void runInit(String jobId, Path projectRoot, String projectId) {
        try {
            update(jobId, "RUNNING", 15, "Preparing .ai-project directories", null);
            prepareWorkspace(projectRoot);

            update(jobId, "RUNNING", 40, "Scanning source code", null);
            ScanResult scanResult = scannerEngine.scanProject(projectRoot);
            writeProjectDna(projectRoot, scanResult);

            update(jobId, "RUNNING", 75, "Ingesting scan result to Knowledge Engine", null);
            Map<String, Object> ingestResult = knowledgeEngineClient.bulkIngest(
                    projectId,
                    objectMapper.convertValue(scanResult, Map.class)
            );
            int created = ingestResult == null ? 0 : ((Number) ingestResult.getOrDefault("created", 0)).intValue();

            Map<String, Object> summary = Map.of(
                    "fragmentCount", scanResult.getFragmentCount(),
                    "knowledgeCreated", created,
                    "projectName", scanResult.getProjectMeta().name()
            );
            update(jobId, "COMPLETED", 100, "Project initialized", summary);
        } catch (Exception ex) {
            update(jobId, "FAILED", 100, "Init failed: " + ex.getMessage(), null);
        }
    }

    private void prepareWorkspace(Path projectRoot) throws IOException {
        if (!Files.exists(projectRoot) || !Files.isDirectory(projectRoot)) {
            throw new IOException("projectRoot does not exist or is not a directory: " + projectRoot);
        }
        Path aiProject = projectRoot.resolve(".ai-project");
        Files.createDirectories(aiProject.resolve("dna"));
        Files.createDirectories(aiProject.resolve("knowledge/db"));
        Files.createDirectories(aiProject.resolve("vector"));
    }

    private void writeProjectDna(Path projectRoot, ScanResult scanResult) throws IOException {
        Path dnaFile = projectRoot.resolve(".ai-project/dna/project-dna.json");
        Map<String, Object> dna = Map.of(
                "projectMeta", scanResult.getProjectMeta(),
                "architecture", scanResult.getArchitectureGraph(),
                "dependencies", scanResult.getDependencyTree(),
                "scannedAt", scanResult.getScannedAt().toString()
        );
        objectMapper.writerWithDefaultPrettyPrinter().writeValue(dnaFile.toFile(), dna);
    }

    private void update(String jobId, String status, int progress, String message, Map<String, Object> summary) {
        InitJobStatus old = jobs.get(jobId);
        if (old == null) {
            return;
        }
        jobs.put(jobId, new InitJobStatus(
                old.jobId(),
                status,
                progress,
                message,
                old.projectRoot(),
                old.projectId(),
                old.startedAt(),
                summary
        ));
    }

    private String normalizeProjectId(Path projectRoot, String projectId) {
        if (projectId != null && !projectId.isBlank()) {
            return projectId;
        }
        Path fileName = projectRoot.getFileName();
        return fileName == null ? "default" : fileName.toString().toLowerCase();
    }

    public record InitJobStatus(
            String jobId,
            String status,
            int progress,
            String message,
            String projectRoot,
            String projectId,
            String startedAt,
            Map<String, Object> summary
    ) {}
}



