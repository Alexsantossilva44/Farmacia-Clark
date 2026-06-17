# Relatório de Testes Exploratórios — Farmacia Clark
**Data:** 13 de Junho de 2026  
**Metodologia:** Glenford Myers — *"Testar software é o processo de executar um programa com a intenção de encontrar erros"*  
**Heurística Aplicada:** SFDIPOT (Structure, Function, Data, Interface, Platform, Operations, Time)  
**Testador:** Alex Santos Silva  
**Sistema:** Farmacia Clark — Java 21 · Spring Boot 3.5 · React 18 · Tailwind CSS  
**Foco da Sessão:** PR #1 — Cadastro de clientes: validação por campo, UX de CPF e layout responsivo

---

## Escopo do Sistema Testado

| Módulo | Responsabilidade |
|--------|-----------------|
| `farmacia-web / ClientesCadastroTab` | Formulário de cadastro: validação por campo, CPF, contato, endereço |
| `farmacia-web / CadastrosPage` | Container das abas: max-width e scroll por aba |
| `farmacia-web / Input, Select, DataNascimentoInput, CidadePorUfSelect` | Componentes UI: overflow e min-w-0 |
| `farmacia-api / ClienteController` | Endpoints REST de cadastro e consulta de clientes |

---

## Mapa Mental da Exploração (SFDIPOT)

```
CADASTRO DE CLIENTES — PR #1
│
├── STRUCTURE (Estrutura)
│   ├── Grid responsivo: 1 col (mobile) → 2 col (md) → 3 col (xl)
│   ├── Sidebar: fixa 240px apenas em xl+
│   ├── min-w-0 em todos os componentes UI
│   └── max-width 1800px exclusivo para aba clientes
│
├── FUNCTION (Função)
│   ├── CPF: valida ao completar 11 dígitos
│   ├── CPF inválido/duplicado: limpa automaticamente após 3s
│   ├── Nome: permanece editável mesmo com CPF duplicado
│   ├── Telefone/Email: valida ao sair do campo (onBlur)
│   └── Enter nos campos de contato: refoca apenas se houver erro
│
├── DATA (Dados)
│   ├── CPF: válido, inválido (dígito verificador errado), duplicado
│   ├── Telefone: 10 dígitos (fixo), 11 dígitos (celular), incompleto
│   ├── Email: formatos válidos, inválidos, duplicados
│   └── Nome: acentos, hífen, >100 caracteres
│
├── INTERFACE (Interface)
│   ├── Tab key: não deve mais bloquear em telefone/email com erro
│   ├── Scroll: formulário de clientes dentro do card
│   └── Scroll: aba de medicamentos
│
├── PLATFORM (Plataforma)
│   ├── Chrome / Firefox / Edge
│   └── Resoluções: 375px · 768px · 1024px · 1280px · 1440px · 1920px
│
├── OPERATIONS (Operações)
│   ├── Fluxo completo: preencher → erro de CPF → corrigir → salvar
│   └── Fluxo de busca: pesquisar cliente → selecionar → editar
│
└── TIME (Tempo) ← timer de limpeza do CPF!
    ├── Timer 3s: campo deve limpar exatamente em 3 segundos
    └── Debounce de verificação de contato
```

---

## Bugs Encontrados — Tabela Completa

