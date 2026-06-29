# Backlog de Bugs — 18/06/2026

## BUG-01 — Prescritores sem operações de Alterar e Excluir

**Módulo:** Cadastros → Prescritores (médicos)
**Severidade:** Alta
**Descoberto em:** Sessão de análise QA — 18/06/2026

### Descrição
O formulário de Prescritores não possui botões nem lógica de edição ou exclusão.
Um médico cadastrado com dados incorretos (nome errado, CRM ou UF trocados,
especialidade inválida) não pode ser corrigido nem removido pela interface.

### Impacto
- Dados incorretos ficam permanentemente no sistema sem possibilidade de correção.
- A única solução seria intervenção direta no banco de dados.
- O módulo de Receituário (emissão de receitas) dependeria de prescritores corretos.

### Evidência
- `api.ts` não possui funções `atualizarPrescritor` nem `excluirPrescritor`.
- `CatalogoController.java` não possui endpoints `PUT` nem `DELETE` para `/prescritores/{id}`.
- `CatalogoSimpleTab.tsx` (aba Prescritores) não exibe botão de edição/exclusão na lista.

### Correção aplicada
Implementado em 18/06/2026:
- Backend: `PUT /api/v1/catalogo/prescritores/{id}` e `DELETE /api/v1/catalogo/prescritores/{id}` (soft delete).
- Frontend `api.ts`: `atualizarPrescritor` e `excluirPrescritor`.
- Frontend `CatalogoSimpleTab.tsx`: lista clicável, formulário com modo edição, botões Salvar alterações / Excluir / Cancelar.

**Status:** CORRIGIDO

---

## BUG-02 — Edição de Categoria bloqueada quando descrição está vazia

**Módulo:** Cadastros → Categorias
**Severidade:** Alta
**Descoberto em:** Sessão QA — 18/06/2026

### Descrição
Ao clicar em uma categoria cadastrada sem descrição (ex.: "Analgésicos") e tentar salvar
a edição, o sistema bloqueava o envio em duas camadas:

1. **Frontend:** `validarFormulario` chamava `obrigatorio(descricao)` mesmo em modo edição,
   retornando erro quando a descrição estava vazia.
2. **Backend:** `atualizarCategoria` usava `@Valid CategoriaInput`, e `CategoriaInput.descricao`
   tem `@NotBlank` — rejeitaria qualquer atualização com descrição vazia (HTTP 422).

### Impacto
Impossível editar qualquer categoria que tenha sido salva sem descrição.
O usuário clicava em "Salvar alterações" e nada acontecia (erro silencioso no campo).

### Correção aplicada
Implementada em 18/06/2026:
- **Frontend `validarFormulario`:** descrição só é exigida em modo criação (`!editId`).
- **Frontend label:** campo mostra "Descrição" (sem asterisco) quando em modo edição.
- **Frontend `onBlur`:** erro de campo obrigatório só dispara em modo criação.
- **Backend `atualizarCategoria`:** removido `@Valid`; nome validado manualmente;
  descrição em branco/nula é armazenada como `null` no banco (coluna permite null).

**Status:** CORRIGIDO
