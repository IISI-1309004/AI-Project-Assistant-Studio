# Installer Packaging Guide

This repository supports two installation modes:

1. **Release bundle install (recommended)**: install scripts download a prebuilt archive and extract it.
2. **Git clone fallback**: if bundle download fails, scripts clone/update the repository.

## Why bundle install

Bundle install avoids requiring end users to download full git history. It is better for production-like OS installation workflows.

## Build release bundles

Run from repo root.

### Windows ZIP

```powershell
$version = "v1.0.0"
$outDir = "dist"
New-Item -ItemType Directory -Force -Path $outDir | Out-Null
$zipPath = Join-Path $outDir "aipa-studio-$version-windows.zip"

Compress-Archive -Path `
  "apps","aipa_ai_engine","knowledge","memory","learning","experience","wisdom","packages","cli","web","scripts","installer","templates","pyproject.toml","package.json","README.md" `
  -DestinationPath $zipPath -Force
```

### Linux TAR.GZ

```bash
version="v1.0.0"
mkdir -p dist

tar -czf "dist/aipa-studio-${version}-linux.tar.gz" \
  apps aipa_ai_engine knowledge memory learning experience wisdom \
  packages cli web scripts installer templates \
  pyproject.toml package.json README.md
```

## Publish artifacts

Upload bundles to a release page (GitHub release example):

- `aipa-studio-<version>-windows.zip`
- `aipa-studio-<version>-linux.tar.gz`

## Use installer scripts with bundle mode

### Windows

```powershell
cd installer/windows
.\install.ps1 -Version v1.0.0
```

Or specify an explicit bundle URL:

```powershell
$env:AIPA_BUNDLE_URL = "https://example.com/aipa-studio-v1.0.0-windows.zip"
.\install.ps1 -Version v1.0.0
```

### Linux

```bash
cd installer/linux
AIPA_VERSION=v1.0.0 bash install.sh
```

Or with explicit URL:

```bash
AIPA_BUNDLE_URL="https://example.com/aipa-studio-v1.0.0-linux.tar.gz" bash install.sh
```

## Notes

- Installers still fall back to git clone when bundle download fails.
- Bundle contents must include `installer/docker/docker-compose.yml`.
- Docker remains the runtime dependency for one-command deployment.

