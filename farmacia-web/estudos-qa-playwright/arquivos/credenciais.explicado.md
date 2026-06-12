# credenciais.ts — linha por linha

**Arquivo real:** `e2e/helpers/credenciais.ts`  
**Função:** Centralizar dados que os testes usam (usuário, senha, chaves do navegador).

---

## Código completo

```typescript
/** Credenciais do seed dev (`DevAmbienteSeed`) — só válidas com API em perfil `dev`. */
export const ADMIN = {
  email: 'admin@farmacia.com',
  senha: 'admin123',
} as const

export const FARMACIA_NOME_COMPLETO = 'Farmácia Clark'

/** Chaves alinhadas a `farmacia-web/src/lib/auth.ts` (sessão no navegador). */
export const TOKEN_KEY = 'farmacia_token'
export const TOKEN_EXPIRES_KEY = 'farmacia_token_expires'
```

---

## Linha por linha

| Linha | Código | O que faz |
|-------|--------|-----------|
| 1 | Comentário `/** ... */` | Documentação: estes dados vêm do seed Java `DevAmbienteSeed`, só no perfil `dev`. |
| 2 | `export const ADMIN = {` | Cria objeto **exportado** chamado `ADMIN` para importar em outros arquivos. |
| 3 | `email: 'admin@farmacia.com'` | E-mail do administrador de desenvolvimento. |
| 4 | `senha: 'admin123'` | Senha correspondente (nunca use em produção real). |
| 5 | `} as const` | TypeScript: valores **fixos** e imutáveis — evita alterar sem querer. |
| 6 | (linha vazia) | Separação visual. |
| 7 | `export const FARMACIA_NOME_COMPLETO = ...` | Nome da marca para asserts na tela de login. |
| 8 | (linha vazia) | — |
| 9 | Comentário | Explica que as chaves abaixo são iguais às de `src/lib/auth.ts`. |
| 10 | `TOKEN_KEY = 'farmacia_token'` | Nome da chave no `sessionStorage` onde o JWT fica guardado. |
| 11 | `TOKEN_EXPIRES_KEY = ...` | Chave onde guardamos **quando** o token expira (timestamp). |

---

## Por que este arquivo existe? (QA)

- **Um lugar só** para mudar usuário/senha se o seed mudar.  
- **Rastreabilidade:** teste ↔ seed Java ↔ tela de login.  
- **Evita “número mágico”** espalhado em 5 arquivos.

---

## Perguntas de estudo

1. O que acontece se a API rodar em perfil `prod` sem esse usuário?  
2. Por que `TOKEN_KEY` precisa ser igual ao do `auth.ts`?
