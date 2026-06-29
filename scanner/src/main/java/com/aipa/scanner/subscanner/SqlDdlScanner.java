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
 * SqlDdlScanner — 掃描 SQL DDL 檔案，提取 Table / Column 定義（Phase 2 實作）
 */
@Component
public class SqlDdlScanner implements SubScanner {

    @Override
    public String scannerName() { return "SqlDdlScanner"; }

    @Override
    public boolean supports(Path projectRoot) {
        try {
            return Files.walk(projectRoot, 8)
                    .anyMatch(p -> p.toString().endsWith(".sql"));
        } catch (IOException e) { return false; }
    }

    @Override
    public PartialScanResult scan(Path projectRoot, ScanConfig config) {
        PartialScanResult result = new PartialScanResult();

        try {
            Files.walk(projectRoot, 8)
                .filter(p -> p.toString().endsWith(".sql") && !isExcluded(p, config))
                .forEach(sqlFile -> {
                    try {
                        String content = Files.readString(sqlFile);
                        List<TableDefinition> tables = extractTables(content);
                        if (!tables.isEmpty()) {
                            tables.forEach(table -> result.addFragment(
                                new PartialScanResult.KnowledgeFragment(
                                    "DATABASE",
                                    "資料表：" + table.name(),
                                    buildTableDescription(table),
                                    sqlFile.toString()
                                )
                            ));
                        }
                    } catch (IOException ignored) {}
                });
        } catch (IOException ignored) {}

        return result;
    }

    private List<TableDefinition> extractTables(String sql) {
        List<TableDefinition> tables = new ArrayList<>();
        // 匹配 CREATE TABLE 語句
        Pattern createTable = Pattern.compile(
            "CREATE\\s+TABLE\\s+(?:IF\\s+NOT\\s+EXISTS\\s+)?[`'\"]?(\\w+)[`'\"]?\\s*\\(([^;]+)\\)",
            Pattern.CASE_INSENSITIVE | Pattern.DOTALL
        );
        var m = createTable.matcher(sql);
        while (m.find()) {
            String tableName = m.group(1);
            String columnDefs = m.group(2);
            List<String> columns = extractColumns(columnDefs);
            tables.add(new TableDefinition(tableName, columns));
        }
        return tables;
    }

    private List<String> extractColumns(String columnDefs) {
        List<String> columns = new ArrayList<>();
        Pattern colPattern = Pattern.compile(
            "^\\s*[`'\"]?(\\w+)[`'\"]?\\s+(\\w+(?:\\([^)]+\\))?)",
            Pattern.MULTILINE
        );
        var m = colPattern.matcher(columnDefs);
        while (m.find()) {
            String colName = m.group(1).toLowerCase();
            // 排除約束關鍵字
            if (!colName.matches("primary|unique|foreign|key|constraint|index|check")) {
                columns.add(m.group(1) + " " + m.group(2));
            }
        }
        return columns;
    }

    private String buildTableDescription(TableDefinition table) {
        StringBuilder sb = new StringBuilder();
        sb.append("資料表名稱：").append(table.name()).append("\n");
        sb.append("欄位數量：").append(table.columns().size()).append("\n");
        if (!table.columns().isEmpty()) {
            sb.append("欄位定義：\n");
            table.columns().forEach(c -> sb.append("  - ").append(c).append("\n"));
        }
        return sb.toString();
    }

    private boolean isExcluded(Path path, ScanConfig config) {
        String pathStr = path.toString().replace("\\", "/");
        return config.excludePatterns().stream().anyMatch(pattern -> {
            String regex = pattern.replace("**", ".*").replace("*", "[^/]*");
            return pathStr.matches(".*" + regex + ".*");
        });
    }

    record TableDefinition(String name, List<String> columns) {}
}