| ID | Área | Descrição do Teste | Status | Erro Encontrado | Prioridade |
|----|------|--------------------|--------|-----------------|------------|
| **C-01** | CPF | Digitar CPF inválido (ex: 111.111.111-11) — aguardar limpeza automática | ✅ PASSOU | Nenhum | — |
| **C-02** | CPF | Digitar CPF duplicado já cadastrado — aguardar limpeza automática após 3s | ✅ PASSOU | Nenhum | — |
| **C-03** | CPF | Digitar CPF válido não cadastrado — campos de data e sexo desbloqueiam | ✅ PASSOU | Nenhum | — |
| **C-04** | CPF | Com CPF inválido ou duplicado: cursor deve ficar impossibilitado de ir para qualquer outro campo | ✅ PASSOU | Nenhum | — |
| **C-05** | CPF | Com CPF duplicado: data de nascimento e sexo devem permanecer bloqueados | ✅ PASSOU | Nenhum | — |
| **D-08** | Data Nasc. | Data inválida (menor de 18 anos / futura): cursor seguia para o próximo campo sem bloquear | ✅ CORRIGIDO | Implementado timer de 3s + limpeza do campo + travamento de foco, igual ao padrão CPF | 🟠 Alta |
| **D-09** | Data Nasc. | Enter com data inválida exibia mensagem "Sexo é obrigatório" antes de bloquear o cursor | ✅ CORRIGIDO | `onKeyDown` agora verifica o erro diretamente (sem depender do estado) e chama `e.preventDefault()` + timer imediatamente | 🟡 Média |
| **C-06** | CPF | Após limpeza automática: campo CPF deve aceitar nova digitação | ✅ PASSOU | Nenhum | — |
| **C-07** | CPF | Digitar CPF com menos de 11 dígitos e sair do campo — deve exibir erro, aguardar 3s, limpar e travar foco | ✅ CORRIGIDO | Erro era exibido mas o timer de 3s não era acionado e outros campos exibiam erros falsos ao receber foco; corrigido com `agendarLimpezaCpfAposErro()` no `onBlur` e `focoDirecionado()` guard em todos os campos | 🟠 Alta |
| **T-01** | Telefone | Telefone com menos de 10 dígitos → erro ao sair do campo (onBlur) | ✅ CORRIGIDO | Após exibir erro e limpar o campo, o cursor ia para o próximo campo; corrigido chamando `agendarLimpezaTelefoneAposErro()` sem parâmetro (trava foco) em ambas as chamadas em `confirmarTelefone` | 🟠 Alta |
| **T-02** | Telefone | Telefone já cadastrado → mensagem de duplicado ao sair do campo | ✅ PASSOU | Nenhum | — |
| **T-03** | Telefone | Com erro em telefone: pressionar Tab não deve travar o foco no campo | ✅ PASSOU | Nenhum | — |
| **T-04** | Telefone | Pressionar Enter no telefone com erro → deve refocar o campo | ✅ PASSOU | Nenhum | — |
| **T-05** | Telefone | Pressionar Enter no telefone válido → deve avançar normalmente | ✅ PASSOU | Nenhum | — |
| **E-01** | Email | Email sem @ → erro ao sair do campo | ✅ PASSOU | Nenhum | — |
| **E-02** | Email | Email já cadastrado → mensagem de duplicado ao sair do campo | ✅ PASSOU | Nenhum | — |
| **E-03** | Email | Com erro em email: pressionar Tab não deve travar o foco no campo | ✅ PASSOU | Nenhum | — |
| **E-04** | Email | Pressionar Enter no email com erro → deve refocar o campo | ✅ PASSOU | Nenhum | — |
| **E-05** | Email | Colar email com espaços (ex: " teste@teste.com ") → deve sanitizar automaticamente | ✅ PASSOU | Nenhum | — |
| **E-06** | Email | Digitar email com TLD inválido e longo (ex: helena@gmail.com.jknknnklnlknlkmnkl2156561) → deve rejeitar | ✅ CORRIGIDO | Bug encontrado e corrigido na sessão — regex atualizada no frontend e backend para exigir TLD somente alfabético (2–63 chars) | 🟡 Média |
| **E-07** | Email | Email inválido → exibir mensagem, aguardar 3s, limpar campo e manter foco para nova digitação (padrão CPF) | ✅ CORRIGIDO | Comportamento ausente — implementado `agendarLimpezaEmailAposErro()` seguindo o mesmo padrão do CPF; retestado e aprovado | 🟡 Média |
| **C-10** | CPF | CPF inválido/duplicado: cursor deve ficar preso no campo CPF (não permite ir para outro campo) até CPF válido ser inserido | ✅ CORRIGIDO | Foco não era travado — usuário conseguia clicar em outros campos durante e após o timer; corrigido com estado `cpfTravandoFoco` que intercepta qualquer tentativa de foco em outro campo e redireciona ao CPF | 🟠 Alta |
| **N-01** | Navegação | Pressionar Enter em qualquer campo envia o formulário em vez de avançar para o próximo campo | ✅ CORRIGIDO | Botões sem `type="button"` eram tratados como submit pelo browser; corrigido adicionando `type="button"` em todos os botões e handler de Enter no formulário que move o foco para o próximo campo | 🟠 Alta |
| **N-02** | Navegação | Enter no campo e-mail ficava travado no campo mesmo com e-mail válido | ✅ CORRIGIDO | Handler do email chamava `e.preventDefault()` mas não avançava o foco após validação bem-sucedida; corrigido chamando `avancarCampo()` após `confirmarEmail()` retornar `true` | 🟡 Média |
| **EN-01** | Endereço | Campo Nº aceitava qualquer caractere (letras puras, especiais) sem restrição | ✅ CORRIGIDO | Filtro alfanumérico + máximo 8 chars + obrigatoriedade de ao menos 1 dígito implementado no frontend e backend; padrão CPF (timer 3s + limpeza + travamento de foco) aplicado | 🟡 Média |
| **L-01** | Layout | Formulário em 375px (mobile) → layout 1 coluna, sem overflow horizontal | ✅ PASSOU | Nenhum | — |
| **L-02** | Layout | Formulário em 768px (tablet) → grid de 2 colunas na seção de dados pessoais | ✅ PASSOU | Nenhum | — |
| **L-03** | Layout | Formulário em 1280px (xl) → grid de 3 colunas + sidebar de 240px visível | ✅ PASSOU | Nenhum | — |
| **L-04** | Layout | Formulário em 1920px → conteúdo respeita max-width de 1800px | ✅ PASSOU | Nenhum | — |
| **L-05** | Layout | Inputs (CPF, telefone, email) não transbordam o container em telas estreitas | ✅ PASSOU | Nenhum | — |
| **L-06** | Layout | Select de cidade não transborda ao selecionar UF com nome longo | ✅ PASSOU | Nenhum | — |
| **L-07** | Layout | Sidebar some em telas menores que 1280px (xl) | ✅ PASSOU | Nenhum | — |
| **S-01** | Scroll | Aba medicamentos: lista longa deve ser scrollável verticalmente | ✅ PASSOU | Nenhum | — |
| **S-02** | Scroll | Formulário de clientes: scroll dentro do card sem mover o cabeçalho da página | ✅ PASSOU | Nenhum | — |
| **S-03** | Scroll | Sidebar de busca de clientes: lista de resultados scrollável independentemente | ✅ PASSOU | Nenhum | — |
| **A-01** | API | POST /clientes com CPF inválido → 422 com mensagem de campo | ✅ PASSOU | Status real é 422 (não 400); API rejeita CPF com dígito verificador inválido com `"CPF inválido."`; espera CPF sem formatação (só 11 dígitos) | — |
| **A-02** | API | POST /clientes com CPF duplicado → 409 Conflict | ✅ PASSOU | Nenhum — 409 com `"Já existe um cliente com este CPF."` | — |
| **A-03** | API | POST /clientes sem campos obrigatórios → 422 com lista de violações | ✅ PASSOU | Status real é 422; apenas `nome` e `cpf` aparecem em `fields` — demais campos validados em camadas internas (domínio/use case) | — |
| **A-04** | API | GET /clientes?nome=João → retorna clientes filtrados corretamente | ✅ CORRIGIDO | Endpoint implementado: `GET /api/v1/clientes?nome=` com busca parcial case-insensitive e sem acentos (unaccent); retorna 200 com lista de clientes filtrados | 🟡 Média |

