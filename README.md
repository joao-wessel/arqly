# Arqly

Fundação SaaS para gerenciamento de projetos de arquitetura.

## Stack

- Backend: Java 21, Spring Boot 3, Maven, Spring Security, JWT, JPA/Hibernate, Flyway, PostgreSQL.
- Frontend: Angular 20 standalone, Tailwind CSS, Lucide Icons, ApexCharts e FullCalendar preparados.
- Infra: Docker Compose com `frontend`, `backend` e `postgres`.

## Requisitos

- Java 21 para execuções locais do backend.
- Docker e Docker Compose para o ambiente integrado.

## Ambiente local

Crie um arquivo `.env` a partir de `.env.example` e substitua todos os valores de exemplo por segredos exclusivos. Em especial, gere duas chaves JWT independentes com pelo menos 32 caracteres e defina a origem pública em `APP_ALLOWED_ORIGINS`.

O bootstrap do primeiro administrador de plataforma é opcional e fica desativado por padrão. Em banco vazio, preencha `ARQLY_INITIAL_ADMIN_NAME`, `ARQLY_INITIAL_ADMIN_EMAIL` e `ARQLY_INITIAL_ADMIN_PASSWORD`, então defina `ARQLY_INITIAL_BOOTSTRAP_ENABLED=true` somente para a primeira subida. Ele não cria Tenant. Depois de confirmar o acesso administrativo, altere `ARQLY_INITIAL_BOOTSTRAP_ENABLED=false` e reinicie o backend. Nunca versione essa senha no `.env`.

Os Tenants continuam sendo criados exclusivamente pelo fluxo administrativo normal. Como todo `ProjectTemplate` pertence a um Tenant, cada Tenant recém-criado recebe o modelo padrão `Residencial`, com as fases e etapas residenciais já adotadas pelo sistema. Esse seed é idempotente e transacional junto à criação do Tenant.

```bash
docker compose up -d --build
```

O frontend é publicado na porta configurada em `FRONTEND_PORT`. O backend e PostgreSQL ficam vinculados apenas a `127.0.0.1`; o frontend encaminha as chamadas de `/api` internamente ao backend.

## Staging em uma VPS

O ambiente de staging usa quatro serviços: `reverse-proxy`, `frontend`, `backend` e `postgres`. Somente o proxy publica as portas `80` e `443`. Backend, PostgreSQL e frontend ficam na rede Docker interna; o banco nunca é publicado diretamente.

### Provisionamento da VPS

Para o piloto, uma VPS Linux única com 4 vCPU, 8 GB de RAM e 80–160 GB de SSD é uma referência razoável. Ubuntu Server 24.04 LTS, ou outra distribuição Linux LTS equivalente, é recomendado. Esses valores não são requisitos rígidos da aplicação; acompanhe memória, disco e crescimento de uploads durante o piloto.

Instale apenas as ferramentas operacionais necessárias. Java, Node.js, PostgreSQL e Nginx não devem ser instalados no host, pois são executados nos containers:

```bash
sudo apt update
sudo apt install -y ca-certificates curl git ufw
# Instale Docker Engine e o plugin Docker Compose conforme a documentação oficial do Docker para sua distribuição.
```

Crie um usuário operacional dedicado e permita o uso do Docker sem executar a operação cotidiana como root:

```bash
sudo adduser arqly
sudo usermod -aG docker arqly
sudo install -d -o arqly -g arqly -m 750 /opt/arqly /opt/arqly/backups
sudo -iu arqly
git clone <URL_DO_REPOSITORIO> /opt/arqly/app
cd /opt/arqly/app
cp .env.example .env
chmod 600 .env
```

Preencha `.env` exclusivamente no servidor. Não use valores de exemplo nem versione esse arquivo. As variáveis obrigatórias são `POSTGRES_DB`, `POSTGRES_USER`, `POSTGRES_PASSWORD`, `ARQLY_PLATFORM_JWT_SECRET`, `ARQLY_TENANT_JWT_SECRET`, `APP_ALLOWED_ORIGINS`, `APP_PUBLIC_WEBSITE_URL`, `APP_PUBLIC_FRONTEND_URL`, `APP_PUBLIC_BACKEND_URL`, `APP_DOMAIN`, `LETSENCRYPT_EMAIL`, `APP_TIMEZONE`, `APP_STORAGE_PATH` e `ARQLY_BACKUP_DIR`. SMTP usa, quando disponível, `SPRING_MAIL_HOST`, `SPRING_MAIL_PORT`, `SPRING_MAIL_USERNAME` e `SPRING_MAIL_PASSWORD`.

Gere segredos localmente no servidor, por exemplo:

```bash
openssl rand -base64 48
```

Use valores independentes para cada segredo JWT. Nunca cole o resultado em terminal compartilhado, issue, chat ou repositório.

Configure DNS com registros `A` e, se aplicável, `AAAA` do domínio real para o IP da VPS. Antes de emitir o certificado, confirme:

```bash
dig +short <DOMINIO_REAL>
curl -I http://<DOMINIO_REAL>
```

No firewall, mantenha somente SSH operacional, HTTP e HTTPS. Quando viável, restrinja SSH à rede administrativa:

```bash
sudo ufw allow OpenSSH
sudo ufw allow 80/tcp
sudo ufw allow 443/tcp
sudo ufw enable
sudo ufw status numbered
```

Não libere `5432` ou `8080`. Use autenticação SSH por chave, proteja a chave privada e evite senha para acesso administrativo quando a operação permitir.

### Requisitos do servidor

