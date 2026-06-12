# O que um QA Sênior precisa saber hoje — com IA no mercado

Documento de referência para **carreira**, não só para o Farmácia Clark.  
Atualizado para o contexto de **2025–2026**: IA generativa em todo lugar, mas qualidade de software continua sendo problema humano + sistêmico.

---

## A pergunta certa

Não é: *“A IA vai substituir QA?”*  
É: ***“O que sobra para um sênior quando a IA faz o trabalho repetitivo?”***

Resposta curta: **julgamento de risco, desenho de estratégia, profundidade investigativa, influência no produto e garantia de que o que foi automatizado testa a coisa certa.**

---

## O que a IA já faz bem (e você deve usar)

| Tarefa | Como usar sem virar dependente |
|--------|--------------------------------|
| Gerar casos de teste iniciais | Revisar, cortar 70%, adicionar regras de negócio reais |
| Esboçar scripts Playwright/Cypress | Validar seletores, estabilidade, manutenção |
| Explicar logs e stack traces | Confirmar na evidência (reproduzir você mesmo) |
| Documentar bugs e relatórios | Editar tom, impacto, prioridade para o negócio |
| Dados de teste sintéticos | Nunca dados reais de cliente; validar LGPD |
| Resumir PRs e diffs | Ler o diff crítico manualmente |
| Converter requisitos em checklist | Questionar ambiguidades com PO/dev |

**Regra de ouro:** IA é **estagiário rápido** — produz volume; o sênior produz **confiança**.

---

## O que a IA faz mal (seu diferencial)

- Saber **o que não testar** por custo vs risco  
- Entender **impacto no usuário farmacêutico** (ou banco, saúde, varejo)  
- Detectar que o teste passa mas o **produto está errado** (falso positivo)  
- Negociar prazo com PM quando cobertura é ilusória  
- Perceber **flaky** que esconde bug intermitente  
- Auditar se automação cobre **caminho crítico de receita/compliance**  
- Cultura de qualidade: post-mortem, blameless, métricas honestas  

---

## Mapa de competências — QA Sênior moderno

### Nível 1 — Fundação (não negociável)

Você precisa dominar na prática, não só no currículo:

| Área | O que “saber” significa |
|------|-------------------------|
| **Test design** | Partição de equivalência, valores limite, tabelas de decisão, fluxos alternativos |
| **Pirâmide de testes** | Unitário (dev) + integração + E2E enxuto; saber por que E2E é caro |
| **API testing** | REST, status HTTP, JSON, auth JWT, contratos; Postman/Insomnia + testes automatizados |
| **SQL básico-intermediário** | Validar dados após ação; investigar bug em fila/estoque/pedido |
| **Git + PR** | Ler diff, comentar risco, reproduzir branch do dev |
| **HTTP e navegador** | Cookies, storage, CORS, cache — essencial para bugs “só em prod” |
| **Escrita de bug** | Passos mínimos, esperado vs atual, ambiente, severidade vs prioridade |

Sem isso, você vira “executor de ferramenta” que a IA substitui primeiro.

---

### Nível 2 — Automação com critério (diferencial forte)

| Tema | Sênior faz assim |
|------|------------------|
| **Por que automatizar** | ROI: regressão frequente, release rápido, dados críticos |
| **O que não automatizar** | UI instável, terceiros, hardware, impressora fiscal |
| **Seletores** | `data-testid`, roles ARIA — não XPath frágil |
| **Page Object / helpers** | Como `autenticar.ts` e `linkMenu()` neste projeto |
| **CI** | Pipeline falha = bloqueio real ou ruído? Ajustar flakes |
| **Paralelismo e isolamento** | Testes independentes, dados próprios, sem ordem fixa |
| **Relatórios** | Screenshot, trace, vídeo — evidência para o time |

Ferramentas: Playwright (web), Rest Assured/Karate (API Java), pytest (se Python). **A ferramenta muda; os princípios não.**

---

### Nível 3 — Sistema e engenharia (o que separa pleno de sênior)

| Tema | Por que importa |
|------|----------------|
| **Arquitetura do produto** | Monolito, microserviços, filas (RabbitMQ), cache — onde o bug mora |
| **Observabilidade** | Logs, métricas, tracing; correlacionar erro de UI com API |
| **Ambientes** | dev / staging / prod; paridade; feature flags |
| **Containers** | Docker, compose — reproduzir bug “só na pipeline” |
| **Segurança básica** | OWASP Top 10, auth, injection, dados sensíveis em teste |
| **Performance smoke** | k6/JMeter leve — não precisa ser perf engineer, mas detectar regressão grosseira |
| **Contratos** | Consumer-driven ou OpenAPI — quebra de API antes do front |

