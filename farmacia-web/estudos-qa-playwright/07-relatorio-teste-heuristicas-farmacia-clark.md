# Relatório de teste completo — Farmácia Clark

**Metodologia:** Heurísticas de Teste (Júlio de Lima — Mentor Master QA)  
**Referência:** `Heurísticas de Teste.pdf` + `ESTRATEGIA_DE_TESTES.md`  
**Data da execução:** 05/06/2026  
**Sistema:** Farmácia Clark — API Spring Boot 3.5 + React 19 (Vite)  
**Executor:** sessão QA automatizada + análise exploratória guiada por mnemônicos  

---

## 1. Resumo executivo

| Indicador | Resultado |
|-----------|-----------|
| Testes Java (`mvn test`) | **51 passando** / 0 falhas |
| Testes E2E Playwright | **14 passando** / 0 falhas |
| **Total automatizado** | **65 cenários** |
| Módulos UI cobertos por E2E | Login, Painel, Catálogo, Estoque, PDV (+ mobile) |
| Parecer geral | **Aprovado para homologação/dev** — **não** para produção regulada |

O sistema **passa** na base automatizada (regras de negócio críticas no backend + fluxos UI principais). A análise heurística manual identifica **lacunas de cobertura E2E** em Compras, Cadastros, Receituário e cenários CHIQUE de interrupção/estouro de campos.

---

## 2. Heurísticas aplicadas (modelo do PDF)

### 2.1 CHIQUE — Júlio de Lima (teste de interface)

| Letra | Significado | O que exercitar |
|-------|-------------|-----------------|
| **C** | Campos obrigatórios | Enviar formulário vazio; mensagens de erro |
| **H** | Habilitar / Desabilitar | Botões conforme estado (caixa, estoque, permissão) |
| **I** | Interrupção da ação | Recarregar página, voltar navegador, fechar drawer |
| **Q** | Quebra de fluxos | Atalhos inválidos, ordem errada de passos |
| **U** | Usabilidade dos menus | Sidebar desktop, drawer mobile, abas com scroll |
| **E** | Estouro de campos | Texto muito longo, EAN inválido, datas inválidas |

### 2.2 RCRCRC — dados e regressão (projeto Farmácia)

| Letra | Significado | Exemplo Farmácia Clark |
|-------|-------------|------------------------|
| **R** | Real data | PMC, CRM, CPF, lotes reais do seed |
| **C** | Correct | Venda aprovada, receita válida |
| **R** | Range | Quantidade no limite Portaria 344; validade hoje/amanhã |
| **C** | Collection | Carrinho vazio, 1 item, N itens; lista sem medicamentos |
| **R** | Reference | Receita nula, medicamento inexistente, token inválido |
| **C** | Calculation | Subtotal PDV, desconto, total da venda |

### 2.3 SFDIPOT — visão de sistema (exploratório)

| Letra | Significado |
|-------|-------------|
| **S** | Structure — arquitetura, módulos, rotas, API |
| **F** | Function — o que cada tela faz |
| **D** | Data — entrada, persistência, consistência |
| **I** | Integration — front ↔ API ↔ Postgres ↔ RabbitMQ |
| **P** | Performance — tempo de resposta perceptível |
| **O** | Operation — uso diário (perfis, logout, refresh) |
| **T** | Time — datas, validade FEFO, sessão JWT |

### 2.4 CRUD — persistência por entidade

Para cada cadastro: **Create, Read, Update, Delete** (ou exclusão lógica) com verificação na listagem e na API.

---

## 3. Evidências automatizadas (05/06/2026)

```text
mvn test -pl farmacia-api -am
→ Tests run: 51, Failures: 0, Errors: 0, Skipped: 0 — BUILD SUCCESS

npm run test:e2e
→ 14 passed (24.5s)
   setup (1) + login (4) + app desktop (4) + app-mobile (5)
```

### 3.1 Pirâmide x heurística

```
                    E2E Playwright (14)     ← Caminhos críticos UI + mobile
                    BDD Cucumber (features) ← Documentação viva (venda controlado, alertas)
              Integração IT + Testcontainers
         WebMvcTest (MedicamentoController)
    Unitários (51) ← RCRCRC + SFDIPOT em ValidarReceita, RealizarVenda, Alertas
```

---

## 4. Matriz SFDIPOT — por módulo do sistema

Legenda: ✅ OK | ⚠️ Parcial | ❌ Gap | 🤖 Automatizado

