package com.aipa.scanner;

import java.nio.file.Path;

/**
 * SubScanner — 子掃描器介面
 */
public interface SubScanner {
    String scannerName();
    boolean supports(Path projectRoot);
    PartialScanResult scan(Path projectRoot);
}
