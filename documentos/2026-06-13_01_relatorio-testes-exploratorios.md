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
| **C-05** | CPF | Com CPF duplicado: data de nascimento e sexo devem permanecer bloqueados | | | |
| **D-08** | Data Nasc. | Data inválida (menor de 18 anos / futura): cursor seguia para o próximo campo sem bloquear | ✅ CORRIGIDO | Implementado timer de 3s + limpeza do campo + travamento de foco, igual ao padrão CPF | 🟠 Alta |
| **D-09** | Data Nasc. | Enter com data inválida exibia mensagem "Sexo é obrigatório" antes de bloquear o cursor | ✅ CORRIGIDO | `onKeyDown` agora verifica o erro diretamente (sem depender do estado) e chama `e.preventDefault()` + timer imediatamente | 🟡 Média |
| **C-06** | CPF | Após limpeza automática: campo CPF deve aceitar nova digitação | | | |
| **C-07** | CPF | Digitar CPF com menos de 11 dígitos e sair do campo — não deve disparar validação | | | |
| **T-01** | Telefone | Telefone com menos de 10 dígitos → erro ao sair do campo (onBlur) | | | |
| **T-02** | Telefone | Telefone já cadastrado → mensagem de duplicado ao sair do campo | | | |
| **T-03** | Telefone | Com erro em telefone: pressionar Tab não deve travar o foco no campo | | | |
| **T-04** | Telefone | Pressionar Enter no telefone com erro → deve refocar o campo | | | |
| **T-05** | Telefone | Pressionar Enter no telefone válido → deve avançar normalmente | | | |
| **E-01** | Email | Email sem @ → erro ao sair do campo | | | |
| **E-02** | Email | Email já cadastrado → mensagem de duplicado ao sair do campo | | | |
| **E-03** | Email | Com erro em email: pressionar Tab não deve travar o foco no campo | | | |
| **E-04** | Email | Pressionar Enter no email com erro → deve refocar o campo | | | |
| **E-05** | Email | Colar email com espaços (ex: " teste@teste.com ") → deve sanitizar automaticamente | | | |
| **E-06** | Email | Digitar email com TLD inválido e longo (ex: helena@gmail.com.jknknnklnlknlkmnkl2156561) → deve rejeitar | ✅ CORRIGIDO | Bug encontrado e corrigido na sessão — regex atualizada no frontend e backend para exigir TLD somente alfabético (2–63 chars) | 🟡 Média |
| **E-07** | Email | Email inválido → exibir mensagem, aguardar 3s, limpar campo e manter foco para nova digitação (padrão CPF) | ✅ CORRIGIDO | Comportamento ausente — implementado `agendarLimpezaEmailAposErro()` seguindo o mesmo padrão do CPF; retestado e aprovado | 🟡 Média |
| **C-10** | CPF | CPF inválido/duplicado: cursor deve ficar preso no campo CPF (não permite ir para outro campo) até CPF válido ser inserido | ✅ CORRIGIDO | Foco não era travado — usuário conseguia clicar em outros campos durante e após o timer; corrigido com estado `cpfTravandoFoco` que intercepta qualquer tentativa de foco em outro campo e redireciona ao CPF | 🟠 Alta |
| **N-01** | Navegação | Pressionar Enter em qualquer campo envia o formulário em vez de avançar para o próximo campo | ✅ CORRIGIDO | Botões sem `type="button"` eram tratados como submit pelo browser; corrigido adicionando `type="button"` em todos os botões e handler de Enter no formulário que move o foco para o próximo campo | 🟠 Alta |
| **N-02** | Navegação | Enter no campo e-mail ficava travado no campo mesmo com e-mail válido | ✅ CORRIGIDO | Handler do email chamava `e.preventDefault()` mas não avançava o foco após validação bem-sucedida; corrigido chamando `avancarCampo()` após `confirmarEmail()` retornar `true` | 🟡 Média |
| **EN-01** | Endereço | Campo Nº aceitava qualquer caractere (letras puras, especiais) sem restrição | ✅ CORRIGIDO | Filtro alfanumérico + máximo 8 chars + obrigatoriedade de ao menos 1 dígito implementado no frontend e backend; padrão CPF (timer 3s + limpeza + travamento de foco) aplicado | 🟡 Média |
| **L-01** | Layout | Formulário em 375px (mobile) → layout 1 coluna, sem overflow horizontal | | | |
| **L-02** | Layout | Formulário em 768px (tablet) → grid de 2 colunas na seção de dados pessoais | | | |
| **L-03** | Layout | Formulário em 1280px (xl) → grid de 3 colunas + sidebar de 240px visível | | | |
| **L-04** | Layout | Formulário em 1920px → conteúdo respeita max-width de 1800px | | | |
| **L-05** | Layout | Inputs (CPF, telefone, email) não transbordam o container em telas estreitas | | | |
| **L-06** | Layout | Select de cidade não transborda ao selecionar UF com nome longo | | | |
| **L-07** | Layout | Sidebar some em telas menores que 1280px (xl) | | | |
| **S-01** | Scroll | Aba medicamentos: lista longa deve ser scrollável verticalmente | | | |
| **S-02** | Scroll | Formulário de clientes: scroll dentro do card sem mover o cabeçalho da página | | | |
| **S-03** | Scroll | Sidebar de busca de clientes: lista de resultados scrollável independentemente | | | |
| **A-01** | API | POST /clientes com CPF inválido → 400 Bad Request com mensagem de campo | | | |
| **A-02** | API | POST /clientes com CPF duplicado → 409 Conflict | | | |
| **A-03** | API | POST /clientes sem campos obrigatórios → 400 com lista de violações | | | |
| **A-04** | API | GET /clientes?nome=João → retorna clientes filtrados corretamente | | | |

---

## Resumo Quantitativo

| Categoria | Quantidade |
|-----------|-----------|
| Total de testes executados | 28 |
| ✅ Passou | |
| ❌ Falhou — bugs encontrados | |
| 🔴 Críticos | |
| 🟠 Alta prioridade | |
| 🟡 Média prioridade | |
| ⚪ Baixa prioridade | |

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