- Docker Engine e Docker Compose plugin.
- Um domínio apontado para o IP da VPS, por exemplo `staging.seudominio.com`.
- Firewall liberando apenas TCP `80` e `443` para o público.
- Um arquivo `.env`, criado a partir de `.env.example`, com segredos reais e exclusivos.

Defina ao menos `POSTGRES_*`, as duas chaves JWT, `APP_DOMAIN`, `LETSENCRYPT_EMAIL`, as URLs públicas HTTPS, `APP_ALLOWED_ORIGINS`, `APP_TIMEZONE` e, quando aplicável, as credenciais SMTP. Não versione o `.env`.

### Primeira inicialização e HTTPS

O primeiro comando sobe o ambiente HTTP exclusivamente para permitir o desafio ACME do Let's Encrypt:

```bash
docker compose -f docker-compose.staging.yml build
docker compose -f docker-compose.staging.yml up -d
```

Com o DNS já propagado, emita o certificado real:

```bash
docker compose -f docker-compose.staging.yml run --rm \
  --entrypoint /bin/sh certbot -c \
  'certbot certonly --webroot -w /var/www/certbot \
  --email "$LETSENCRYPT_EMAIL" --agree-tos --no-eff-email \
  -d "$APP_DOMAIN"'
```

Em seguida, habilite HTTPS e o redirecionamento obrigatório de HTTP para HTTPS:

```bash
docker compose -f docker-compose.staging.yml -f docker-compose.staging.https.yml up -d
```

O serviço `certbot` renova certificados periodicamente; o proxy recarrega sua configuração a cada doze horas para usar certificados renovados. Não exponha esse ambiente externamente usando certificado autoassinado.

### Operação

Ver logs e saúde:

```bash
docker compose -f docker-compose.staging.yml -f docker-compose.staging.https.yml ps
docker compose -f docker-compose.staging.yml -f docker-compose.staging.https.yml logs -f reverse-proxy backend
```

O endpoint interno de saúde é `http://backend:8080/actuator/health`; ele não é publicado pelo proxy. Para parar o ambiente sem remover dados:

```bash
docker compose -f docker-compose.staging.yml -f docker-compose.staging.https.yml down
```

Não use `down -v` na operação comum: os volumes `*_postgres_data` e `*_uploads` contêm os dados persistentes.

Atualização simples:

```bash
git pull
docker compose -f docker-compose.staging.yml -f docker-compose.staging.https.yml build
docker compose -f docker-compose.staging.yml -f docker-compose.staging.https.yml up -d
```

Os serviços usam `restart: unless-stopped`, portanto retornam após o reboot da VPS. O procedimento de backup e recuperação está documentado a seguir.

## Backup e recuperação

O backup do Arqly é composto obrigatoriamente por banco PostgreSQL e uploads locais. Os scripts operacionais ficam em `scripts/` e usam o mesmo `.env` do staging. Por padrão, os backups são gravados em `backups/`; na VPS, defina `ARQLY_BACKUP_DIR=/opt/arqly/backups` e mantenha esse diretório fora da raiz publicada pelo Nginx.

Cada execução gera uma pasta exclusiva, com permissões restritas:

```text
/opt/arqly/backups/2026-09-17_020000/
  database.dump
  database.dump.sha256
  uploads.tar.gz
  uploads.tar.gz.sha256
  manifest.txt
```

O banco é exportado em formato customizado do PostgreSQL (`pg_dump -Fc`), próprio para `pg_restore`. Os uploads são arquivados diretamente a partir do named volume Docker, portanto o procedimento não depende de caminhos físicos do host. Um backup parcial nunca recebe o status `COMPLETE`.

### Executar e verificar

```bash
./scripts/backup.sh
./scripts/backup-status.sh
./scripts/cleanup-backups.sh --dry-run
```

O backup exige ao menos 1 GiB livre por padrão (`ARQLY_BACKUP_MIN_FREE_KB`) e registra início, fim, duração e resultado em `backup.log`. A política do piloto preserva os últimos 7 backups diários, 4 semanais e 3 mensais. A limpeza é executada após um backup bem-sucedido; ela pode ser revisada antes com `--dry-run`.

Agendamento diário às 02:00 na VPS:

```cron
0 2 * * * cd /opt/arqly && /opt/arqly/scripts/backup.sh >> /opt/arqly/backups/cron.log 2>&1
```

Antes de atualizações que possam executar migrations, faça um backup, atualize a aplicação e valide a saúde. Ao restaurar um banco de uma versão anterior, o Flyway preserva o histórico restaurado e aplica migrations posteriores compatíveis quando o backend voltar a iniciar.

### Restore

Restore substitui integralmente o banco e os uploads atuais. Ele valida manifest, checksums e todas as `storage_key` ativas antes de iniciar os serviços novamente:

```bash
./scripts/restore.sh /opt/arqly/backups/2026-09-17_020000
```

O script exige digitar `RESTORE`. Para automação controlada, use explicitamente `--yes`. Em uma instalação limpa sem certificado ainda emitido, execute com `ARQLY_STAGING_HTTPS=false` durante a recuperação, emita/recupere o certificado e só então suba a composição HTTPS.

### Cópia externa e proteção

Backup somente na VPS não protege contra perda da própria VPS. Configure uma chave SSH restrita e use, por exemplo:

```bash
ARQLY_BACKUP_REMOTE='backup@backup.example:/srv/arqly-backups' \
  ./scripts/copy-backup.sh /opt/arqly/backups/2026-09-17_020000
```

O destino externo deve restringir permissões ao usuário de backup. Como os artefatos contêm dados pessoais, financeiros e documentos, use um canal protegido e criptografia no destino, como disco criptografado, `restic` ou `age`; não há chaves nem credenciais externas versionadas neste repositório.
