# Exemplos de terminal — antes e depois de corrigir

Simulações **fieis ao que você vê no PowerShell** ao rodar `npm run test:e2e` no Farmácia Clark.  
Use para treinar o olho: **qual linha importa** e **qual é a primeira causa**.

---

## Como ler estes exemplos

| Símbolo | Significado |
|---------|-------------|
| ❌ **ANTES** | Terminal quando algo está errado |
| ✅ **DEPOIS** | Terminal quando o ambiente está correto |
| `→` | Ação que você deve tomar |

Relacionado: [04-duvidas-comuns-erros.md](./04-duvidas-comuns-erros.md)

---

## 1. API desligada (ECONNREFUSED :8080)

### ❌ ANTES — API não está rodando

```text
PS C:\Java\Farmacia\farmacia-web> npm run test:e2e

> farmacia-web@0.0.0 test:e2e
> playwright test

Running 9 tests using 4 workers

  ✘  1 [setup] › e2e\auth.setup.ts:5:1 › pré-condição: API e sessão admin (312ms)
  ✘  2 [login] › e2e\login.spec.ts:27:3 › admin dev autentica e abre o painel (1.2s)

  1) [setup] › e2e\auth.setup.ts:5:1 › pré-condição: API e sessão admin

    Error: apiRequestContext.post: connect ECONNREFUSED 127.0.0.1:8080

       at helpers\autenticar.ts:9

       7 | /** Garante JWT no sessionStorage (API + injeção — estável para E2E). */
       8 | export async function garantirSessaoAdmin(page: Page, request: APIRequestContext) {
    >  9 |   const res = await request.post(`${apiBase}/api/v1/auth/token`, {
         |                             ^
      10 |     data: { email: ADMIN.email, senha: ADMIN.senha },
      11 |   })
      12 |   expect(res.ok(), `API auth falhou: ${res.status()}`).toBeTruthy()

  2) [login] › login.spec.ts:27:3 › admin dev autentica e abre o painel

    Error: expect(page).toHaveURL(expected)

    Expected: "http://localhost:5173/"
    Received: "http://localhost:5173/login"

  2 failed
  7 did not run
```

**Linha que importa:** `ECONNREFUSED 127.0.0.1:8080` na linha 9 de `autenticar.ts`.  
**Efeito dominó:** `setup` falha → `app` nem roda (`7 did not run`). Login com sucesso também falha porque a API não autentica.

→ Subir Docker + API:

```bash
docker compose up -d
mvn spring-boot:run -pl farmacia-api -am
```

---

### ✅ DEPOIS — API no ar

```text
PS C:\Java\Farmacia\farmacia-web> npm run test:e2e

Running 9 tests using 4 workers

  ✓  1 [setup] › e2e\auth.setup.ts:5:1 › pré-condição: API e sessão admin (2.1s)
  ✓  2 [login] › e2e\login.spec.ts:9:3 › exibe marca e formulário de acesso (890ms)
  ✓  3 [login] › e2e\login.spec.ts:18:3 › rejeita credenciais inválidas (1.4s)
  ✓  4 [login] › e2e\login.spec.ts:27:3 › admin dev autentica e abre o painel (1.8s)
  ✓  5 [login] › e2e\login.spec.ts:36:3 › com Vite em modo dev exibe atalhos... (412ms)
  ✓  6 [app] › e2e\app.spec.ts:10:3 › sidebar exibe marca Farmácia Clark (1.1s)
  ✓  7 [app] › e2e\app.spec.ts:17:3 › navega para estoque e exibe listagem (956ms)
  ✓  8 [app] › e2e\app.spec.ts:24:3 › abre formulário de nova entrada... (1.2s)
  ✓  9 [app] › e2e\app.spec.ts:31:3 › navega para PDV (743ms)

  9 passed (12.4s)
```

---

## 2. Front desligado (Vite :5173)

### ❌ ANTES — só API rodando, sem `npm run dev`

```text
  ✘  1 [setup] › e2e\auth.setup.ts:5:1 › pré-condição: API e sessão admin (8.2s)

    Error: page.goto: net::ERR_CONNECTION_REFUSED at http://localhost:5173/login
    Call log:
      - navigating to "http://localhost:5173/login", waiting until "load"

       at helpers\autenticar.ts:16

      14 |   const body = (await res.json()) as { token: string; expiraEmSegundos: number }
      15 |
    > 16 |   await page.goto('/login')
         |              ^
      17 |   await page.evaluate(
```

**Linha que importa:** `ERR_CONNECTION_REFUSED` em `page.goto('/login')`.  
**Detalhe:** o POST na API **pode ter funcionado** (linha 9 passou); o erro vem **depois**, ao abrir o front.

→ Em outro terminal:

```bash
cd farmacia-web
npm run dev
```

---

### ✅ DEPOIS