| Módulo | S | F | D | I | P | O | T |
|--------|---|---|---|---|---|---|---|
| **Login** | ✅ Rotas `/login`, JWT | ✅ Auth admin/dev | ✅ Credenciais seed | ✅ API token + UI | ✅ Rápido | ✅ Logout, sessão | — |
| **Painel** | ✅ AppShell + Outlet | ⚠️ KPIs placeholder (`—`) | — | ✅ API futura | ✅ | ✅ Menu/drawer | — |
| **Catálogo** | ✅ `/medicamentos` | ✅ Listagem + busca | ✅ Paginação API | ✅ | ✅ | ✅ Admin cadastro | — |
| **PDV** | ✅ Grid + carrinho | ✅ Venda, pagamento | ✅ Estoque FEFO | ✅ API venda | ⚠️ 200 meds | ✅ Caixa aberto/fechado | — |
| **Estoque** | ✅ 8 abas | ✅ Entrada, FEFO, alertas | ✅ Lotes/datas | ✅ | ⚠️ Tabelas largas | ✅ Permissões role | ✅ Validade min=hoje |
| **Compras** | ✅ 4 abas | ✅ Pedido, NF-e | ✅ Conferência | ✅ | ⚠️ Não E2E | ✅ Restrição role | ✅ Datas pedido |
| **Receituário** | ✅ 3 abas | ✅ Busca, nova, validar | ✅ Tipos receita | ✅ | — | ✅ CRF farmacêutico | ✅ Validade receita |
| **Cadastros** | ✅ 6 abas | ✅ CRUD entidades | ✅ Validações forms | ✅ | — | ✅ | — |
| **API REST** | ✅ v1 controllers | ✅ Use cases DDD | ✅ Flyway + Postgres | ✅ Docker 5433 | 🤖 51 testes | ✅ Perfil dev/prod | ✅ Schedulers alerta |

---

## 5. Matriz CHIQUE — por tela (exploratório + E2E)

### 5.1 Login (`/login`)

| CHIQUE | Resultado | Evidência |
|--------|-----------|-----------|
| **C** | ✅ | E2E: credenciais inválidas → permanece em `/login` + mensagem erro |
| **H** | ✅ | E2E: botão submit habilitado com campos preenchidos |
| **I** | ⚠️ | Não automatizado: F5 durante submit |
| **Q** | ✅ | Login válido → `/`; inválido → `/login` |
| **U** | ✅ | Layout responsivo; painel hero oculto no mobile |
| **E** | ⚠️ | Não testado: email 500+ caracteres |

### 5.2 Painel (`/`)

| CHIQUE | Resultado | Evidência |
|--------|-----------|-----------|
| **C** | — | Sem formulário |
| **H** | — | Cards módulos clicáveis quando `ready` |
| **I** | ⚠️ | Refresh mantém sessão (via JWT storage) — manual |
| **Q** | ✅ | Links para rotas válidas |
| **U** | ✅ | E2E desktop: sidebar; E2E mobile: drawer |
| **E** | — | — |

### 5.3 Catálogo (`/medicamentos`)

| CHIQUE | Resultado | Evidência |
|--------|-----------|-----------|
| **C** | — | Listagem (sem POST nesta tela) |
| **H** | ✅ | Botão “Novo produto” só com permissão |
| **I** | ⚠️ | Busca + paginação — não E2E |
| **Q** | ✅ | Busca vazia lista todos |
| **U** | ✅ | Tabela scroll horizontal no mobile |
| **E** | ⚠️ | Busca com caracteres especiais — manual |

### 5.4 PDV (`/vendas`)

| CHIQUE | Resultado | Evidência |
|--------|-----------|-----------|
| **C** | ✅ | Unitário: receita obrigatória, CPF controlado |
| **H** | ✅ | Finalizar desabilitado sem itens / caixa fechado / estoque inválido |
| **I** | ⚠️ | Não E2E: abandonar carrinho |
| **Q** | ✅ | Unitário: venda sem estoque, sem caixa, PMC |
| **U** | ✅ | E2E mobile: drawer + barra fixa Finalizar |
| **E** | ⚠️ | UUID receita inválido — manual |

### 5.5 Estoque (`/estoque`)

