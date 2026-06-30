# Phase 9 第二切片 — Runtime API IP 白名單 + 結構化 JSON 日誌

## 概述

本切片實作 **企業級安全強化** 的核心功能，包括：
1. **Runtime API IP 白名單** — 限制 API 存取來源 IP
2. **結構化 JSON 日誌** — 統一 Runtime 和 AI Engine 的日誌格式

## 已落地功能

### 1. Runtime API IP 白名單 (Spring Filter)

**新檔案**: `runtime/src/main/java/com/aipa/runtime/config/IpWhitelistFilter.java`

**功能**:
- Spring Filter 層級的 IP 攔截
- 支援精確 IP、CIDR 段匹配
- 本地測試環境 (127.0.0.1) 預設白名單
- 後方代理 (X-Forwarded-For, X-Real-IP) 支援
- 拒絕未授權 IP 時返回 403 Forbidden
- 搭配安全審計日誌記錄

**配置選項** (`application.yml`):
```yaml
aipa:
  security:
    # IP 白名單 (逗号分隔)
    # 支援格式: 精確 IP (192.168.1.1), CIDR 段 (192.168.0.0/16)
    ip-whitelist: "127.0.0.1,::1"
    # 本地開發禁用，生產環境啟用
    enable-ip-whitelist: false
```

**使用示例** (生產部署):
```yaml
aipa:
  security:
    enable-ip-whitelist: true
    ip-whitelist: "127.0.0.1,::1,192.168.1.0/24,10.0.0.0/8"
```

### 2. 結構化 JSON 日誌（Runtime）

**新檔案**: `runtime/src/main/resources/logback-spring.xml`

**功能**:
- JSON 格式日誌 (使用 logstash-logback-encoder)
- 控制台 + 檔案 + 審計日誌三層輸出
- 自動日誌滾動備份 (10MB 單檔，最多 30 檔)
- 安全審計日誌獨立分流 (50MB/90 個檔案)

**日誌範例** (JSON 格式):
```json
{
  "timestamp": "2026-06-30T12:34:56.789Z",
  "level": "INFO",
  "logger": "com.aipa.runtime.api.controller.SessionController",
  "message": "建立新的工作階段",
  "thread": "http-nio-8080-exec-1",
  "service": "runtime",
  "version": "1.0.0-SNAPSHOT"
}
```

**安全審計日誌範例**:
```json
{
  "timestamp": "2026-06-30T12:34:56.789Z",
  "level": "WARN",
  "message": "IP 192.168.1.100 被 IP 白名單過濾器拒絕",
  "service": "runtime",
  "auditType": "security",
  "severity": "security"
}
```

### 3. 結構化 JSON 日誌（AI Engine）

**新檔案**: `aipa_ai_engine/logging_config.py`

**功能**:
- Python 統一 JSON 日誌格式化
- traceId 支援 (用於分繁式追蹤)
- 自訂欄位支援 (user_id, ip_address 等)
- 檔案滾動備份 (10MB/30 檔，審計 50MB/90 檔)
- 安全審計日誌分流

**使用示例**:
```python
from aipa_ai_engine.logging_config import setup_json_logging, get_audit_logger, log_with_context

# 初始化日誌
logger = setup_json_logging(
    service_name="ai-engine",
    version="1.0.0-SNAPSHOT",
    log_directory="/var/log/aipa"
)

# 記錄普通日誌
logger.info("知識庫更新完成", extra={"items_processed": 100})

# 記錄帶自訂欄位的日誌
log_with_context(
    logger, "WARN", "異常 API 呼叫",
    user_id="user123",
    endpoint="/engine/knowledge/search",
    response_time_ms=5000
)

# 審計日誌
audit_logger = get_audit_logger()
audit_logger.info("使用者權限異動: user123 從 EDITOR 升級為 ADMIN")
```

**日誌檔案輸出**:
- `aipa-runtime-json.log` — 應用日誌
- `aipa-runtime-audit.log` — 安全審計
- `aipa-ai-engine-json.log` — AI Engine 應用日誌
- `aipa-ai-engine-audit.log` — AI Engine 審計日誌

