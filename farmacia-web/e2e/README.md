# Testes E2E (Playwright) — Farmácia Clark

Testes de ponta a ponta no navegador: login, navegação e fluxos críticos da interface.

## Pré-requisitos

1. **API** em perfil `dev` (`http://localhost:8080`) — seeds com `admin@farmacia.com` / `admin123`
2. **Front** em `http://localhost:5173` (`npm run dev`)
3. **Infra:** `docker compose up -d` (Postgres na porta **5433**, RabbitMQ **5672**)

## Executar localmente

Com API e front já rodando:

```bash
cd farmacia-web
npm ci
npx playwright install chromium
npm run test:e2e
```

Modo **headed** (vê o navegador):

```bash
npm run test:e2e:headed
```

Modo **debug** (Playwright Inspector — passo a passo):

```bash
npm run test:e2e:debug
```

Headed + debug juntos:

```bash
npm run test:e2e:headed-debug
```

Modo UI (painel visual):

```bash
npm run test:e2e:ui
```

CI local (sobe API + front automaticamente — exige JAR compilado):

```bash
cd ..
mvn package -pl farmacia-api -am -DskipTests
cd farmacia-web
npm run test:e2e:ci
```

## Estrutura (QA)

| Arquivo | Objetivo |
|---------|----------|
| `login.spec.ts` | Marca, formulário, erro de login, sucesso admin |
| `app.spec.ts` | Navegação autenticada desktop (estoque, entrada, PDV) |
| `clientes-cadastro.spec.ts` | Regressão das validações corrigidas no cadastro de clientes |
| `helpers/clientes-cadastro.ts` | API + navegação + CPF válido para testes de clientes |
| `app.mobile.spec.ts` | Drawer mobile (iPhone 13): menu, estoque, PDV |
| `auth.setup.ts` | Pré-condição: API responde e login admin funciona |
| `helpers/autenticar.ts` | Token via API + `sessionStorage` (estável no Chromium) |
| `helpers/mobile.ts` | `abrirMenuMobile`, `navegarMenuMobile`, viewport 390×844 |
| `helpers/credenciais.ts` | Dados de teste alinhados ao `DevAmbienteSeed` |

Total: **26 testes** (1 setup + 4 login + 4 app desktop + 11 clientes + 1 fluxo válido + 5 app mobile).

Rodar só cadastro de clientes:

```bash
npx playwright test clientes-cadastro
```

Rodar só mobile:

```bash
npx playwright test --project=app-mobile
npx playwright test --project=app-mobile --headed
```

## Seletores

Preferência: `data-testid` no login; menu lateral via `linkMenu()` em `helpers/navegacao.ts` (evita ambiguidade com cards do painel). Mobile: `getByRole('button', { name: 'Abrir menu' })` e `complementary` com `name: 'Menu principal'`.

## Relatório

Após falha: `npm run test:e2e:report` abre o HTML em `playwright-report/`.