---

## Resumo Quantitativo

| Categoria | Quantidade |
|-----------|-----------|
| Total de testes executados | 39 |
| ✅ Passou (sem bug) | 28 |
| ✅ Corrigido (bug encontrado e corrigido) | 11 |
| ❌ Falhou — funcionalidade ausente | 0 |
| 🔴 Críticos | 0 |
| 🟠 Alta prioridade | 5 (D-08, C-07, C-10, N-01, T-01) |
| 🟡 Média prioridade | 6 (D-09, E-06, E-07, N-02, EN-01, A-04) |
| ⚪ Baixa prioridade | 0 |

---

## Bugs Corrigidos por Sessão

### Sessão 13/06/2026

1. **Select UF com busca por letra** — `Select.tsx` ganhou prop `searchable` com campo de busca no topo do dropdown, auto-foco e filtro em tempo real. Commit `1047328`.
2. **Telefone segue padrão CPF** — erro → 2s → limpa → trava foco. Adicionado: `telefoneTravandoFoco`, `telefoneResetTimerRef`, `agendarLimpezaTelefoneAposErro`. Commit `39f3f62`.
3. **`focoDirecionado()` guard em todos os onBlur** — evita validações falsas quando outro campo trava o foco. Commit `39f3f62`.
4. **Botão "Cadastrar cliente" condicional** — função `formularioPronto()` habilita o botão apenas quando todos os 11 campos obrigatórios estão preenchidos. Commit `39f3f62`.

