package com.aipa.scanner;

import java.nio.file.Path;
import java.time.Instant;

/**
 * ScannerEngine — 靜態分析專案程式碼庫介面
 */
public interface ScannerEngine {
    ScanResult scanProject(Path projectRoot);
    ScanResult incrementalScan(Path projectRoot, Instant since);
    TechStack detectTechStack(Path projectRoot);
}
