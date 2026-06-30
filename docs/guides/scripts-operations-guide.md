# Docs Utilities

## TOC / Heading Checker

Validate that Markdown TOC anchors in key docs exist in actual headings.

Checks:

- `docs/guides/002-user-guide.md`
- `docs/guides/001-installation-guide.md`

Run from repository root:

```bash
npm run docs:check
```

Expected output:

- `status: OK` when all TOC links are valid
- `status: FAIL` plus line numbers for broken anchors

## Start/Stop Unified Service

From repository root:

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\start-all.ps1
```

Stop service (best effort):

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\stop-all.ps1
```

Notes:

- `start-all.ps1` checks both unified health endpoints and starts one service on `:18080`.
- Control Plane health: `http://localhost:18080/api/v1/health`
- Engine health: `http://localhost:18080/engine/health`
- `stop-all.ps1` also cleans up legacy listeners on `:18082` if they still exist.