### Sessão 15/06/2026

5. **T-01 — Cursor avançava após erro de telefone** — `agendarLimpezaTelefoneAposErro(false)` não travava o foco; corrigido removendo o parâmetro (padrão `true`). Commit `ce92a8a`.
6. **Busca por CPF preenchia formulário com CPF inexistente** — handler 404 populava o campo CPF com o valor buscado; corrigido para limpar o campo. Commit `ce92a8a`.
7. **Trocar dígito no Buscar por CPF não limpava o formulário** — corrigido para limpar todos os campos imediatamente ao detectar `clienteId` definido. Commit `ce92a8a`.
8. **Mensagem de CPF não encontrado com texto desnecessário** — simplificado de "CPF não cadastrado. Preencha os dados para novo cliente." para "CPF não cadastrado." Commit `ce92a8a`.
9. **Barra de progresso no botão "Cadastrar cliente"** (melhoria de UX) — sobreposição escura encolhe da direita para a esquerda conforme os 11 campos obrigatórios são preenchidos, revelando o verde mint. Commit `ce92a8a`.

---

## Análise de Risco por Área

### ⏱️ Área de Atenção: Timer de Limpeza do CPF (C-01/C-02)
O CPF é limpo após 3 segundos via `setTimeout`. Riscos potenciais:
- O timer pode ser cancelado se o componente for desmontado antes dos 3s (memory leak / chamada em componente desmontado)
- Se o usuário começar a digitar no campo nome durante os 3s, o comportamento pode ser inesperado
> **Pergunta que o QA deve fazer:** *"O que acontece se o usuário clicar em outro campo durante os 3 segundos?"*

### 🖥️ Área de Atenção: Layout Responsivo (L-01 a L-07)
Grids com `min-w-0` e colunas fracionadas são corretos, mas podem revelar comportamentos inesperados com:
- Conteúdo dinâmico (mensagens de erro longas que esticam a célula)
- Inputs com placeholder longo
- Select de cidade com nome de cidade muito longo

### ⌨️ Área de Atenção: Navegação por Teclado (T-03/E-03)
A remoção do bloqueio de Tab é uma melhoria de UX, mas precisa ser validada:
- O foco deve avançar para o próximo campo em ordem lógica
- Shift+Tab deve funcionar corretamente (navegação reversa)

---

## Lições Aprendidas — Para o QA Master

| # | Lição |
|---|-------|
| 1 | **Timers são comportamentos assíncronos** — sempre teste o que acontece *durante* o timer, não apenas depois |
| 2 | **Acessibilidade de teclado** deve ser testada explicitamente: Tab, Shift+Tab, Enter em todos os campos interativos |
| 3 | **Layout responsivo** exige testes em múltiplas resoluções — o que funciona em 1920px pode quebrar em 375px |
| 4 | **`min-w-0` em grids** previne overflow, mas conteúdo dinâmico (mensagens de erro) ainda pode esticar células |
| 5 | **Sanitização silenciosa** (email com espaços) deve ser verificada: o usuário precisa saber que o valor foi alterado? |
| 6 | **Testar a API diretamente** revela contratos que o frontend pode mascarar — formato de CPF, status codes reais (422 vs 400) e endpoints ausentes (A-04) só aparecem via Postman/Insomnia |
| 7 | **Endpoints ausentes são bugs de escopo** — GET /clientes?nome= não implementado (A-04) é uma funcionalidade faltante que deve ser reportada como pendência do backend |

