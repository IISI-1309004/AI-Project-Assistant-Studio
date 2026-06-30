# AIPA Studio 摰???嚗nstallation Guide嚗?

**?**嚗?.0.0-SNAPSHOT
**?敺??*嚗?026-06-30
**?拍撠情**嚗頂蝯梁恣??evOps 撌亦?撣?

> 憒?雿銝?砌蝙?刻?隢?亙???**[雿輻?? (002-user-guide.md)](./002-user-guide.md)**??
> ?祆???瘜冽閰喟敦摰?甇仿??身摰牧???恣????瘚???

---

## ?桅?

1. [摰????(#1-摰?????
2. [?孵? A嚗indows ?祆?璅∪?嚗 Docker ???刻隡平?啣?嚗(#2-?孵?-a-windows-?祆?璅∪???docker-?刻隡平?啣?)
3. [?孵? B嚗冗蝢支撩?璅∪?嚗???垢嚗(#3-?孵?-b-蝷曄黎隡箸??冽芋撘???垢)
4. [?孵? C嚗ocker Compose嚗inux/macOS嚗(#4-?孵?-cdocker-composelinuxmacos)
5. [?孵? D嚗inux 隡箸??典?鋆(#5-?孵?-dlinux-隡箸??典?鋆?
6. [?孵? E嚗indows Docker 摰?嚗?賂?](#6-?孵?-ewindows-docker-摰??舫)
7. [摰?敺身摰(#7-摰?敺身摰?
8. [??蝞∠?](#8-??蝞∠?)
9. [??瘚?](#9-??瘚?)
10. [閫?摰?](#10-閫?摰?)
11. [摰?撽皜](#11-摰?撽皜)

---

## 1. 摰?????

### 1.1 蝖祇??瘙?

| 閬 | ?雿?| 撱箄降嚗??Ｙ憓? |
|------|------|----------------|
| CPU | 4 ?詨? | 8 ?詨?隞乩? |
| 閮擃?| 8 GB | 16 GB 隞乩? |
| ?脣?蝛粹? | 20 GB | 50 GB 隞乩?嚗?亥??霅澈嚗?|
| 蝬脰楝 | 10 Mbps | 100 Mbps嚗I API ?澆嚗?|

### 1.2 雿平蝟餌絞?詨捆??

| 雿平蝟餌絞 | ? | ?舀???|
|----------|------|----------|
| Ubuntu | 22.04 LTS | ??摰?舀 |
| Ubuntu | 20.04 LTS | ???舀 |
| RHEL | 8.x??.x | ??摰?舀 |
| CentOS Stream | 9 | ???舀 |
| Windows | 10 Build 19041+ | ???舀 |
| Windows | 11 | ??摰?舀 |
| macOS | 12+ | ?? 蝷曄黎?舀嚗摰皜祈岫嚗?|

### 1.3 蝬脰楝?瘙?

| ?桃? | ?格? | 隤芣? |
|------|------|------|
| AI API ?澆 | `api.anthropic.com`?api.openai.com` | 憒蝙?券蝡?AI |
| Docker ??銝? | `registry-1.docker.io` | ?活摰? |
| 憟辣銝? | `npmjs.com`?pypi.org`?repo1.maven.org` | ?活撱箇蔭 |
| ?折?? | localhost | Runtime ??AI Engine ??Web |

### 1.4 ??摰???

```bash
git clone https://github.com/your-org/AI-Project-Assistant-Studio.git
cd AI-Project-Assistant-Studio
```

---

## 2. ?孵? A嚗indows ?祆?璅∪? ???刻隡平?啣?

**?拍??Windows ??啣?**

### 2.1 ?蔭頠?嚗???鋆?

```powershell
# 1. 摰? Node.js 20 LTS
#    銝?嚗ttps://nodejs.org/en/download/
#    ?蝯∟眼?砍 IT ?摰?

# 撽?摰?
node --version  # ?＊蝷?v20.x.x ??啁???
npm --version
```

### 2.2 ??銝血?鋆?CLI

```powershell
# 撠?啣極雿??
cd C:\Users\YourUsername\work

# ??蝔?蝣?
git clone https://github.com/your-org/AI-Project-Assistant-Studio.git
cd AI-Project-Assistant-Studio

# 摰? CLI 撌亙
cd cli
npm install
npm run build
npm install -g .

# 撽?
aipa version  # ?＊蝷箇??祈?
```

### 2.3 ?蔭?祆?璅∪?

蝺刻摩 `cli\.env.local` 瑼?嚗??⊥迨瑼?? `cli` ?桅?銝遣蝡?嚗?

```ini
# .env.local ??Windows ?祆?璅∪?閮剖?
AIPA_MODE=LOCAL
SKIP_SERVER_CHECK=true
AIPA_AI_PROVIDER=OLLAMA
OLLAMA_BASE_URL=http://localhost:11434
```

### 2.4嚗?佗?摰? Ollama ?祆? AI 璅∪?

Ollama ?舐蝡??函?撘??臬?祆??Ｙ??? AI 璅∪???

```powershell
# 1. 銝? Ollama Windows ?
#    https://ollama.ai/download
#    ?湔摰?

# 2. 蝣箄? Ollama 撌脣??????瑁?嚗?
#    瑼Ｘ嚗ttp://localhost:11434 ?臬??

# 3. ?冽 PowerShell 閬?銝?璅∪?
ollama pull llama3.1:8b   # 頛?嚗?虫??祇??潘?4GB嚗?
# ??
ollama pull qwen2.5-coder:7b  # 蝔?蝣潛???雿喳?嚗?GB嚗?
```

### 2.5 撽??祆?璅∪?摰?

```powershell
# 瑼Ｘ CLI
aipa version

# 皜祈岫 Ollama ???
Invoke-WebRequest -Uri "http://localhost:11434/api/tags" -UseBasicParsing
# ???歇摰??芋??銵?
```

---

## 3. ?孵? B嚗冗蝢支撩?璅∪?嚗???垢嚗?

**?拍?潭??砍 Linux 隡箸??冽?撌脫? AIPA ?函蔡??璆?*

甇斗芋撘? Windows 銝摰? CLI 撌亙嚗???啣歇???垢 AIPA Runtime ????

### 3.1 ?蔭璇辣

- IT ?券?撌脣?砍 Linux 隡箸??其??函蔡 AIPA Runtime嚗蝙?冽撘?D ???萄?鋆?穿?
- ?砍蝬脰楝?迂 Windows ????啗府隡箸???
- 隡箸???IP ??DNS ?迂嚗?憒?`company-aipa-server` ??`10.0.1.100`嚗?

### 3.2 摰? CLI

```powershell
cd AI-Project-Assistant-Studio\cli
npm install
npm run build
npm install -g .
```

### 3.3 閮剖??垢隡箸??其??

```powershell
# 閮剖??啣?霈???砍隡箸???
[System.Environment]::SetEnvironmentVariable("AIPA_RUNTIME_URL", "http://company-aipa-server:8080", "Machine")
```

?楊頛?`cli\.env` 瑼?嚗?

```ini
AIPA_RUNTIME_URL=http://company-aipa-server:8080
AIPA_MODE=REMOTE
```

### 3.4 皜祈岫???

```powershell
aipa health
# ?＊蝷粹?蝡?Runtime ?靽⊥
```

摰?嚗indows ?冽?曉?臭誑?? CLI ????啣?貊? AIPA ????

---

## 4. ?孵? C嚗ocker Compose嚗inux/macOS ?芾??函蔡嚗?

?拙?嚗之憭 Linux ?湔?閮蝵脤?瘙?

### 4.1 ?蔭?瘙?鋆?

#### Ubuntu/Debian

```bash
# 摰? Docker
curl -fsSL https://get.docker.com | bash
sudo usermod -aG docker $USER
newgrp docker

# 蝣箄??
docker --version    # ?閬?24.0+
docker compose version  # ?閬?2.20+
```

#### RHEL/CentOS

```bash
# 摰? Docker
sudo dnf install -y yum-utils
sudo yum-config-manager --add-repo https://download.docker.com/linux/rhel/docker-ce.repo
sudo dnf install -y docker-ce docker-ce-cli containerd.io docker-compose-plugin

# ?? Docker
sudo systemctl enable --now docker
sudo usermod -aG docker $USER
```

### 4.2 閮剖??啣?霈

```bash
cd installer/docker
cp .env.example .env
```

蝺刻摩 `.env` 瑼?嚗?

```ini
# ======================================================
# AIPA Studio ??Docker Compose ?啣?閮剖?
# ======================================================

# ?箸閮剖?
COMPOSE_PROJECT_NAME=aipa-studio
AIPA_VERSION=1.0.0-SNAPSHOT

# 蝡臬閮剖?嚗?垢???蝒??冽迨靽格嚗?
RUNTIME_PORT=8080        # Runtime Service 撠?蝡臬
WEB_PORT=80              # Web Dashboard 撠?蝡臬

# ======================================================
# AI 靘??身摰??喳?閮剖?銝??
# ======================================================
CLAUDE_API_KEY=           # Anthropic Claude API Key
OPENAI_API_KEY=           # OpenAI API Key
GEMINI_API_KEY=           # Google Gemini API Key
OLLAMA_BASE_URL=          # Ollama ?砍: http://host.docker.internal:11434

# ======================================================
# 鞈?摨怨身摰?
# ======================================================
# ?身雿輻 SQLite嚗?憿?閮剖?嚗?

# ?亥?雿輻 PostgreSQL嚗?瘨誑銝釣??
# POSTGRES_USER=aipa
# POSTGRES_PASSWORD=your_secure_password_here
# POSTGRES_DB=aipa_studio

# ======================================================
# 摰閮剖?
# ======================================================
# IP ?賢??殷???啣?撱箄降?嚗?
ENABLE_IP_WHITELIST=false
IP_WHITELIST=127.0.0.1,::1

# ??鞈??桃蔗嚗迤閬”??嚗???嚗?
AIPA_CONTEXT_EXCLUDE_PATTERNS=

# ======================================================
# ?亥?閮剖?
# ======================================================
LOG_PATH=/var/log/aipa
LOG_LEVEL=INFO
```

### 4.3 ????

```bash
docker compose up -d
```

????蝝? 2?? ?????????

```bash
docker compose ps
```

??頛詨嚗?
```
NAME                    STATUS          PORTS
aipa-studio-runtime-1   Up (healthy)    0.0.0.0:8080->18080/tcp
aipa-studio-ai-engine-1 Up (healthy)
aipa-studio-web-1       Up (healthy)    0.0.0.0:80->80/tcp
aipa-studio-chromadb-1  Up (healthy)
```

### 4.4 摰? CLI 撌亙

```bash
cd cli
npm install
npm run build
sudo npm install -g .
```

撽?嚗?
```bash
which aipa   # ?＊蝷?/usr/local/bin/aipa ??隡潸楝敺?
aipa version # AIPA Studio CLI v1.0.0-SNAPSHOT
```

### 4.5 撽?摰?

```bash
aipa doctor
```

????格?憿舐內 ??????嚗郎??急?敹賜嚗?

```bash
# ?瑁??函蔡撽??單
bash installer/docker/verify-deployment.sh
```

### 4.6 Docker Compose ??隤芣?

| ???迂 | ?? | 蝡臬 | 隤芣? |
|----------|------|------|------|
| `runtime` | `aipa-runtime:1.0.0` | 8080??8080 | 後端 後端框架 銝餅???|
| `ai-engine` | `aipa-ai-engine:1.0.0` | (?折 18082) | Python FastAPI AI 撘? |
| `web` | `aipa-web:1.0.0` | 80??0 | React Web Dashboard |
| `chromadb` | `chromadb/chroma:0.5.0` | (?折 18083) | ??鞈?摨?|
| `postgres` | `postgres:15-alpine` | (?折 5432) | ?撘??澈嚗?賂? |

---

## 5. ?孵? D嚗inux 隡箸??典?鋆?

?拙?嚗??Ｖ撩???閬?systemd ??蝞∠???帘摰?銵?

### 5.1 敹恍?鋆?蝺?嚗?

```bash
# ?閬?root ??sudo 甈?
curl -sSL https://raw.githubusercontent.com/your-org/AI-Project-Assistant-Studio/main/installer/linux/install.sh | bash
```

### 5.2 ?祆?摰?嚗蝺?撖拇敺銵?

```bash
# 擐?撖拇?單?批捆
cat installer/linux/install.sh

# ?瑁?摰?
chmod +x installer/linux/install.sh
sudo ./installer/linux/install.sh
```

### 5.3 摰??單?瑁???

?單?瑁?隞乩???嚗?芸?嚗? 10??5 ??嚗?

```
[1/8] 蝟餌絞?詨捆?扳炎??
      ??雿平蝟餌絞嚗buntu 22.04.3 LTS
      ??CPU ?詨?嚗?
      ??閮擃?16 GB

[2/8] 摰?蝟餌絞?訾?憟辣
      ??curl, git, jq

[3/8] 摰? Docker Engine
      ??Docker 24.0.7
      ??Docker Compose 2.21.0

[4/8] ?? AIPA Studio
      摰?頝臬?嚗?opt/aipa-studio/

[5/8] 閮剖??啣?霈
      隢撓??AI API Key嚗???Enter 頝喲?嚗?

[6/8] 撱箇蔭 Docker ??
      ??aipa-runtime:1.0.0
      ??aipa-ai-engine:1.0.0
      ??aipa-web:1.0.0

[7/8] ????
      ????捆?典歇??

[8/8] 摰? CLI 撌亙
      ??aipa 撌脣?鋆 /usr/local/bin/

摰?摰?嚗銵?aipa doctor 撽??啣???
```

### 5.4 摰?敺??桅?蝯?

```
/opt/aipa-studio/          # 摰??桅?
??? installer/docker/
??  ??? docker-compose.yml
??  ??? .env
??? cli/
??? ...

/etc/systemd/system/
??? aipa-studio.service    # systemd ??摰儔

/var/log/aipa/             # ?亥??桅?
??? aipa-runtime-json.log
??? aipa-ai-engine-json.log
```

### 5.5 閮剖? AI API Key嚗?鋆?嚗?

```bash
# 蝺刻摩?啣?閮剖?
sudo nano /opt/aipa-studio/installer/docker/.env

# 靽格摰?????
sudo systemctl restart aipa-studio
```

### 5.6 閮剖??脩??

```bash
# Ubuntu嚗fw嚗?
sudo ufw allow 8080/tcp comment "AIPA Runtime"
sudo ufw allow 80/tcp comment "AIPA Web"

# RHEL/CentOS嚗irewalld嚗?
sudo firewall-cmd --permanent --add-port=8080/tcp
sudo firewall-cmd --permanent --add-port=80/tcp
sudo firewall-cmd --reload
```

---

## 6. ?孵? E嚗indows Docker 摰?嚗?賂?

?拙?嚗indows 10/11 ?犖??蝙?具?

### 6.1 ?蔭?瘙?

1. **蝣箄? Windows ?**嚗?
   - ???銵?Win+R嚗? 頛詨 `winver`
   - ?閬?Build 19041 ?擃???

2. **蝣箄?撌脣???WSL 2**嚗ocker Desktop ?閬?嚗?
```powershell
# 隞亦恣?頨思遢?瑁? PowerShell
wsl --install
wsl --set-default-version 2
```

### 6.2 ?瑁?摰??單

1. 隞?*蝞∠??∟澈隞?*?? PowerShell

2. 閮剖??瑁???嚗?閮梯?砍銵?嚗?
```powershell
Set-ExecutionPolicy Bypass -Scope Process -Force
```

3. ?瑁?摰??單嚗?
```powershell
cd AI-Project-Assistant-Studio
.\installer\windows\install.ps1
```

### 6.3 摰???

?單???

1. 瑼Ｘ Windows ???WSL 2 ???
2. 銝?銝血?鋆?Docker Desktop嚗?撠摰?嚗?
3. 蝑? Docker Desktop ??嚗? 2?? ??嚗?
4. 閮剖??啣?霈
5. ???????
6. 摰? CLI 撌亙

> ?? Docker Desktop 摰?摰?敺?賡?閬?*???**??

### 6.4 閮剖? AI API Key嚗?鋆?嚗?

```powershell
# 閮剖?蝟餌絞?啣?霈嚗恣? PowerShell嚗?
[System.Environment]::SetEnvironmentVariable("CLAUDE_API_KEY", "sk-ant-xxxxx", "Machine")
[System.Environment]::SetEnvironmentVariable("OPENAI_API_KEY", "sk-xxxxx", "Machine")

# ??? Docker ??隞亙???
cd C:\Program Files\aipa-studio\installer\docker
docker compose down
docker compose up -d
```

### 6.5 ???芸???

摰??單?身閮剖? Docker Compose ???箄????
憒???蝞∠?嚗?

```powershell
# ?亦???隞餃?
Get-ScheduledTask | Where-Object {$_.TaskName -like "*aipa*"}

# ????
cd "C:\Program Files\aipa-studio\installer\docker"
docker compose up -d
```

---

## 7. 摰?敺身摰?

### 7.1 閮剖? AI 靘???

?喳?閮剖?銝??AI 靘????賭蝙?函?撘Ⅳ?????

#### GitHub Copilot嚗?????砍撌脰頃鞎瘀?

GitHub Copilot ?臬?詨歇鞈潸眺?獢??刻?芸?雿輻??

```bash
# 1. ?? GitHub Personal Access Token
#    ??嚗ttps://github.com/settings/tokens
#    撱箇???Token嚗??copilot scope嚗?
#    銴ˊ Token

# 2. 閮剖??啣?霈
export GITHUB_TOKEN=ghp_xxxxxxxxxxxxx

# 3. 撽????
curl -H "Authorization: token $GITHUB_TOKEN" https://api.github.com/user
```

#### Anthropic Claude嚗?鞎餃??賂?

```bash
# ?? API Key嚗ttps://console.anthropic.com/
export CLAUDE_API_KEY=sk-ant-xxxxx
```

#### OpenAI嚗?鞎餃??賂?

```bash
# ?? API Key嚗ttps://platform.openai.com/api-keys
export OPENAI_API_KEY=sk-xxxxx
```

#### Google Gemini嚗?鞎餃??賂??怠?鞎駁?憿?

```bash
# ?? API Key嚗ttps://makersuite.google.com/app/apikey
export GEMINI_API_KEY=AIxxxxx
```

#### GitHub Copilot嚗??????GitHub 撣單?游?嚗?

```bash
# ?? GitHub Token嚗ttps://github.com/settings/tokens
# ?閬?repo ??gist 甈?
export GITHUB_TOKEN=ghp_xxxxxxxxxxxxxxxxxxxxxxxxxxxxx
```

### 7.2 閮剖??亥??桅?嚗inux ??啣?嚗?

```bash
# 撱箇??亥??桅?
sudo mkdir -p /var/log/aipa
sudo chown $USER:$USER /var/log/aipa

# ??.env 銝剛身摰?
echo "LOG_PATH=/var/log/aipa" >> installer/docker/.env
```

### 7.3 閮剖? IP ?賢??殷???啣?敹?嚗?

```bash
# ??.env 銝剛身摰?
echo "ENABLE_IP_WHITELIST=true" >> installer/docker/.env
echo "IP_WHITELIST=127.0.0.1,::1,10.0.0.0/8" >> installer/docker/.env

# ????憟閮剖?
docker compose restart runtime
```

### 7.4 閮剖? HTTPS嚗ginx ??隞??嚗?

憒? HTTPS嚗 Nginx 閮剖?銝剜溶??SSL ??嚗?

```nginx
# /etc/nginx/conf.d/aipa.conf
server {
    listen 443 ssl;
    server_name aipa.your-company.com;

    ssl_certificate /etc/ssl/aipa/fullchain.pem;
    ssl_certificate_key /etc/ssl/aipa/privkey.pem;

    location / {
        proxy_pass http://localhost:80;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
    }

    location /api/ {
        proxy_pass http://localhost:8080/;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
    }
}
```

### 7.5 CLI 敹急隤???project_id ?芸??菜葫嚗遣霅啣??剁?

摰?摰?敺?CLI ?舀?芰隤?憸冽敹急隤?嚗lash wrapper嚗?靘?嚗?

```powershell
/spec ?啣?隞狡???
/plan
/test
/learn
```

`project_id` ?芸??菜葫??嚗?

1. 憿臬? `--project-id`
2. ?啣?霈 `AIPA_PROJECT_ID`
3. ?桀??桅? `.ai-project/project.json`
4. ?桀??桅??迂嚗迤閬?嚗?

> 閰喟敦雿輻?孵?隢???`docs/guides/002-user-guide.md` ??.10 ?芰隤?憸冽敹急?誘??蝭??

---

## 8. ??蝞∠?

### 8.1 Docker Compose ?啣?

```bash
# ?亦??????
docker compose ps
docker compose top

# ?亦??亥?
docker compose logs -f              # ?????
docker compose logs -f runtime      # ?芰? Runtime
docker compose logs -f ai-engine    # ?芰? AI Engine

# ????
docker compose restart              # ?????
docker compose restart runtime      # ???孵???

# ?迫/??
docker compose stop
docker compose start

# 摰?迫銝衣宏?文捆??
docker compose down
# ??
docker compose up -d
```

### 8.2 Linux systemd ?啣?

```bash
# ?亦????
systemctl status aipa-studio

# ??/?迫/??
sudo systemctl start aipa-studio
sudo systemctl stop aipa-studio
sudo systemctl restart aipa-studio

# ?亦??單??亥?
journalctl -u aipa-studio -f

# 閮剖????芸???
sudo systemctl enable aipa-studio

# ?????芸???
sudo systemctl disable aipa-studio
```

### 8.3 ???亙熒???

```bash
# Runtime Service
curl http://localhost:8080/api/v1/health
# ??嚗"status":"UP","version":"1.0.0-SNAPSHOT"}

# AI Engine
curl http://localhost:18082/engine/health
# ??嚗"status":"UP","engines":{...}}

# Web Dashboard
curl -I http://localhost
# ??嚗TTP/1.1 200 OK
```

---

## 9. ??瘚?

### 9.1 Docker Compose ??

```bash
# ?脣摰??桅?
cd /opt/aipa-studio

# ????啁?撘Ⅳ
git fetch origin
git pull origin main

# ?撱箇蔭??
docker compose build --no-cache

# 皛曉???嚗?撠?????嚗?
docker compose up -d --force-recreate

# 撽???
aipa version
aipa health
```

### 9.2 CLI ??

```bash
cd cli
git pull
npm install
npm run build
sudo npm install -g . --force

# 撽?
aipa version
```

### 9.3 鞈?摨恍蝘?

??敺???Schema 霈嚗lyway ??銵蝘鳴?

```bash
# ?亦??瑞宏甇瑕
docker compose exec runtime java -jar aipa-runtime.jar --spring.flyway.out-of-order=true

# 憒蝘餃仃???亦??航炊?亥?
docker compose logs runtime | grep "Flyway"
```

---

## 10. 閫?摰?

### 10.1 Docker Compose 閫?摰?

```bash
# ?迫銝衣宏?斗??捆??
cd installer/docker
docker compose down -v  # -v ??蝘駁 volume

# 蝘駁 Docker ??
docker rmi aipa-runtime:1.0.0 aipa-ai-engine:1.0.0 aipa-web:1.0.0

# 蝘駁 CLI 撌亙
sudo npm uninstall -g aipa

# 蝘駁蝔?蝣潛???舫嚗?
cd ~
rm -rf /opt/aipa-studio
```

### 10.2 Linux systemd 閫?摰?

```bash
# ?迫銝衣??冽???
sudo systemctl stop aipa-studio
sudo systemctl disable aipa-studio

# 蝘駁 systemd ??瑼?
sudo rm /etc/systemd/system/aipa-studio.service
sudo systemctl daemon-reload

# 敺???Docker 閫?摰?
```

### 10.3 鞈??遢嚗圾?文?嚗?

```bash
# ?遢?亥?摨怨??澈
cp .ai-project/knowledge/db/aipa.db ~/backup/aipa-backup-$(date +%Y%m%d).db

# ?遢?箸閬?
cp -r templates/wisdom ~/backup/wisdom-rules-$(date +%Y%m%d)

# ?遢 Docker Volumes
docker run --rm \
  -v aipa_data:/data \
  -v ~/backup:/backup \
  alpine tar czf /backup/aipa-data-$(date +%Y%m%d).tar.gz /data
```

---

## 11. 摰?撽皜

摰?摰?敺?隢?蝣箄?隞乩??嚗?

### ?箸?

- [ ] `aipa version` 憿舐內甇?Ⅱ?
- [ ] `aipa health` 憿舐內 Runtime Service UP
- [ ] `aipa doctor` ??????????亙?????嚗?
- [ ] ?汗?刻?? `http://localhost`嚗eb Dashboard嚗?

### AI 靘???

- [ ] ?喳?銝??AI 靘???API Key 撌脰身摰?
- [ ] `aipa doctor` ??`ai-provider` ?憿舐內 ??

### ?詨??撽?

```bash
# 皜祈岫?亥?摨恬??⊿??祕撠?嚗?
aipa knowledge search "test"

# 皜祈岫 slash wrapper
/plan

# ?撓?綽?No knowledge found.嚗誨銵冽??迤撣賂??芣?亥?摨怎蝛綽?
```

- [ ] 隞乩?皜祈岫?誘?瑁??⊿隤?

### 摰閮剖?

- [ ] IP ?賢??桀歇?寞??啣??瘙身摰???啣?敹??嚗?
- [ ] ??鞈??桃蔗閬?撌脰身摰?`AIPA_CONTEXT_EXCLUDE_PATTERNS`嚗?
- [ ] ?亥??桅?摮銝撖怠

### ???皜祈岫

- [ ] CLI ??Runtime ???甇?虜嚗aipa health`嚗?
- [ ] Runtime ??AI Engine ???甇?虜嚗??health ??銝剔? engines ???
- [ ] ?汗????Web Dashboard 甇?虜

---

## ??嚗????閫?

### 摰孵??憭望?

```bash
# ?亦?閰喟敦?航炊
docker compose logs runtime --tail 50

# 撣貉???嚗?
# 1. 蝡臬銵? ??靽格 .env 銝剔? RUNTIME_PORT
# 2. 閮擃?頞???憓? Docker 閮擃??塚?Docker Desktop ??閮剖? ??鞈?嚗?
# 3. 甈??? ??蝣箄? docker 蝢斤?閮剖?
```

### CLI 摰?憭望?

```bash
# 蝣箄? Node.js ?
node --version  # ?閬?v20+

# 皜 npm 敹怠?敺?閰?
npm cache clean --force
cd cli && npm install && npm run build && sudo npm install -g .
```

### Windows Docker ?⊥???

1. 蝣箄? BIOS 撌脣??刻??砍?嚗irtualization嚗?
2. 蝣箄? WSL 2 撌脣?鋆?`wsl --status`
3. ?摰? WSL 2嚗wsl --install --no-distribution`
4. ???餉敺?閰?

---

*AIPA Studio 摰??? v1.0.0-SNAPSHOT*
*憒???隢蝯?AIPA Studio ??*