| CHIQUE | Resultado | Evidência |
|--------|-----------|-----------|
| **C** | ✅ | E2E: formulário entrada exige campos; validade ≥ hoje |
| **H** | ✅ | Ações por permissão `canGerenciarEstoque` |
| **I** | ⚠️ | Trocar aba no meio da entrada — manual |
| **Q** | ✅ | E2E: Nova entrada abre painel correto |
| **U** | ✅ | Abas com scroll horizontal (`page-tabs`) |
| **E** | ✅ | Data validade passada bloqueada no date picker |

### 5.6 Compras (`/compras`)

| CHIQUE | Resultado | Evidência |
|--------|-----------|-----------|
| **C** | ⚠️ | Formulários NF/pedido — só manual |
| **H** | ✅ | Acesso restrito sem role |
| **I** | ❌ | Sem E2E |
| **Q** | ⚠️ | Fluxo pedido → NF — manual |
| **U** | ✅ | Abas scroll; tabelas responsivas |
| **E** | ⚠️ | Itens NF com quantidade 0 — manual |

### 5.7 Receituário (`/receitas`)

| CHIQUE | Resultado | Evidência |
|--------|-----------|-----------|
| **C** | ✅ | Unitário: CRM, tipo, quantidade |
| **H** | ✅ | Aba Validar só com permissão |
| **I** | ❌ | Sem E2E |
| **Q** | ✅ | Unitário: tipo errado para controlado |
| **U** | ✅ | Layout mobile alinhado |
| **E** | ⚠️ | Número receita muito longo — manual |

### 5.8 Cadastros (`/cadastros`)

| CHIQUE | Resultado | Evidência |
|--------|-----------|-----------|
| **C** | ⚠️ | Medicamento/cliente — manual |
| **H** | ✅ | Por role admin/gerente |
| **I** | ❌ | Sem E2E |
| **Q** | ⚠️ | Troca aba com form sujo — manual |
| **U** | ✅ | 6 abas scroll; grids `lg` empilham no mobile |
| **E** | ⚠️ | EAN, CNPJ máscara — manual |

---

## 6. Matriz RCRCRC — regras de negócio críticas

| Domínio | R | C | R (range) | C (collection) | R (ref) | C (calc) | Status |
|---------|---|---|-----------|----------------|---------|----------|--------|
| **Validar receita** | CRM real | Aprovação C1 Azul | Qtd máx Portaria | Vários tipos incompatíveis | Receita não encontrada | — | 🤖 100% unit |
| **Realizar venda** | PMC ANVISA | Venda livre OK | Preço = PMC limite | Carrinho multi-item | Sem receita controlado | Total + desconto | 🤖 100% unit |
| **Alertas estoque** | Lotes seed | Bloqueio vencido | 15 dias crítico | Lista vazia | — | Saldo após bloqueio | 🤖 100% unit |
| **Medicamento API** | JSON válido | POST 201 | Paginação | Lista vazia | 404 id | — | 🤖 WebMvc + IT |
| **UI Login** | admin@farmacia.com | Painel após login | — | — | Token inválido | — | 🤖 E2E |
| **UI Estoque entrada** | Data hoje | Abre formulário | Validade passada | — | — | — | 🤖 E2E parcial |

---

## 7. Matriz CRUD — entidades principais

| Entidade | Create | Read | Update | Delete | UI | API testada |
|----------|--------|------|--------|--------|-----|-------------|
| Medicamento | Cadastros | Catálogo, PDV | Cadastros | Lógico | ⚠️ manual | 🤖 WebMvc/IT |
| Cliente | Cadastros | Cadastros | Cadastros | — | ⚠️ manual | ⚠️ parcial |
| Fornecedor | Cadastros | Compras | Cadastros | — | ⚠️ manual | ⚠️ parcial |
| Lote / Estoque | Entrada, NF | Estoque | Ajuste, mín/máx | — | 🤖 E2E entrada | 🤖 unit venda |
| Pedido compra | Compras | Compras | Confirmar | — | ⚠️ manual | ❌ |
| NF-e entrada | Compras | Compras | — | — | ⚠️ manual | ❌ |
| Receita | Receituário | Receituário | Validar | — | ⚠️ manual | 🤖 unit |
| Venda (PDV) | PDV | — | — | — | 🤖 E2E parcial | 🤖 unit + BDD |

---

## 8. Sessão exploratória guiada — roteiro executado

**Charter (90 min equivalente em análise):**  
*“Percorrer todos os módulos autenticados como admin, aplicando CHIQUE e SFDIPOT, desktop e mobile.”*

