# Sistema Farmacêutico

Plataforma integrada para operação de farmácias no Brasil: catálogo ANVISA, estoque FEFO, PDV, receituário controlado, SNGPC e front-end operacional **Farmácia Clark**.

**Stack:** Java 21 · Spring Boot 3.5 · PostgreSQL 16 · RabbitMQ · React 19 (Vite)  
**Arquitetura:** DDD + Hexagonal (domain → application → infrastructure → api)

---

## Módulos Maven

| Módulo | Responsabilidade |
|--------|------------------|
| `farmacia-domain` | Entidades, enums, repositórios (portas), value objects |
| `farmacia-application` | Use cases (regras de negócio) |
| `farmacia-infrastructure` | JPA, adapters, security, RabbitMQ, schedulers, Flyway |
| `farmacia-api` | Controllers REST, DTOs, assemblers, testes |
| `farmacia-web` | Front-end React (Farmácia Clark) |

---

## Estado atual do sistema

### Concluído

- **Domain** — 8 bounded contexts (medicamento, estoque, venda, receituário, cliente, financeiro, funcionário; compra só enums)
- **Persistência** — 20 JPA entities, 16 persistence mappers, 13 repository adapters
- **Migrations Flyway** — V1 a V5 (catálogo, estoque, funcionários, receituário, PDV/vendas)
- **Segurança** — JWT RS256 (`POST /api/v1/auth/token`), roles, CORS para `localhost`
- **Seeds dev** — `DevAmbienteSeed` (usuários) + `DevOperacionalSeed` (catálogo, estoque, caixa aberto PDV-01)
- **Front-end** — Login, dashboard, catálogo, PDV/vendas (`farmacia-web`)
- **Testes** — Unit, WebMvc, IT, BDD Cucumber (`@alertas`, `@controlado`)

### API REST exposta (v1)

| Recurso | Endpoints principais |
|---------|---------------------|
| **Auth** | `POST /auth/token` |
| **Medicamentos** | CRUD paginado |
| **Vendas** | `POST`, `GET /{id}`, `GET` (filtros), `DELETE` (cancelar) |
| **PDV** | `GET /pdv/contexto` |
| **Caixa** | `GET /caixa/aberto`, `POST /caixa/abrir`, `POST /caixa/fechar` |
| **Estoque** | saldo, lotes FEFO, entrada, ajuste, alertas, movimentações |
| **Compras** | pedidos, NF-e entrada, fornecedores |
| **Receitas** | `POST`, `GET /{id}`, `GET ?numero=`, `PUT /{id}/validar` |
| **Clientes** | `POST`, `GET /{id}`, `GET /cpf/{cpf}`, `PUT /{id}` |