Sênior **não precisa programar como dev staff**, mas **lê código** nas áreas que testa e conversa com dev em igualdade.

---

### Nível 4 — Estratégia e liderança (marca de sênior de verdade)

| Responsabilidade | Entregável |
|------------------|------------|
| **Test plan por risco** | Matriz: impacto × probabilidade × mitigação |
| **Release readiness** | Go/no-go com evidência, não “achismo” |
| **Métricas úteis** | Escape rate, MTTR de bug, tempo de feedback CI — não vanity % cobertura |
| **Mentoria** | Pleno/júnior escrevem testes melhores por sua revisão |
| **Shift-left** | Critérios de aceite testáveis antes do código |
| **Shift-right** | Monitoramento pós-deploy, canary, rollback |
| **Comunicação** | Traduzir risco técnico para PO/negócio em linguagem de impacto |

---

## Pirâmide revisada na era da IA

```text
                    ┌─────────────────────┐
                    │  Exploratório humano │  ← IA não substitui intuição
                    │  + charter + sessões │
                    ├─────────────────────┤
                    │  E2E crítico (pouco) │  ← Playwright: login, $, compliance
                    ├─────────────────────┤
                    │  API / integração    │  ← Maior ROI de automação
                    ├─────────────────────┤
                    │  Unitário (dev)      │  ← Regras de negócio puras
                    └─────────────────────┘
         IA acelera a base da pirâmide ──────────────► você desenha o topo
```

**Erro comum em 2026:** gerar 500 testes E2E com ChatGPT, pipeline de 2 horas, ninguém confia.

**Sênior:** menos testes, mais **significativos**, alinhados ao risco.

---

## Habilidades humanas que valem mais com IA

1. **Pensamento crítico** — “Este teste prova o quê?”  
2. **Curiosidade** — reproduzir antes de abrir ticket  
3. **Comunicação escrita** — bug claro economiza dias  
4. **Empatia com usuário** — farmácia, idoso, operador sob pressão  
5. **Negociação** — “não dá para testar tudo em 2 dias” com alternativas  
6. **Ética** — dados fake, não vazar PHI/PII em prompt de IA  
7. **Aprendizado contínuo** — ferramenta nova a cada 2 anos; princípios estáveis  

---

## O que estudar em 2026 (prioridade prática)

### Curto prazo (3–6 meses)

- [ ] Playwright ou Cypress até automação estável (você já começou aqui)  
- [ ] Testes de API no seu stack (Spring: Rest Assured ou testes `@WebMvcTest` / Testcontainers)  
- [ ] SQL para validação de dados  
- [ ] CI: GitHub Actions ou GitLab — rodar testes em PR  
- [ ] Ler OWASP Top 10 (1ª passagem)  
- [ ] Usar IA para **acelerar**, sempre revisando saída  

### Médio prazo (6–18 meses)

- [ ] Testcontainers ou ambiente dockerizado repetível  
- [ ] Contrato de API (OpenAPI) + testes de regressão  
- [ ] Performance básica (p95 latency, carga leve)  
- [ ] Acessibilidade (axe, Lighthouse) em fluxos críticos  
- [ ] Domínio do negócio (farmacêutico: ANVISA, SNGPC, estoque FEFO — no seu projeto)  
- [ ] Liderar ritual de qualidade (refinamento, trio, demo de risco)  

### Longo prazo (carreira sênior+)

- [ ] Arquitetura de testes em microserviços / event-driven  
- [ ] Quality gates em release (canary, feature flag)  
- [ ] Influência sem autoridade formal — quality champion  
- [ ] Hiring: entrevistar QAs, calibrar níveis  
- [ ] Post-mortem e melhoria de processo  

---

## IA no dia a dia do QA Sênior — fluxo saudável

```text
Requisito novo
    → IA: rascunho de casos + edge cases óbvios
    → Você: cortar, adicionar regra regulatória, priorizar por risco
    → Dev: critérios de aceite no ticket
    → Automação: IA esboça spec; você fixa seletores e asserts de negócio
    → CI: verde com significado
    → Exploratório: 90 min charter no que a IA não viu
    → Release: parecer go/no-go
```