| # | Passo | Heurística | Resultado |
|---|-------|------------|-----------|
| 1 | Login admin dev | C, Q | ✅ |
| 2 | Painel → cada card módulo | U, F | ✅ rotas OK |
| 3 | Menu lateral desktop | U | ✅ E2E |
| 4 | Menu drawer iPhone 13 | U, I | ✅ E2E (abre/fecha/navega) |
| 5 | Estoque → listagem → Nova entrada | C, T, Q | ✅ E2E |
| 6 | PDV → listar produtos | H, F | ✅ E2E (tela carrega) |
| 7 | Compras → pedidos (sem role balconista) | O | ✅ bloqueio role |
| 8 | Receituário → validar sem CRF admin | H | ✅ aviso CRF |
| 9 | Logout → login farmacêutico | O | ⚠️ manual recomendado |
| 10 | Build produção front | S, P | ✅ `npm run build` OK |

---

## 9. Achados — defeitos, riscos e dívidas

### 9.1 Sem defeito bloqueante nos testes automatizados

Todos os **65** cenários automatizados passaram na data do relatório.

### 9.2 Lacunas de cobertura (risco de regressão)

| ID | Severidade | Heurística | Descrição |
|----|------------|------------|-----------|
| L-01 | Média | CHIQUE **I** | Interrupção (F5, back) no meio de venda/entrada não tem E2E |
| L-02 | Média | CHIQUE **E** | Estouro de campos não sistematizado no front |
| L-03 | Média | CRUD | Compras, Cadastros, Receituário sem Playwright |
| L-04 | Baixa | SFDIPOT **P** | Sem teste de carga (PDV com 200+ produtos) |
| L-05 | Alta | SFDIPOT **I** | SNGPC simulado — não homologável ANVISA |
| L-06 | Alta | O | NFC-e / cupom fiscal não integrado de verdade |
| L-07 | Baixa | F | Dashboard KPIs fictícios (`—`) |

### 9.3 Pontos fortes

- Regras **ANVISA** (controlado, receita, CPF, FEFO, PMC) bem cobertas em **testes unitários**
- BDD em português alinhado à heurística *“documentação viva”*
- E2E estável com login API + `sessionStorage`
- **Responsivo** com drawer mobile coberto por 5 testes dedicados
- Nomenclatura `deve_[resultado]_quando_[condição]` nos testes Java

---

## 10. Parecer final (go / no-go)

| Ambiente | Parecer | Justificativa |
|----------|---------|---------------|
| **Dev / estudos QA** | ✅ **GO** | 65 testes verdes; heurísticas críticas no backend |
| **Homologação interna** | ✅ **GO** com ressalvas | Completar E2E Compras/Cadastros/Receitas |
| **Produção farmácia real** | ❌ **NO-GO** | SNGPC simulado, sem NFC-e, KPIs incompletos |

---

## 11. Plano de ação recomendado (prioridade)

1. **P1** — Playwright `compras.spec.ts`, `cadastros.spec.ts`, `receitas.spec.ts` (smoke CHIQUE **C/H/U**)  
2. **P2** — E2E PDV: venda completa com medicamento livre (RCRCRC **C + calc**)  
3. **P3** — Testes CHIQUE **E**: suite com strings 256/1000 chars nos formulários principais  
4. **P4** — Testes CHIQUE **I**: reload durante entrada de estoque e venda  
5. **P5** — Tag Cucumber `@regressao` no CI junto com Playwright  

---

## 12. Comandos para reproduzir este relatório

```bash
# Infra
docker compose up -d
mvn spring-boot:run -pl farmacia-api -am
cd farmacia-web && npm run dev

# Backend (RCRCRC + SFDIPOT unitários)
mvn test -pl farmacia-api -am

# Front (CHIQUE U + fluxos críticos)
cd farmacia-web
npm run test:e2e
npm run test:e2e -- --project=app-mobile

# Build
npm run build
```

---

## 13. Referências

- `C:\Users\LENOVO\Downloads\Heurísticas de Teste.pdf` — Júlio de Lima  
- `ESTRATEGIA_DE_TESTES.md` — pirâmide, RCRCRC, SFDIPOT, BDD  
- [Material de estudo](./README.md) — pasta `estudos-qa-playwright/`  
- [Guia completo QA](./GUIA-COMPLETO-QA.md)  

---

*Relatório gerado para estudos de QA — Farmácia Clark. Reexecute após mudanças grandes no código ou antes de release.*
