# Exercícios práticos — Playwright Farmácia Clark

Use estes exercícios depois de ler os arquivos em `arquivos/*.explicado.md`.

---

## Nível 1 — Entender o que roda

1. Rode `npm run test:e2e:headed` e observe a **ordem**: setup → login (4) → app (4).
2. Pare a API e rode de novo. Qual projeto falha primeiro? Anote a mensagem.
3. Abra `playwright-report/index.html` após uma falha e localize screenshot + vídeo.

---

## Nível 2 — Ler código

1. No `login.spec.ts`, qual linha prova que o usuário **não** entrou no sistema?
2. No `autenticar.ts`, por que abrimos `/login` antes do `sessionStorage`?
3. Explique em uma frase a diferença entre `page` e `request`.

---

## Nível 3 — Pequenas mudanças (no `e2e/` real, não na pasta de estudo)

1. Adicione um teste em `login.spec.ts` que verifica se o campo senha tem `type="password"`.
2. Em `app.spec.ts`, após abrir Nova entrada, verifique se existe um campo com label ou placeholder de medicamento.
3. Troque temporariamente `ADMIN.senha` para `'errada'` em `credenciais.ts` — qual teste quebra? Por quê?

---

## Nível 4 — Debug

1. Rode `npm run test:e2e:debug` no teste `admin dev autentica e abre o painel`.
2. Use o Inspector para pausar **depois** do click no submit.
3. Anote a URL e um elemento visível nesse momento.

---

## Gabarito rápido (Nível 2)

| Pergunta | Resposta curta |
|----------|----------------|
| Login inválido não entra | Linha 23: `toHaveURL(/\/login/)` |
| Por que `/login` antes do storage | Mesma origem (protocolo + host + porta) para o `sessionStorage` |
| `page` vs `request` | `page` = navegador; `request` = HTTP direto à API sem UI |

---

## Quando estiver confuso

Volte sempre a este fluxo mental:

```
Config → Setup (ambiente OK?) → Login (tela) → App (logado) → Helpers (peças reutilizáveis)
```

Se travar em um `expect`, pergunte: **o que eu esperava ver na tela e o que o teste está procurando?**

Se aparecer mensagem vermelha no terminal, abra o índice em [04-duvidas-comuns-erros.md](./04-duvidas-comuns-erros.md).
