# Fluxo completo — o que acontece quando você roda `npm run test:e2e`

## Passo a passo

```
1. Você executa: npm run test:e2e
2. Playwright lê playwright.config.ts
3. Descobre 3 projetos: setup → login + app (app depende de setup)
4. Abre o Chromium (Desktop Chrome)
5. RODA setup (auth.setup.ts)
6. RODA login.spec.ts (4 testes)
7. RODA app.spec.ts (4 testes) — cada um chama garantirSessaoAdmin no beforeEach
8. Mostra relatório: passed / failed
```

Total: **9 testes** (1 setup + 4 login + 4 app).

## Diagrama

```
┌─────────────────────────────────────────────────────────┐
│                  playwright.config.ts                      │
│  baseURL, timeout, projetos, reporters, webServer (CI)    │
└──────────────────────────┬──────────────────────────────┘
                           │
         ┌─────────────────┼─────────────────┐
         ▼                 ▼                 ▼
    ┌─────────┐     ┌───────────┐     ┌───────────┐
    │  setup  │     │   login   │     │    app    │
    │ 1 teste │     │ 4 testes  │     │ 4 testes  │
    └────┬────┘     └───────────┘     └─────┬─────┘
         │                                  │
         │         auth.setup.ts            │  beforeEach
         │              │                   │      │
         └──────────────┴───────────────────┴──────┘
                           │
                    autenticar.ts
                     /          \
              request (API)    page (browser)
```

## Projeto `setup` — o que valida?

- API responde em `POST /api/v1/auth/token`  
- Token entra no `sessionStorage`  
- Após ir para `/`, o link **Estoque** no menu aparece  

Se falhar, o projeto `app` **nem deveria** ser confiável — por isso `dependencies: ['setup']`.

## Projeto `login` — o que valida?

Testa a **experiência de login na UI**, sem usar `autenticar.ts`:

| Teste | Objetivo QA |
|-------|-------------|
| Marca e formulário | Tela carregou? Campos existem? |
| Credenciais inválidas | Sistema barra usuário errado? |
| Admin autentica | Fluxo feliz pela tela |
| Atalhos dev | Só em modo desenvolvimento Vite |

## Projeto `app` — o que valida?

Usuário **já logado** (via `garantirSessaoAdmin`):

| Teste | Objetivo QA |
|-------|-------------|
| Sidebar | Marca Farmácia + Clark + menu |
| Estoque listagem | Título, busca, botão Nova entrada |
| Nova entrada | Formulário abre com regra de validade |
| PDV | Tela “Nova venda” carrega |

## CI (GitHub Actions)

No servidor de integração:

- Sobe Postgres e RabbitMQ  
- Compila o JAR da API  
- `PLAYWRIGHT_MANAGED_SERVERS=1` → Playwright sobe API + `npm run dev`  
- Roda os 9 testes com `workers: 1` e `retries: 2`  

## Quando um teste falha, olhe nesta ordem

1. API ligada? (`http://127.0.0.1:8080/actuator/health`)  
2. Front ligado? (`http://localhost:5173`)  
3. Docker com Postgres?  
4. Screenshot em `test-results/`  
5. Relatório HTML: `npm run test:e2e:report`  
