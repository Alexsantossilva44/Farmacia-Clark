# Estudos QA — Testes Playwright (Farmácia Clark)

Material em **português claro** para quem está aprendendo testes automatizados E2E.

## Guia completo (um arquivo só — PDF / offline)

**[GUIA-COMPLETO-QA.md](./GUIA-COMPLETO-QA.md)** — reúne todas as partes (intro, glossário, código linha a linha, erros, terminal, carreira sênior + apêndice). ~2.370 linhas, ideal para imprimir ou exportar PDF.

## O código “vivo” (o que o sistema usa de verdade)

| Arquivo | Caminho real no projeto |
|---------|-------------------------|
| Configuração | `farmacia-web/playwright.config.ts` |
| Pré-condição | `farmacia-web/e2e/auth.setup.ts` |
| Testes de login | `farmacia-web/e2e/login.spec.ts` |
| Testes logado | `farmacia-web/e2e/app.spec.ts` |
| Helper login API | `farmacia-web/e2e/helpers/autenticar.ts` |
| Dados de teste | `farmacia-web/e2e/helpers/credenciais.ts` |
| Helper menu | `farmacia-web/e2e/helpers/navegacao.ts` |

Cópias para leitura offline: pasta `codigo-fonte/` (iguais aos arquivos acima).

## Ordem de leitura recomendada

1. **[00-por-onde-comecar.md](./00-por-onde-comecar.md)** — se você está confuso, comece aqui  
2. **[01-glossario.md](./01-glossario.md)** — palavras que aparecem nos testes  
3. **[02-fluxo-dos-testes.md](./02-fluxo-dos-testes.md)** — o que roda, em que ordem  
4. Helpers (do mais simples ao mais complexo):  
   - [credenciais.explicado.md](./arquivos/credenciais.explicado.md)  
   - [navegacao.explicado.md](./arquivos/navegacao.explicado.md)  
   - [autenticar.explicado.md](./arquivos/autenticar.explicado.md)  
5. Config e testes:  
   - [playwright.config.explicado.md](./arquivos/playwright.config.explicado.md)  
   - [auth.setup.explicado.md](./arquivos/auth.setup.explicado.md)  
   - [login.spec.explicado.md](./arquivos/login.spec.explicado.md)  
   - [app.spec.explicado.md](./arquivos/app.spec.explicado.md)  
6. **[03-exercicios-praticos.md](./03-exercicios-praticos.md)** — prática guiada com gabarito  
7. **[04-duvidas-comuns-erros.md](./04-duvidas-comuns-erros.md)** — erros típicos e como interpretar  
8. **[05-exemplos-terminal-erros.md](./05-exemplos-terminal-erros.md)** — terminal ❌ antes / ✅ depois  
9. **[06-guia-qa-senior-era-ia.md](./06-guia-qa-senior-era-ia.md)** — carreira QA Sênior com IA no mercado  
10. **[07-relatorio-teste-heuristicas-farmacia-clark.md](./07-relatorio-teste-heuristicas-farmacia-clark.md)** — teste completo CHIQUE + RCRCRC + SFDIPOT + CRUD  

## Estrutura da pasta

```
estudos-qa-playwright/
├── GUIA-COMPLETO-QA.md          ← TUDO em um arquivo (PDF/offline)
├── README.md                    ← você está aqui
├── 00-por-onde-comecar.md
├── 01-glossario.md
├── 02-fluxo-dos-testes.md
├── 03-exercicios-praticos.md
├── 04-duvidas-comuns-erros.md   ← ECONNREFUSED, timeout, strict mode…
├── 05-exemplos-terminal-erros.md
├── 06-guia-qa-senior-era-ia.md  ← o que saber para ser sênior hoje
├── 07-relatorio-teste-heuristicas-farmacia-clark.md
├── arquivos/                    ← explicação linha a linha (7 arquivos)
│   ├── credenciais.explicado.md
│   ├── navegacao.explicado.md
│   ├── autenticar.explicado.md
│   ├── playwright.config.explicado.md
│   ├── auth.setup.explicado.md
│   ├── login.spec.explicado.md
│   └── app.spec.explicado.md
└── codigo-fonte/                ← cópias dos .ts para ler offline
    ├── playwright.config.ts
    ├── auth.setup.ts
    ├── login.spec.ts
    ├── app.spec.ts
    └── helpers/
        ├── credenciais.ts
        ├── navegacao.ts
        └── autenticar.ts
```

## Como praticar

```bash
cd farmacia-web
docker compose up -d          # na raiz do repo
mvn spring-boot:run -pl farmacia-api -am
npm run dev                   # outro terminal
npm run test:e2e              # rodar testes
npm run test:e2e:headed       # ver o navegador
npm run test:e2e:debug        # passo a passo (Inspector)
```

## Mapa mental rápido

```
playwright.config.ts  →  define COMO rodar (navegador, timeout, projetos)
        ↓
auth.setup.ts         →  pré-condição: “API e login funcionam?”
        ↓
login.spec.ts         →  testa a TELA de login (sem depender do setup)
        ↓
app.spec.ts           →  testa o sistema JÁ LOGADO (estoque, PDV…)
        ↑
autenticar.ts         →  função que faz login via API + sessionStorage
credenciais.ts        →  email/senha fixos para dev
navegacao.ts          →  achar link do menu sem confundir com o painel
```

Este material é **só para estudo** — não altera o comportamento dos testes.
