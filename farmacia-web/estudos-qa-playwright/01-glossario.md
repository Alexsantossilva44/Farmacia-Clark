# Glossário — QA + Playwright + TypeScript

## Testes

| Termo | Explicação |
|-------|------------|
| **Teste automatizado** | Script que repete verificações sem humano clicando |
| **E2E** | Testa interface + integração como o usuário final |
| **Spec** | Arquivo de especificação de teste (`*.spec.ts`) |
| **Setup** | Preparação antes dos testes principais |
| **Fixture** | Coisa que o Playwright “empresta” ao teste (`page`, `request`) |
| **Locator** | Endereço de um elemento na tela (`getByRole`, `getByTestId`) |
| **Assert / expect** | Afirmação: “eu espero que X seja verdade” |
| **Flaky** | Teste que às vezes passa, às vezes falha (rede lenta, etc.) |
| **Timeout** | Tempo máximo de espera antes de falhar |

## Playwright

| Termo | Explicação |
|-------|------------|
| **page** | Uma aba do navegador controlada pelo teste |
| **request** | Cliente HTTP para chamar API sem abrir tela |
| **baseURL** | Prefixo das URLs (`/login` vira `http://localhost:5173/login`) |
| **project** | Grupo de testes (setup, login, app) |
| **worker** | Processo paralelo que roda testes |
| **headed** | Mostra o navegador na tela |
| **debug** | Modo passo a passo (Inspector) |
| **webServer** | Playwright sobe API/front automaticamente (no CI) |

## TypeScript (só o que aparece nos testes)

| Sintaxe | Explicação |
|---------|------------|
| `import { x } from 'y'` | Traz função/variável de outro arquivo |
| `export` | Deixa outro arquivo importar |
| `async` / `await` | Espera operação terminar (rede, navegação) |
| `const` | Constante — não muda depois |
| `??` | Se esquerda for `null`/`undefined`, usa direita |
| `as const` | Objeto readonly, valores fixos |
| `as { token: string }` | Diz ao TypeScript o formato do JSON |
| `type Page` | Só tipo, some na compilação |
| `/regex/i` | Texto com padrão flexível, `i` = ignora maiúsculas |

## Farmácia Clark (domínio)

| Termo | Explicação |
|-------|------------|
| **JWT / token** | “Crachá digital” após login |
| **sessionStorage** | Armazenamento no navegador enquanto a aba está aberta |
| **Seed dev** | Usuários criados automaticamente no perfil `dev` da API |
| **ADMIN** | Usuário `admin@farmacia.com` / `admin123` |

## Seletores usados aqui (boa prática)

| Método | Quando usar |
|--------|-------------|
| `getByTestId('login-email')` | Campos críticos com `data-testid` no React |
| `getByRole('button', { name: '...' })` | Botões e links como usuário enxerga |
| `getByRole('heading', { name: /.../ })` | Títulos de página |
| `getByPlaceholder(/.../)` | Campo de busca |
| `getByRole('complementary')` | Região lateral (`<aside>`) |
