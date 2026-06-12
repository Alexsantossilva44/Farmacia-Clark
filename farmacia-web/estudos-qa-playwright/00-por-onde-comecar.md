# Por onde começar (se testes automatizados te confundem)

## O que é um teste E2E?

**E2E** = *End-to-End* (ponta a ponta).

O Playwright abre um **navegador de verdade** (Chromium), acessa o site da Farmácia Clark como um usuário faria, clica, preenche campos e **verifica** se o resultado está certo.

Não testa só uma função isolada (isso seria teste **unitário** no Java). Testa o **fluxo completo**: front + API + tela.

## As três palavras que você vai ver sempre

| Palavra | Significado simples |
|---------|---------------------|
| **Arrange** (preparar) | Deixar o sistema no estado certo antes do teste (ex.: estar logado) |
| **Act** (agir) | Fazer a ação (clicar, navegar, preencher) |
| **Assert** (verificar) | `expect(...)` — conferir se deu certo |

Exemplo no teste de login:

1. **Arrange:** `beforeEach` abre `/login`  
2. **Act:** preenche email/senha e clica em “Acessar painel”  
3. **Assert:** `expect(page).toHaveURL('/')` — URL mudou para o painel?

## Por que temos vários arquivos?

| Arquivo | Papel | Analogia |
|---------|-------|----------|
| `playwright.config.ts` | Regras da “prova” | Edital do exame: tempo, quantas tentativas, qual navegador |
| `credenciais.ts` | Dados fixos | Gabarito com usuário e senha de teste |
| `autenticar.ts` | Atalho para logar | Entrar na sala antes da prova começar |
| `auth.setup.ts` | Checagem inicial | “A API está ligada?” |
| `login.spec.ts` | Prova da tela de login | Candidato tenta entrar pela porta da frente |
| `app.spec.ts` | Prova já dentro do sistema | Candidato já credenciado visita Estoque e PDV |

## O que NÃO é o Playwright neste projeto

- Não substitui os **51 testes Java** (`mvn test`) — eles testam regras no servidor.  
- Não testa **SNGPC real** nem impressora fiscal.  
- Não precisa da API para ler os arquivos `.md` desta pasta — mas **precisa** para rodar os testes.

## Erro que todo mundo vê no começo

```
ECONNREFUSED 127.0.0.1:8080
```

Tradução: o teste tentou falar com a **API** e ninguém atendeu na porta 8080.

**Solução:** Docker + `mvn spring-boot:run` + `npm run dev` antes de `npm run test:e2e`.

Guia completo com **13 erros típicos** (timeout, strict mode, login, CI…):  
→ [04-duvidas-comuns-erros.md](./04-duvidas-comuns-erros.md)

## Próximo passo

Leia [01-glossario.md](./01-glossario.md) e depois [02-fluxo-dos-testes.md](./02-fluxo-dos-testes.md).  
Quando um teste falhar, consulte [04-duvidas-comuns-erros.md](./04-duvidas-comuns-erros.md).