---

## Módulo: Cadastro de Medicamentos — Testes Exploratórios

**Data:** 17 de Junho de 2026
**Sessão:** 3
**Foco:** Formulário de cadastro de medicamentos — validação de campos, warnings, barra de progresso e fluxo completo

### Tabela de Testes

| ID | Área | Descrição do Teste | Status | Erro Encontrado | Prioridade |
|----|------|--------------------|--------|-----------------|------------|
| **M-01** | Fluxo completo | Cadastro completo com campos obrigatórios e opcionais (sessão 16/06) | ✅ PASSOU | Nenhum | — |
| **M-02** | Nome comercial | onBlur com campo vazio → warning "Lembre-se: Campo Obrigatório." aparece | ✅ PASSOU | Nenhum | — |
| **M-03** | Nome comercial | Warning some após 2 segundos | ✅ PASSOU | Nenhum | — |
| **M-04** | PMC (R$) | onBlur sem valor → warning deve ser a mensagem padronizada | ✅ CORRIGIDO | onBlur exibia "PMC (R$) é obrigatório." (retorno bruto de `validarNumeroPositivo`) em vez de "Lembre-se: Campo Obrigatório." (warning padrão); corrigido em `MedicamentosCadastroTab.tsx` | 🟡 Média |
| **M-05** | PMC (R$) | Warning some após 2 segundos | ✅ PASSOU | Nenhum | — |
| **M-06** | PMC (R$) | Valor negativo no onBlur → warning dispara | ✅ PASSOU | Nenhum | — |
| **M-07** | Fabricante | onBlur sem selecionar → warning "Lembre-se: Campo Obrigatório." | ✅ PASSOU | Nenhum | — |
| **M-08** | Fabricante | Warning some após 2 segundos | ✅ PASSOU | Nenhum | — |
| **M-09** | Botão | Botão "Cadastrar medicamento" desabilitado com formulário vazio | ✅ PASSOU | Nenhum | — |
| **M-10** | Botão / Progresso | Barra de progresso em 33% com só Nome comercial preenchido | ✅ PASSOU | Nenhum | — |
| **M-11** | Botão / Progresso | Barra de progresso em 67% com Nome + PMC preenchidos | ✅ PASSOU | Nenhum | — |
| **M-12** | Botão / Progresso | Barra em 100% e botão habilita com Nome + PMC + Fabricante | ✅ PASSOU | Nenhum | — |
| **M-13** | EAN-13 | Campo não aceita letras — somente dígitos | ✅ PASSOU | Nenhum | — |
| **M-14** | EAN-13 | Campo limita a 13 dígitos | ✅ PASSOU | Nenhum | — |
| **M-15** | Concentração | Campo limita a 50 caracteres (maxLength) | ✅ PASSOU | Nenhum | — |
| **M-16** | Apresentação | Campo limita a 100 caracteres (maxLength no browser) | ✅ PASSOU | Nenhum — maxLength=100 correto; browser bloqueia digitação manual; ferramentas de automação podem bypass (não é cenário de usuário real) | — |
| **M-17** | Nível de controle | Mudar para não-LIVRE ativa "Exige receita" automaticamente; voltar para LIVRE desmarca | ✅ PASSOU | Nenhum | — |
| **M-18** | Categoria | onBlur permanente — warning não some em 2s | ✅ PASSOU | Campo sempre tem valor padrão (CATEGORIA_DEV_ID); usuário real nunca aciona o warning | — |
| **M-19** | Navegação | Enter avança foco para o próximo campo | ✅ PASSOU | Nenhum | — |
| **M-20** | Fluxo completo | Cadastro com todos os campos preenchidos → "Medicamento cadastrado." | ✅ PASSOU | Nenhum | — |
| **M-21** | PMC (R$) — Máscara | Digitar dígitos → campo exibe formato "R$ X,XX" | ✅ PASSOU | Nenhum | — |
| **M-22** | PMC (R$) — Máscara | Sinal negativo bloqueado: "-500" → "R$ 5,00" | ✅ PASSOU | Nenhum | — |
| **M-23** | PMC (R$) — Máscara | Letras bloqueadas: "abc" → campo vazio | ✅ PASSOU | Nenhum | — |
| **M-24** | PMC (R$) — Máscara | Separador de milhar correto: "123456" → "R$ 1.234,56" | ✅ PASSOU | Nenhum | — |
| **M-25** | Edição | Clicar em medicamento da lista → formulário preenchido com dados + PMC com máscara | ✅ PASSOU | Nenhum | — |
| **M-26** | Edição — PMC | PMC exibe valor correto ao abrir edição de medicamento (ex.: Amoxil → "R$ 35,99") | ✅ PASSOU | Nenhum | — |
| **M-27** | Edição — Salvar | Alterar PMC e clicar "Salvar alterações" → lista atualiza e exibe novo valor | ✅ PASSOU | Nenhum | — |
| **M-28** | Edição — Cancelar | Clicar "Cancelar" sem salvar → volta para "Novo medicamento" com campos vazios e botão desabilitado | ✅ PASSOU | Nenhum | — |
| **M-29** | Exclusão | Inativar medicamento → confirmação exibida; após confirmar, medicamento some da lista | ✅ CORRIGIDO | Lista do cadastro exibia medicamentos inativos (sem filtro `ativo`); corrigido com `.filter(m => m.ativo !== false)` em `MedicamentosCadastroTab.tsx` | 🟡 Média |
| **M-30** | API — EAN duplicado | Tentar cadastrar com EAN já existente → API retorna 409 com mensagem clara | ✅ PASSOU | Nenhum | — |
| **M-31** | API — PMC null | Enviar PMC null → `@NotNull` rejeita com HTTP 422 e mensagem "PMC é obrigatório" | ✅ PASSOU | Nenhum | — |
| **M-32** | Nome duplicado | Tentar cadastrar "Amoxil" (já existente) → bloqueado com "Já existe um medicamento cadastrado com o nome: Amoxil" | ✅ CORRIGIDO | Sem a validação, nomes duplicados eram aceitos silenciosamente; corrigido com verificação na camada de use case + índice UNIQUE parcial no banco | 🔴 Alta |
| **M-33** | Nome duplicado — case-insensitive | Tentar cadastrar "AMOXIL" (maiúsculas) com "Amoxil" existente → bloqueado com "Já existe um medicamento cadastrado com o nome: AMOXIL" | ✅ PASSOU | Nenhum | — |
| **M-34** | PMC zerado | Digitar PMC = R$ 0,00 → botão "Cadastrar medicamento" fica desabilitado; envio bloqueado no frontend antes de chegar à API | ✅ PASSOU | Nenhum | — |
| **M-35** | EAN-13 inválido | Digitar EAN com 12 dígitos → API rejeita; mensagem exibe "EAN deve conter exatamente 13 dígitos numéricos" | ✅ CORRIGIDO | Duas anotações Bean Validation (`@Size` + `@Pattern`) disparavam juntas, colando duas mensagens sem separador; `@Size` removida (redundante com `\d{13}`) e mensagem do `@Pattern` unificada; join frontend alterado de `' '` para `'; '` | 🟡 Média |