```text
  VITE v6.x.x  ready in 420 ms

  ➜  Local:   http://localhost:5173/
  ➜  press h + enter to show help
```

Com Vite assim, o `page.goto('/login')` completa e o setup segue.

---

## 3. Credenciais erradas ou perfil sem seed (401)

### ❌ ANTES — senha trocada em `credenciais.ts` por engano

```text
  ✘  1 [setup] › e2e\auth.setup.ts:5:1 › pré-condição: API e sessão admin (156ms)

    Error: API auth falhou: 401

    expect(received).toBeTruthy()

    Received: false

       at helpers\autenticar.ts:12

      10 |     data: { email: ADMIN.email, senha: ADMIN.senha },
      11 |   })
    > 12 |   expect(res.ok(), `API auth falhou: ${res.status()}`).toBeTruthy()
         |                                                        ^
```

**Linha que importa:** `API auth falhou: 401` — a API **respondeu**, mas rejeitou login.  
**Não é** ECONNREFUSED.

→ Conferir `e2e/helpers/credenciais.ts` = `admin@farmacia.com` / `admin123` e API em perfil **dev**.

---

### ✅ DEPOIS — curl confirma antes dos testes

```text
PS C:\Java\Farmacia> curl.exe -s -o NUL -w "%{http_code}" ^
  -X POST http://127.0.0.1:8080/api/v1/auth/token ^
  -H "Content-Type: application/json" ^
  -d "{\"email\":\"admin@farmacia.com\",\"senha\":\"admin123\"}"

200
```

`200` → credenciais OK → testes de auth devem passar.

---

## 4. Timeout — menu Estoque não aparece (sessão inválida)

### ❌ ANTES — token não injetado / usuário na tela de login

```text
  ✘  1 [app] › e2e\app.spec.ts:10:3 › sidebar exibe marca Farmácia Clark (16.8s)

    Error: expect(locator).toBeVisible()

    Locator: getByRole('complementary').getByRole('link', { name: 'Estoque', exact: true })
    Expected: visible
    Timeout: 15000ms
    Error: element(s) not found

       at helpers\autenticar.ts:34

      32 |
      33 |   await page.goto('/')
    > 34 |   await expect(linkMenu(page, 'Estoque')).toBeVisible({ timeout: 15_000 })
         |                                           ^
```

**Screenshot no relatório mostraria:** tela de **login**, não o painel.  
**Tradução:** o teste esperava menu lateral logado; a aplicação mandou para `/login`.

→ Rodar headed e ver onde para:

```bash
npm run test:e2e:headed -g "sidebar exibe marca"
```

---

### ✅ DEPOIS — mesma linha, teste passa em ~1s

```text
  ✓  6 [app] › e2e\app.spec.ts:10:3 › sidebar exibe marca Farmácia Clark (1.1s)
```

Quando passa rápido, o `garantirSessaoAdmin` funcionou de primeira.

---

## 5. Strict mode violation (dois links “Estoque”)

### ❌ ANTES — seletor amplo (erro clássico de quem está aprendendo)

```text
  ✘  1 [app] › e2e\app.spec.ts:10:3 › sidebar exibe marca Farmácia Clark (412ms)

    Error: strict mode violation: getByRole('link', { name: 'Estoque' }) resolved to 2 elements:
        1) <a href="/estoque" ...>Estoque</a> aka getByRole('complementary').getByRole('link', { name: 'Estoque' })
        2) <a href="/estoque" ...>Estoque FEFO</a> aka getByRole('link', { name: 'Estoque FEFO' })

    Call log:
      - waiting for getByRole('link', { name: 'Estoque' })
```

**Linha que importa:** `resolved to 2 elements` — Playwright achou **dois** links.  
**Correção no projeto:** `linkMenu(page, 'Estoque')` restringe ao `<aside>`.

---

### ✅ DEPOIS — com `linkMenu()`

```text
  ✓  6 [app] › e2e\app.spec.ts:10:3 › sidebar exibe marca Farmácia Clark (1.1s)
```

Nenhuma mensagem de strict mode — um único elemento encontrado.

---

## 6. Login inválido — teste passando vs falhando

### ✅ CORRETO — credenciais erradas, fica em `/login`

```text
  ✓  3 [login] › e2e\login.spec.ts:18:3 › rejeita credenciais inválidas (1.4s)
```

Nenhum erro — o assert `toHaveURL(/\/login/)` bate com a realidade.

---

### ❌ ERRADO — app redireciona para rota inesperada (hipotético)

```text
  ✘  3 [login] › e2e\login.spec.ts:18:3 › rejeita credenciais inválidas (890ms)

    Error: expect(page).toHaveURL(expected)

    Expected pattern: /\/login/
    Received string:  "http://localhost:5173/erro"
```

**Interpretação:** o **produto mudou** (nova página de erro) ou há **bug** de navegação. O teste precisa alinhar com o comportamento esperado do negócio — não é só “consertar o teste”.

