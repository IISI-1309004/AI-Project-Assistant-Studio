package com.aipa.scanner;

import java.nio.file.Path;

/**
 * SubScanner — 子掃描器介面
 */
public interface SubScanner {
    String scannerName();
    boolean supports(Path projectRoot);

    default PartialScanResult scan(Path projectRoot) {
        return scan(projectRoot, ScanConfig.defaults());
    }

    PartialScanResult scan(Path projectRoot, ScanConfig config);
}
