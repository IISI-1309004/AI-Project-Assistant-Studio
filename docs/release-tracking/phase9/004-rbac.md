# Phase 9 第三切片 — 角色型存取控制 (RBAC) 與權限管理

## 概述

本切片實作 **企業級安全強化** 的用戶權限管理，包括：
1. **五層角色定義** — SUPER_ADMIN、ADMIN、OPERATOR、VIEWER、GUEST
2. **註解型權限檢查** — @Authorized 裝飾器
3. **AOP 攔截器** — 動態權限驗證
4. **全域異常處理** — 統一安全回應

## 已落地功能

### 1. 角色定義 (AIRole 枚舉)

**新檔案**: `runtime/src/main/java/com/aipa/runtime/security/AIRole.java`

**五層角色階層**:

| 角色 | 級別 | 權限 |
|------|------|------|
| **SUPER_ADMIN** (超級管理員) | 99 | 完全存取、系統設定、用戶管理、審計 |
| **ADMIN** (管理員) | 50 | 系統監控、日誌檢視、受限用戶管理、Checkpoint 核審 |
| **OPERATOR** (操作員) | 30 | Checkpoint 核審、工作階段管理、意見反饋 |
| **VIEWER** (檢視者) | 10 | 唯讀存取、Session/Checkpoint/知識庫檢視 |
| **GUEST** (訪客) | 1 | 最小化唯讀、公開文件檢視 |

**使用範例**:
```java
AIRole userRole = AIRole.OPERATOR;

// 檢查級別
if (userRole.hasLevel(30)) {
    // 操作員或更高權限
}

// 比較角色
if (userRole.hasLevelOrHigher(AIRole.VIEWER)) {
    // 至少是檢視者或更高
}
```

### 2. 權限註解 (@Authorized)

**新檔案**: `runtime/src/main/java/com/aipa/runtime/security/Authorized.java`

**使用範例**:

```java
@RestController
@RequestMapping("/api/v1/checkpoints")
public class CheckpointController {

    // 管理員或更高可核審
    @Authorized(role = AIRole.ADMIN, description = "核准檢查點")
    @PostMapping("/{id}/approve")
    public ResponseEntity<Map> approveCheckpoint(@PathVariable String id) {
        // ...
    }

    // 操作員可核審
    @Authorized(role = AIRole.OPERATOR, description = "初審檢查點")
    @PostMapping("/{id}/review")
    public ResponseEntity<Map> reviewCheckpoint(@PathVariable String id) {
        // ...
    }

    // 檢視者可檢視
    @Authorized(minLevel = 10, description = "檢視檢查點清單")
    @GetMapping
    public ResponseEntity<List<CheckpointDTO>> listCheckpoints() {
        // ...
    }

    // 多角色支援
    @Authorized(multiRole = true, roles = {AIRole.ADMIN, AIRole.OPERATOR})
    @PostMapping("/{id}/comment")
    public ResponseEntity<Map> addComment(@PathVariable String id, @RequestBody String comment) {
        // ...
    }
}
```

### 3. AOP 攔截器 (RBACInterceptor)

**新檔案**: `runtime/src/main/java/com/aipa/runtime/security/RBACInterceptor.java`

**功能**:
- 在方法執行前檢查權限
- 從 SecurityContext 提取使用者角色
- 支援按級別或特定角色檢查
- 自動記錄審計日誌
- 拒絕未授權存取時拋出異常

**攔截流程**:
```
請求 → @Authorized 檢查 → 取得用戶認證
  ↓
提取用戶角色 → 比較權限 → 如果充分 → 執行方法
                           ↓
                      如果不足 → 拋出 AccessDeniedException
                                ↓
                           Global Exception Handler
                                ↓
                           返回 403 Forbidden
```

### 4. 異常處理

**新檔案**:
- `runtime/src/main/java/com/aipa/runtime/security/UnauthorizedException.java`
- `runtime/src/main/java/com/aipa/runtime/security/AccessDeniedException.java`
- `runtime/src/main/java/com/aipa/runtime/api/exception/GlobalExceptionHandler.java`

**異常回應範例**:

未授權 (401):
```json
{
  "errorCode": "UNAUTHORIZED",
  "message": "未授權: 未認證使用者，存取被拒",
  "status": 401,
  "timestamp": "2026-06-30T12:34:56"
}
```

存取被拒 (403):
```json
{
  "errorCode": "FORBIDDEN",
  "message": "存取被拒: 您的角色 (檢視者) 無足夠權限存取此資源",
  "status": 403,
  "timestamp": "2026-06-30T12:34:56"
}
```

### 5. 依賴更新

**Runtime** (`build.gradle.kts`):
```kotlin
// Phase 9: 角色型存取控制
implementation("org.springframework.boot:spring-boot-starter-aop")
```

## 測試

### RBAC 單元測試
```bash
cd runtime
../gradlew test --tests "com.aipa.runtime.security.RoleBasedAccessControlTests"
```

**測試用例**:
- ✓ 角色級別定義
- ✓ 顯示名稱
- ✓ 級別檢查
- ✓ 角色比較
- ✓ 權限階層驗證
- ✓ 角色權限值

## 部署配置

### 啟用 RBAC

在 `application.yml` 中配置:
```yaml
spring:
  security:
    # RBAC 預設啟用 (via @EnableWebSecurity)
    enable-rbac: true
```

### Security 配置範例

```java
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .authorizeHttpRequests(auth -> auth
                // 公開端點
                .requestMatchers("/api/v1/health").permitAll()
                // 受保護端點
                .anyRequest().authenticated()
            )
            // Phase 9: RBAC 搭配註解型檢查
            // 具體的方法級別權限由 @Authorized 處理
            .httpBasic(Customizer.withDefaults());

        return http.build();
    }
}
```

## 審計日誌集成

### 敏感操作記錄

所有 @Authorized 保護的修改操作會自動記錄:

```json
{
  "timestamp": "2026-06-30T12:34:56.789Z",
  "level": "INFO",
  "message": "使用者 admin 執行操作: 核准檢查點",
  "service": "runtime",
  "auditType": "security",
  "severity": "security",
  "user": "admin",
  "operation": "approveCheckpoint",
  "resource": "checkpoint#12345"
}
```

### 存取拒絕記錄

所有被拒的存取嘗試會自動記錄:

```json
{
  "timestamp": "2026-06-30T12:34:56.789Z",
  "level": "WARN",
  "message": "使用者 viewer [192.168.1.100] 嘗試存取受保護資源被拒: approveCheckpoint",
  "service": "runtime",
  "auditType": "security",
  "severity": "security",
  "user": "viewer",
  "ip": "192.168.1.100",
  "deniedResource": "approveCheckpoint"
}
```

## 下一步建議

1. **使用者與角色關聯**
   - 建立 User entity + Role 關聯表
   - JWT token 中包含角色資訊

2. **動態權限配置**
   - 從資料庫加載角色權限
   - 支援角色自訂化

3. **API 層級控制**
   - 不同環境的存取策略 (開發/測試/生產)
   - 基於資源的細粒度控制 (某些用戶只能檢視特定項目)

4. **合規性報告**
   - 生成符合企業安全標準的稽核報告
   - 追蹤權限異動歷史

## 復驗清單

- [x] 五層角色定義
- [x] 角色級別體系
- [x] @Authorized 註解
- [x] AOP 攔截器
- [x] 異常類定義
- [x] 全域異常處理
- [x] 單元測試
- [x] 審計日誌集成
- [ ] 使用者-角色資料庫表
- [ ] JWT 認證集成
- [ ] 動態權限加載

## 貢獻指南

### 添加新角色

1. 在 `AIRole.java` 中添加新枚舉值:
```java
CUSTOM_ROLE("自訂角色", "ROLE_CUSTOM", 25)
```

2. 注意級別應在現有角色之間選擇

### 添加新的權限檢查

1. 在方法上使用 @Authorized 註解
2. 指定所需的最小級別或特定角色
3. 提供描述 (用於審計日誌)

### 自訂異常處理

編輯 `GlobalExceptionHandler.java` 添加新的 @ExceptionHandler

---
**作者**: AIPA Studio Team
**版本**: 1.0.0-SNAPSHOT
**最後更新**: 2026-06-30
**階段**: Phase 9 第三切片 — 角色型存取控制