---

## 7. Browser fechado no meio do fluxo

### ❌ ANTES — Vite reiniciou ou janela headed fechada

```text
  ✘  1 [setup] › e2e\auth.setup.ts:5:1 › pré-condição: API e sessão admin (4.1s)

    Error: page.goto: Target page, context or browser has been closed
    Call log:
      - navigating to "http://localhost:5173/login", waiting until "load"

       at helpers\autenticar.ts:16

    > 16 |   await page.goto('/login')
```

**Diferença do erro 2:** aqui a porta **pode** estar aberta; o processo do navegador **morreu** durante o `goto`.

→ Não feche o Chromium manualmente em headed; mantenha `npm run dev` estável.

---

## 8. Atalhos “Contas de desenvolvimento” (só dev)

### ❌ ANTES — front em preview de produção

```text
  ✘  5 [login] › e2e\login.spec.ts:36:3 › com Vite em modo dev exibe atalhos... (10.8s)

    Error: expect(locator).toBeVisible()

    Locator: getByText(/Contas de desenvolvimento/i)
    Expected: visible
    Timeout: 10000ms
```

**Screenshot:** login normal, **sem** bloco de atalhos — esperado em build prod.

→ Use `npm run dev`, não `npm run preview` com build de produção.

---

### ✅ DEPOIS — `npm run dev`

```text
  ✓  5 [login] › e2e\login.spec.ts:36:3 › com Vite em modo dev exibe atalhos... (412ms)
```

---

## 9. Chromium não instalado

### ❌ ANTES — primeira execução sem `playwright install`

```text
PS C:\Java\Farmacia\farmacia-web> npm run test:e2e

Running 9 tests using 4 workers

  ✘  1 [setup] › e2e\auth.setup.ts:5:1 › pré-condição: API e sessão admin (2ms)

    Error: browserType.launch: Executable doesn't exist at
    C:\Users\LENOVO\AppData\Local\ms-playwright\chromium-1148\chrome-win\chrome.exe

    ╔════════════════════════════════════════════════════════════╗
    ║ Looks like Playwright was just installed or updated.       ║
    ║ Please run:                                                ║
    ║   npx playwright install                                   ║
    ╚════════════════════════════════════════════════════════════╝
```

**Linha que importa:** caixa com `npx playwright install` — siga exatamente isso.

---

### ✅ DEPOIS

```text
PS C:\Java\Farmacia\farmacia-web> npx playwright install chromium

Downloading Chromium 131.0.6778.33 - 162.3 Mb [====================] 100%
Chromium 131.0.6778.33 downloaded to ...
```

---

## 10. CI local — JAR inexistente

### ❌ ANTES — `test:e2e:ci` sem compilar API

```text
PS C:\Java\Farmacia\farmacia-web> npm run test:e2e:ci

Error: Process from webServer was not able to start. Exit code: 1

  Command: java -jar "C:\Java\Farmacia\farmacia-api\target\farmacia-api-1.0.0-SNAPSHOT.jar" --spring.profiles.active=dev

  Error: Unable to access jarfile C:\Java\Farmacia\farmacia-api\target\farmacia-api-1.0.0-SNAPSHOT.jar
```

→ Compilar antes:

```bash
cd C:\Java\Farmacia
mvn package -pl farmacia-api -am -DskipTests
```

---

### ✅ DEPOIS — webServer sobe API e front

```text
[WebServer] Started java -jar ...
[WebServer] Started npm run dev
Running 9 tests using 1 worker

  9 passed (45.2s)
```

No CI, `workers: 1` — mais lento, mais estável.

---

## Tabela resumo — primeira linha a procurar

| Se você vê isto… | Provável causa | Primeira ação |
|------------------|----------------|---------------|
| `ECONNREFUSED 127.0.0.1:8080` | API off | `mvn spring-boot:run` + Docker |
| `ERR_CONNECTION_REFUSED ...5173` | Vite off | `npm run dev` |
| `API auth falhou: 401` | Credenciais/seed | `credenciais.ts` + perfil dev |
| `Timeout: 15000ms` + Estoque | Não logado | headed + screenshot |
| `strict mode violation` + 2 elements | Seletor amplo | `linkMenu()` / `complementary` |
| `Executable doesn't exist` | Browser | `npx playwright install chromium` |
| `Unable to access jarfile` | CI sem build | `mvn package` |
| `7 did not run` | Setup falhou | Corrigir o **primeiro** teste vermelho |

---

## Exercício

1. Copie o bloco **❌ ANTES** do erro 1 e circule mentalmente: arquivo, linha, mensagem raiz.  
2. Suba só o front (sem API) e veja se seu terminal parece com o erro 1 ou 2.  
3. Abra `playwright-report` após uma falha real e compare com o que você esperava neste documento.

Próximo: [06-guia-qa-senior-era-ia.md](./06-guia-qa-senior-era-ia.md)
