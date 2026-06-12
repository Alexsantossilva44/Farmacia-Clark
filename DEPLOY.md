# Deploy — Farmácia Clark

Guia objetivo para homologação ou primeira loja. Pensado para quem estuda **QA**: cada passo tem critério de aceite verificável.

## Pré-requisitos

- Docker e Docker Compose
- Arquivo `.env.prod` (copie de `.env.prod.example`)

## 1. Configurar variáveis

```bash
cp .env.prod.example .env.prod
```

| Variável | Obrigatório | Critério QA |
|----------|-------------|-------------|
| `POSTGRES_PASSWORD` | Sim | Senha forte, não commitada |
| `JWT_KEYS_DIR` | Sim | Pasta com `jwt-private.pem` e `jwt-public.pem` (RSA 2048+, ver abaixo) |
| `RABBIT_PASSWORD` | Sim | Diferente de `guest` em prod |
| `CORS_ORIGINS` | Sim | URL exata do front (ex.: `http://192.168.1.10`) |
| `APP_BOOTSTRAP_ADMIN_*` | 1º deploy | Só enquanto o banco está vazio |

### Par RSA para JWT (RS256)

```bash
mkdir -p secrets/jwt
openssl genpkey -algorithm RSA -pkeyopt rsa_keygen_bits:2048 -out secrets/jwt/jwt-private.pem
openssl pkey -in secrets/jwt/jwt-private.pem -pubout -out secrets/jwt/jwt-public.pem
```

A API assina tokens com a **chave privada**; a validação usa só a **pública** — serviços downstream não precisam do segredo de assinatura.

## 2. Subir a stack

```bash
docker compose -f docker-compose.prod.yml --env-file .env.prod up -d --build
```

**Aceite:** `docker compose ps` mostra `api` e `web` healthy; `curl http://localhost:8080/actuator/health` retorna `UP`.

## 3. Primeiro login

1. Abra `http://localhost` (ou `WEB_PORT` configurado).
2. Entre com o e-mail/senha de `APP_BOOTSTRAP_ADMIN_*` (se configurado).
3. **Aceite:** login redireciona ao painel; token em `sessionStorage` (aba do navegador).

Remova ou esvazie `APP_BOOTSTRAP_ADMIN_*` após criar usuários definitivos.

## 4. Checklist QA pós-deploy

- [ ] Swagger **não** abre em `/swagger-ui.html` (prod).
- [ ] Tela de login **sem** botões “Contas de desenvolvimento”.
- [ ] PDV: venda com estoque; mensagem clara se sem saldo.
- [ ] Estoque: entrada com validade ≥ hoje.
- [ ] Flyway: logs da API sem erro de migration.

## 5. Build local (sem Docker)

**API**

```bash
mvn clean package -pl farmacia-api -am -DskipTests
java -jar farmacia-api/target/farmacia-api-*.jar --spring.profiles.active=prod
```

**Front**

```bash
cd farmacia-web
npm ci
npm run build
# Sirva dist/ com Nginx usando nginx.conf (proxy /api)
```

## 6. CI (GitHub Actions)

O workflow `.github/workflows/ci.yml` executa:

- `mvn test -pl farmacia-api -am`
- `npm ci && npm run build` em `farmacia-web`
- **Playwright E2E** — sobe Postgres/RabbitMQ, API `dev` e front; relatório HTML em caso de falha

## 6.1 Testes E2E locais (Playwright)

Ver `farmacia-web/e2e/README.md`. Resumo:

```bash
docker compose up -d
mvn spring-boot:run -pl farmacia-api -am
cd farmacia-web && npm run dev
# outro terminal:
npm run test:e2e
```

## 7. Limitações conhecidas (não bloqueiam homologação)

- **SNGPC:** fila RabbitMQ simula confirmação; integração ANVISA real ainda pendente.
- **Fiscal:** NFC-e/SAT não implementado.
- **IT/BDD:** exigem PostgreSQL de teste; CI roda testes unitários.

## 8. Rollback

```bash
docker compose -f docker-compose.prod.yml --env-file .env.prod down
# Restaurar volume farmacia_prod_pg a partir de backup se necessário
```

---

Desenvolvimento local continua em `README.md` (`docker compose` + perfil `dev`).
