# 安裝 AIPA AI Engine 依賴
Write-Host "正在安裝 Python 依賴..." -ForegroundColor Green

$deps = @(
    "fastapi==0.111.0",
    "uvicorn[standard]==0.29.0",
    "pydantic==2.7.0",
    "pydantic-settings==2.2.1",
    "sqlalchemy==2.0.29",
    "chromadb==0.5.0",
    "sentence-transformers==3.0.0",
    "gitpython==3.1.43",
    "python-json-logger==2.0.7"
)

foreach ($dep in $deps) {
    Write-Host "安裝 $dep..." -ForegroundColor Cyan
    pip install "$dep" 2>&1 | Select-Object -Last 3
}

Write-Host "✅ 依賴安裝完成" -ForegroundColor Green