### Resumo da Sessão 17/06/2026

| Resultado | Quantidade |
|-----------|-----------|
| ✅ Passou (sem bug) | 30 |
| ✅ Corrigido (bug encontrado e corrigido) | 4 |
| ❌ Falhou | 0 |
| **Total** | **34** |

### Bugs Corrigidos — Sessão 17/06/2026

**M-04 — PMC (R$): warning inconsistente no onBlur**
- **Arquivo:** `farmacia-web/src/pages/cadastros/MedicamentosCadastroTab.tsx`
- **Comportamento incorreto:** onBlur exibia `"PMC (R$) é obrigatório."` — retorno bruto de `validarNumeroPositivo()`, com tom de erro definitivo
- **Comportamento correto:** deve exibir `"Lembre-se: Campo Obrigatório."` — warning padrão do formulário, consistente com Nome comercial e Fabricante
- **Correção:** substituído `err` por `'Lembre-se: Campo Obrigatório.'` no `setFieldErrors` dentro do `onBlur` do PMC
- **Prioridade:** 🟡 Média

**M-29 — Lista de cadastro exibia medicamentos inativos**
- **Arquivo:** `farmacia-web/src/pages/cadastros/MedicamentosCadastroTab.tsx`
- **Comportamento incorreto:** após inativar medicamento, ele continuava visível na lista de cadastro
- **Comportamento correto:** lista deve exibir apenas medicamentos com `ativo: true`
- **Causa:** `medsQuery.data?.map(...)` iterava todos os registros sem filtrar pelo campo `ativo`
- **Correção:** adicionado `.filter((m) => m.ativo !== false)` antes do `.map()`
- **Observação:** `VendasPage.tsx` já aplicava esse filtro; cadastro estava inconsistente
- **Prioridade:** 🟡 Média