### 4. 依賴更新

**Runtime** (`build.gradle.kts`):
```kotlin
// Phase 9: 結構化 JSON 日誌
implementation("net.logstash.logback:logstash-logback-encoder:7.4")
```

**AI Engine** (`pyproject.toml`):
```toml
# Phase 9: 結構化 JSON 日誌
python-json-logger = "^2.0.7"
```

## 測試

### Runtime IP 白名單測試
```bash
cd runtime
./gradlew test --tests IpWhitelistFilterTests
```

**測試用例**:
- ✓ 本地 IP (127.0.0.1) 總是允許
- ✓ IPv6 本地 (::1) 允許
- ✓ 代理頭支援 (X-Forwarded-For)
- ✓ 無效 IP 拒絕 (若啟用)

### AI Engine JSON 日誌測試
```bash
cd specs
python test_phase9_logging.py
```

**測試用例**:
- ✓ JSON 格式化基礎欄位
- ✓ traceId 一致性與隔離
- ✓ 日誌設定與檔案輸出
- ✓ 安全審計日誌分流
- ✓ 自訂欄位注入
- ✓ 異常堆疊追蹤

## 部署配置範例

### Docker Compose 擴展
```yaml
# 在 docker-compose.yml 中為 Runtime 服務添加
services:
  runtime:
    # ... 其他設定 ...
    environment:
      - AIPA_SECURITY_ENABLE_IP_WHITELIST=true
      - AIPA_SECURITY_IP_WHITELIST=127.0.0.1,::1,172.20.0.0/16
      - LOG_PATH=/var/log/aipa
    volumes:
      - aipa-logs:/var/log/aipa
```

### Kubernetes 日誌聚集
```yaml
# values.yaml 配置
aipa:
  security:
    ipWhitelist:
      enabled: true
      cidrs:
        - 127.0.0.1
        - ::1
        - 10.0.0.0/8
  logging:
    format: json
    auditEnabled: true
    logDirectory: /var/log/aipa
```

## 下一步 (Phase 9 第三切片)

1. **RBAC (Role-Based Access Control)**
   - 角色定義 (VIEWER, OPERATOR, ADMIN, SUPER_ADMIN)
   - 權限校驗註解 (@Authorized)
   - 資源級別存取控制

2. **使用者與權限管理**
   - 使用者表 + 角色關聯
   - Token-based 認證 (JWT)
   - 權限快取

3. **進階審計日誌**
   - 操作追蹤 (誰在何時進行了何操作)
   - 資料變更歷史
   - 合規性報告生成

## 復驗清單

- [x] IP 白名單 Filter 實作
- [x] CIDR 段匹配邏輯
- [x] 代理頭支援
- [x] Runtime 結構化 JSON 日誌
- [x] AI Engine 結構化 JSON 日誌
- [x] 審計日誌分流
- [x] 依賴版本更新
- [x] 單元測試與集成測試
- [ ] 負載測試 (日誌輸出性能)
- [ ] 安全掃描 (基於 CVE)

## 貢獻指南

若要擴展或修改此功能：

1. **新增 IP 匹配規則**:
   - 編輯 `IpWhitelistFilter.java` 中的 `parseWhitelist()` 方法
   - 實作新的 `IpMatcher` 子類 (如 IPv6 CIDR)

2. **自訂日誌欄位**:
   - Python: 使用 `log_with_context()` 函式注入欄位
   - Java: 設定 LogstashEncoder 的 `customFields`

3. **日誌分流規則**:
   - 編輯 `logback-spring.xml` 的 `<logger>` 匹配規則
   - 或在 Python 中建立特殊 logger (如 `audit`, `performance`)

---
**作者**: AIPA Studio Team
**版本**: 1.0.0-SNAPSHOT
**最後更新**: 2026-06-30
**階段**: Phase 9 第二切片 — 企業級安全強化

