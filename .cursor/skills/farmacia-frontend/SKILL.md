---
name: farmacia-frontend
description: >-
  Desenvolve o front-end do Sistema Farmacêutico (farmacia-web): React 19, Vite 6,
  TypeScript, Tailwind v4, TanStack Query, design system Farmácia Clark com cores hiper vivas
  e visual artesanal. Use ao criar telas, componentes, integração com a API REST,
  autenticação JWT, ou quando o usuário mencionar UI, front-end ou farmacia-web.
---

# Farmacia Front-end

## Stack obrigatória

| Camada | Tecnologia |
|--------|------------|
| Build | Vite 6 + TypeScript strict |
| UI | React 19, React Router 7 |
| Estilo | Tailwind CSS v4 (`@tailwindcss/vite`) |
| Dados | TanStack Query v5 |
| Ícones | Lucide React |
| API | Fetch nativo + tipos espelhando DTOs Java |

**Não usar**: Material UI, Chakra, shadcn genérico copiado, Inter/Roboto, gradientes roxo-azul clichê de IA.

## Onde vive o código

```
farmacia-web/
├── src/
│   ├── lib/          # api.ts, auth.ts, format.ts
│   ├── types/        # espelho dos Models Java
│   ├── components/
│   │   ├── layout/   # AppShell, Sidebar
│   │   └── ui/       # Button, Badge, Card, Input
│   └── pages/        # Login, Dashboard, Medicamentos, …
```

Skill complementar: [design-tokens.md](design-tokens.md)

## API backend (dev)

- Base URL: `VITE_API_URL` ou `http://localhost:8080`
- Auth: `POST /api/v1/auth/token` body `{ email, senha }` → `{ token, tipo, expiraEmSegundos }`
- Header: `Authorization: Bearer <token>`
- Erros: RFC 7807 `{ status, title, detail, userMessage }`
- CORS já liberado para `http://localhost:*` em `SecurityConfig`

**Credenciais seed (perfil dev):**

| Papel | E-mail | Senha |
|-------|--------|-------|
| Admin | admin@farmacia.com | admin123 |
| Balconista | balconista@farmacia.com | bal123 |
| Farmacêutico | farmaceutico@farmacia.com | farm123 |

**Endpoints REST expostos hoje:**

- `GET/POST/PUT/DELETE /api/v1/medicamentos` (paginado)
- `GET /api/v1/pdv/contexto` — PDV operacional + status caixa
- `POST /api/v1/vendas`, `GET /api/v1/vendas/{id}`, cancelamento
- `GET/POST /api/v1/caixa/*` — abrir, fechar, consultar aberto
- `GET /api/v1/estoque/*` — saldo, lotes FEFO, alertas
- `POST/GET/PUT /api/v1/receitas/*` — cadastro e validação
- `POST/GET/PUT /api/v1/clientes/*` — CRUD mínimo

**Ainda só no backend (use cases existem, REST pendente):** entrada/ajuste estoque, compras/fornecedores.

## Design — Farmácia Clark (anti-AI-slop)

Objetivo: cores **hiper vivas** sobre base escura profunda, mas layout **assimétrico e editorial** — parece produto feito por equipe, não template de IA.

### Regras visuais

1. **Base**: `#070b14` com mesh gradient sutil (mint + coral em 3–5% opacidade)
2. **Acentos**: mint `#2dd4a8`, coral `#ff3366`, âmbar `#ffb020` — nunca os três no mesmo botão
3. **Tipografia**: Outfit (UI) + JetBrains Mono (EAN, códigos ANVISA)
4. **Cards**: glass `bg-white/[0.03]` + borda `border-white/10` + sombra colorida leve no hover
5. **Espaçamento**: grid 4px; seções com respiro assimétrico (ex.: hero deslocado 12px)
6. **Micro-interação**: `transition` 200ms; sem animações exageradas ou parallax
7. **PT-BR**: labels reais do domínio (PMC, ANVISA, SNGPC, FEFO, receita branca especial)
8. **Badges de controle**: cor semântica por `NivelControle` (ver `design-tokens.md`)

### Proibido

- Hero centralizado com “Welcome to…” genérico
- Cards idênticos em grid 3×3 simétrico sem hierarquia
- Ícones decorativos sem função
- Placeholder lorem; usar dados mock realistas de farmácia

## Padrões de código

### Auth

```typescript
// lib/auth.ts — token em sessionStorage, expiração respeitada
export function getAuthHeader(): HeadersInit { … }
```

Rotas protegidas: wrapper `RequireAuth` redireciona para `/login`.

### API client

```typescript
// lib/api.ts
export async function api<T>(path: string, init?: RequestInit): Promise<T>
```

- 401 → limpar token, redirect login
- Problem → exibir `userMessage` ou `detail`

### TanStack Query

- `queryKey` estável: `['medicamentos', page, sort]`
- `staleTime`: 30_000 para listagens
- Mutations invalidam queries relacionadas

### Novas telas — checklist

1. Mapear use case / controller Java correspondente
2. Tipos em `src/types/` espelhando `*Model` e `*Input`
3. Página em `src/pages/` + rota em `App.tsx`
4. Item no Sidebar com ícone Lucide contextual
5. Estados: loading skeleton, empty state ilustrado, erro com retry
6. Formatação: `Intl.NumberFormat('pt-BR', { style: 'currency', currency: 'BRL' })`

## Domínio → telas (roadmap)

| Módulo backend | Tela front | Status |
|----------------|------------|--------|
| Auth + JWT | Login | ✅ |
| Medicamentos | Catálogo | ✅ |
| Vendas | PDV / Nova venda | ✅ |
| Estoque / FEFO | Painel estoque | ✅ |
| Receituário | Validação receita | ✅ |
| Caixa / PDV | Abertura caixa | ✅ API + seed dev |
| Alertas vencimento | Dashboard alertas | 🔜 |

## Subir ambiente

```bash
# Terminal 1 — infra + API
docker compose up -d
mvn spring-boot:run -pl farmacia-api

# Terminal 2 — front
cd farmacia-web && npm run dev
```

Front: `http://localhost:5173` → API: `http://localhost:8080`

## Verificação antes de entregar

- [ ] Build passa: `npm run build`
- [ ] Login com seed dev funciona
- [ ] Lista medicamentos autenticada
- [ ] Visual segue tokens Farmácia Clark (não genérico)
- [ ] Textos em PT-BR do domínio farmacêutico