**M-32 — Nome comercial duplicado era aceito sem bloqueio**
- **Arquivos afetados:** `CadastrarMedicamentoUseCase.java`, `AtualizarMedicamentoUseCase.java`, `MedicamentoDuplicadoException.java`, `MedicamentoRepository.java`, `MedicamentoJpaRepository.java`, `MedicamentoRepositoryAdapter.java` + migration `V9__unique_nome_comercial.sql`
- **Comportamento incorreto:** cadastrar um medicamento com nome já existente (ex.: "Amoxil") era aceito pela API sem retornar erro
- **Comportamento correto:** API deve retornar HTTP 409 com mensagem "Já existe um medicamento cadastrado com o nome: Amoxil"; frontend exibe o texto em vermelho no topo do formulário
- **Causa:** nenhuma verificação de unicidade de nome na camada de domínio; ausência de constraint UNIQUE no banco para `nome_comercial`
- **Correção:**
  - `CadastrarMedicamentoUseCase`: chamada a `existsByNomeComercial()` antes de salvar → lança `MedicamentoDuplicadoException.porNome()`
  - `AtualizarMedicamentoUseCase`: chamada a `existsByNomeComercialExcluindo()` (exclui o próprio ID) → lança exceção se outro registro tiver o mesmo nome
  - `MedicamentoDuplicadoException`: novo factory `porNome(String)` com mensagem específica
  - `MedicamentoJpaRepository`: dois métodos Spring Data — `existsByNomeComercialIgnoreCaseAndAtivoTrue` e `existsByNomeComercialIgnoreCaseAndAtivoTrueAndIdNot`
  - `V9__unique_nome_comercial.sql`: índice UNIQUE parcial `lower(nome_comercial) WHERE ativo = true` (segurança no nível do banco; inativos ficam fora do índice)
- **Prioridade:** 🔴 Alta

**M-35 — EAN inválido: duas mensagens coladas sem separador**
- **Arquivos:** `farmacia-api/.../MedicamentoInput.java` + `farmacia-web/src/lib/erros.ts`
- **Comportamento incorreto:** EAN com 12 dígitos disparava `@Size` e `@Pattern` simultaneamente; frontend juntava as mensagens com espaço simples → "Código EAN deve ter exatamente 13 dígitos Código EAN deve conter apenas números"
- **Comportamento correto:** uma única mensagem clara → "EAN deve conter exatamente 13 dígitos numéricos"
- **Correção backend:** removida anotação `@Size(min=13,max=13)` (redundante com `\d{13}`); mensagem do `@Pattern` atualizada para "EAN deve conter exatamente 13 dígitos numéricos"
- **Correção frontend:** `campos.map(...).join(' ')` → `join('; ')` em `erros.ts` (defesa para múltiplas mensagens futuras)
- **Prioridade:** 🟡 Média

### Distinção WARNING × ERROR neste formulário

| Momento | Tipo | Mensagem | Duração |
|---------|------|----------|---------|
| Sair do campo obrigatório vazio (onBlur) | ⚠️ WARNING | "Lembre-se: Campo Obrigatório." | 2 segundos, some |
| Clicar em Cadastrar com campo vazio | ⚠️ WARNING | "Lembre-se: Campo Obrigatório." | 2 segundos, some |
| Erro retornado pela API | ❌ ERROR | Ex.: "Já existe um medicamento com este nome." | Permanente até nova ação |