**Anti-padrões:**

- Aceitar caso de teste da IA sem ligar ao requisito  
- Prompt com dados reais de cliente  
- 100% cobertura como meta  
- Ignorar flake “porque passou na segunda”  
- Não ler o código do fix do dev  

---

## Certificações — valem?

| Cert | Utilidade |
|------|-----------|
| **ISTQB** (Foundation / Advanced) | Vocabulário comum com empresas tradicionais; base teórica |
| **Certificações de ferramenta** (Playwright, AWS, etc.) | Nicho; menos que portfólio prático |
| **O que mais contrata** | GitHub com testes reais, bugs bem documentados, experiência em domínio |

Certificação **não substitui** projeto com pipeline e automação mantida por você.

---

## Como se posicionar no mercado brasileiro (2026)

**Título:** QA Engineer / Analista de Qualidade Sênior / SDET (se forte em código)

**Proposta de valor em uma frase:**  
*“Reduzo risco de release em [domínio] com estratégia baseada em evidência, automação enxuta e comunicação clara com produto e engenharia.”*

**Portfólio mínimo credível:**

1. Repo com E2E (como este Farmácia Clark)  
2. 1 exemplo de teste de API documentado  
3. 2–3 bugs “clássicos” que você encontrou (anonimizados) com análise de causa raiz  
4. Texto curto: como você priorizaria testes em um sprint de 2 semanas  

**Salário e nível** variam por região e domínio (fintech, saúde pagam mais por compliance). Sênior = **autonomia + impacto no processo**, não só anos de casa.

---

## Perguntas de entrevista — você deve saber responder

1. Diferença entre severidade e prioridade.  
2. Quando um bug não deve ser corrigido agora.  
3. Como priorizar 200 casos em 3 dias.  
4. O que você automatiza primeiro em um e-commerce (ou farmácia).  
5. Como investiga bug “não reproduz em dev”.  
6. Como lida com flake na pipeline.  
7. O que é shift-left na prática, não no slide.  
8. Como valida que um fix realmente resolveu o problema.  
9. Onde IA entra no seu processo hoje.  
10. Como diz **não** para release sem parecer obstáculo.

---

## Plano de estudo usando este repositório

| Semana | Foco | Material aqui |
|--------|------|----------------|
| 1 | E2E + erros | `00`–`05`, rodar `test:e2e:headed` |
| 2 | API | `curl` auth + ler testes Java `farmacia-api` |
| 3 | Estratégia | Escrever test plan de 1 página para “entrada de estoque” |
| 4 | Automação API | 1 teste Rest Assured para `/api/v1/auth/token` |
| 5 | CI | Entender `.github/workflows/ci.yml` do repo |
| 6 | Exploratório | Sessão 1h charter no PDV sem roteiro |

---

## Mindset final

> **Grande QA Sênior em 2026** não é quem clica mais rápido nem quem gera mais script com IA.  
> É quem **antecipa onde o sistema quebra**, **convence com evidência** e **deixa o time mais rápido com segurança**.

A IA **amplifica** quem já pensa bem.  
Quem só copia e cola, a IA **expõe** na primeira release crítica.

---

## Leitura complementar (conceitos)

- Pirâmide de testes — Mike Cohn  
- Exploratory Testing — Elisabeth Hendrickson / James Bach  
- Agile Testing — Lisa Crispin & Janet Gregory  
- Documentação Playwright: [https://playwright.dev/docs/intro](https://playwright.dev/docs/intro)  
- OWASP Top 10: [https://owasp.org/www-project-top-ten/](https://owasp.org/www-project-top-ten/)  

---

## Índice da pasta de estudos

| Arquivo | Conteúdo |
|---------|----------|
| [00-por-onde-comecar.md](./00-por-onde-comecar.md) | Primeiros passos |
| [01-glossario.md](./01-glossario.md) | Termos |
| [02-fluxo-dos-testes.md](./02-fluxo-dos-testes.md) | Ordem de execução |
| [03-exercicios-praticos.md](./03-exercicios-praticos.md) | Prática |
| [04-duvidas-comuns-erros.md](./04-duvidas-comuns-erros.md) | Erros e causas |
| [05-exemplos-terminal-erros.md](./05-exemplos-terminal-erros.md) | Terminal antes/depois |
| **06-guia-qa-senior-era-ia.md** | Carreira sênior (este doc) |