Documentação interativa: [http://localhost:8080/swagger-ui.html](http://localhost:8080/swagger-ui.html)

### Pendente (roadmap)

- Integração SNGPC real (ANVISA) — hoje a fila simula confirmação
- NFC-e / cupom fiscal no PDV
- Fechamento de caixa com conferência detalhada
- Dashboard com KPIs reais da API

### Deploy (homologação / loja)

Ver **[DEPLOY.md](DEPLOY.md)** — `docker-compose.prod.yml`, `.env.prod.example`, CI em `.github/workflows/ci.yml`.

---

## Pré-requisitos

- JDK 21+
- Maven 3.9+
- Docker Desktop (PostgreSQL + RabbitMQ)
- Node.js 20+ (front-end)

---

## Subir o ambiente

### 1. Infraestrutura

```bash
docker compose up -d
```

| Serviço | Host | Credenciais |
|---------|------|-------------|
| PostgreSQL | `localhost:5433` | `farmacia` / `farmacia123` / DB `farmacia_dev` |
| RabbitMQ | `localhost:5672` | `guest` / `guest` (UI: `:15672`) |

### 2. API (perfil `dev`)

O módulo `farmacia-api` depende de `farmacia-application` e `farmacia-domain`. Use **`-am`** (*also make*) para compilar esses módulos antes da API — senão o Maven pode usar JARs antigos do `.m2` e falhar com *package … does not exist*.

**Primeira vez ou após mudar domain/application:**

```bash
mvn clean install -pl farmacia-api -am -DskipTests
```

**Subir a API:**

```bash
mvn spring-boot:run -pl farmacia-api -am
```

A API sobe em **http://localhost:8080**.  
Flyway aplica V1–V5; seeds Java criam usuários, medicamentos de dev e caixa aberto.

### 3. Front-end

```bash
cd farmacia-web
npm install
npm run dev
```

Abre **http://localhost:5173** — proxy Vite encaminha `/api` → `:8080`.

---

## Credenciais de desenvolvimento

| Papel | E-mail | Senha |
|-------|--------|-------|
| Admin | `admin@farmacia.com` | `admin123` |
| Gerente | `gerente@farmacia.com` | `ger123` |
| Farmacêutico | `farmaceutico@farmacia.com` | `farm123` |
| Estoquista | `estoquista@farmacia.com` | `est123` |
| Balconista | `balconista@farmacia.com` | `bal123` |

Obter token:

```bash
curl -s -X POST http://localhost:8080/api/v1/auth/token \
  -H "Content-Type: application/json" \
  -d "{\"email\":\"admin@farmacia.com\",\"senha\":\"admin123\"}"
```

Use o header `Authorization: Bearer <token>` nas demais requisições.

---

## Testes

```bash
# Compilar tudo
mvn compile -pl farmacia-api -am

# Testes unitários + integração
mvn verify -pl farmacia-api -am

# BDD — alertas de estoque/vencimento
mvn test -pl farmacia-api -am -Dtest=CucumberRunnerIT "-Dcucumber.filter.tags=@alertas"

# BDD — venda medicamento controlado
mvn test -pl farmacia-api -am -Dtest=CucumberRunnerIT "-Dcucumber.filter.tags=@controlado"
```

---

## Fluxos operacionais

### PDV (venda)

1. Caixa aberto no PDV-01 (seed dev ou `POST /api/v1/caixa/abrir`)
2. Front em **PDV / Vendas** ou `POST /api/v1/vendas`
3. Regras automáticas: FEFO, PMC, receita/CPF para controlados, SNGPC assíncrono

### Receituário

1. `POST /api/v1/receitas` — status `PENDENTE`
2. Farmacêutico: `PUT /api/v1/receitas/{id}/validar` com itens (medicamento + quantidade)
3. Receita `APROVADA` pode ser usada na venda

### Estoque

- `GET /api/v1/estoque/medicamentos/{id}` — saldo
- `GET /api/v1/estoque/medicamentos/{id}/lotes` — ordem FEFO
- `GET /api/v1/estoque/alertas` — alertas abertos (scheduler gera vencimento/ruptura)

---

## Front-end Farmácia Clark

Design system **Clark** (cores vivas, base escura, tipografia Outfit).  
Skill Cursor do projeto: `.cursor/skills/farmacia-frontend/SKILL.md`

| Tela | Rota | Status |
|------|------|--------|
| Login | `/login` | ✅ |
| Dashboard | `/` | ✅ |
| Catálogo | `/medicamentos` | ✅ |
| PDV / Vendas | `/vendas` | ✅ |
| Estoque | `/estoque` | ✅ |
| Receituário | `/receitas` | ✅ |
| Compras | `/compras` | ✅ |
| Cadastros | `/cadastros` | ✅ |

Build produção: `cd farmacia-web && npm run build`  
Stack Docker: `docker compose -f docker-compose.prod.yml --env-file .env.prod up -d --build`  
Testes E2E (Playwright): `cd farmacia-web && npm run test:e2e` — ver `farmacia-web/e2e/README.md`

---

## Estrutura de pastas (raiz)

```
Farmacia/
├── farmacia-domain/
├── farmacia-application/
├── farmacia-infrastructure/
│   └── src/main/resources/db/migration/   # Flyway V1–V5
├── farmacia-api/
├── farmacia-web/                          # React + Vite
├── docker-compose.yml
├── .cursor/skills/farmacia-frontend/      # Skill agente front-end
└── README.md
```

---

## Roles (Spring Security)

| Role | Acesso típico |
|------|----------------|
| `BALCONISTA` | PDV, vendas, clientes, consulta estoque |
| `ESTOQUISTA` | Estoque, alertas |
| `FARMACEUTICO` | Receitas, controlados, validação |
| `GERENTE` | Relatórios, caixa, cancelamento venda |
| `ADMIN` | Tudo + configurações |

---

## Autores

Alex Silva e Claude — Sistema Farmacêutico · DDD + Hexagonal · Heurística AlgaWorks
