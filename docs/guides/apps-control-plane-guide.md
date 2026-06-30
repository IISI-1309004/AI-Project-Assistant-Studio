# Python Control Plane Skeleton

This directory contains the first cut of a Python-first control plane for AIPA Studio.

## Included now

- `api/` — FastAPI entrypoint and first migration routes
- `worker/` — lightweight worker skeleton for project initialization jobs
- SQLAlchemy-backed services under `packages/` so the scaffold keeps state in SQLite
- In-process async job runner for `project/init` so clients can poll job status

## First migrated endpoints

- `GET /api/v1/health`
- `GET /api/v1/version`
- `POST /api/v1/project/init`
- `GET /api/v1/project/init/{jobId}/status`
- `POST /api/v1/session`
- `GET /api/v1/session`
- `GET /api/v1/session/{id}`
- `GET /api/v1/session/{id}/stream`
- `GET /api/v1/checkpoint`
- `GET /api/v1/checkpoint/{id}`
- `POST /api/v1/checkpoint/{id}/approve`
- `POST /api/v1/checkpoint/{id}/reject`
- `GET|POST /api/v1/knowledge` and `GET|POST /api/v1/knowledge/search`
- `GET /api/v1/memory`, `GET /api/v1/memory/{id}`, `POST /api/v1/memory/reinforce/{id}`
- `POST /api/v1/experience/search`, `GET|POST /api/v1/experience/cases`
- `GET|POST /api/v1/wisdom/rules`, `POST /api/v1/wisdom/match`, `POST /api/v1/wisdom/check`
- `POST /api/v1/learn`, `GET /api/v1/learn/{learningId}`, `POST /api/v1/learn/{learningId}/rollback`, `POST /api/v1/learn/{learningId}/write-back`

## Quick start

```powershell
python -m apps.api
```

Then open:

- `http://127.0.0.1:18080/api/v1/health`
- `http://127.0.0.1:18080/docs`

## Notes

- This skeleton stores state in `./.ai-project/python-control-plane.db` by default.
- You can override the database target with `AIPA_DATABASE_URL`.
- The next migration step is to swap the in-process runner for a real queue worker and replace the stub scanner with plugin/sidecar adapters.

