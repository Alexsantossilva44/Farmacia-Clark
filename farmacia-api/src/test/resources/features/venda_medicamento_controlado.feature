# ============================================================
# Feature: Venda de Medicamento Controlado
#
# Heurística Júlio de Lima (Mentor Master) — BDD:
# "A feature file é a DOCUMENTAÇÃO VIVA do negócio.
#  Deve ser lida por qualquer stakeholder — desenvolvedor,
#  gerente, farmacêutico — e todos devem entender."
#
# Convenção Given/When/Then:
#  - Given = estado inicial (pré-condição)
#  - When  = ação do usuário/sistema
#  - Then  = resultado esperado (assertion)
#  - And   = extensão da clausula anterior
# ============================================================

# language: pt
@controlado

Funcionalidade: Venda de Medicamento Controlado no PDV
  Como balconista de uma farmácia
  Quero realizar a venda de medicamentos controlados
  Para dispensar com segurança e cumprir a legislação da ANVISA

  Contexto:
    Dado que o PDV "PDV-01" está com o caixa aberto
    E que o farmacêutico "CRF-12345/SP" está disponível para validação
    E que o medicamento controlado "Rivotril 2mg" está cadastrado com nível "CONTROLADO_C1"
    E que há 50 unidades do lote "LOT-2024-RIV" com validade para "2026-12-31"

  # ── CENÁRIO 1: Caminho Feliz ──────────────────────────────────────────────

  Cenário: Venda bem-sucedida com receita Azul válida
    Dado que o cliente "João Silva" apresenta receita Azul número "REC-2024-001"
    E que a receita foi emitida há 5 dias pelo Dr. "Ricardo Gomes" CRM "54321/SP"
    E que a receita foi aprovada pelo farmacêutico "CRF-12345/SP"
    Quando o balconista registra a venda de 1 unidade de "Rivotril 2mg" por R$ 45,90
    E informa o CPF do comprador "123.456.789-01"
    E registra o pagamento de R$ 50,00 em dinheiro
    Então a venda deve ser finalizada com sucesso
    E o cupom fiscal deve ser gerado
    E o estoque do lote "LOT-2024-RIV" deve ser decrementado para 49 unidades
    E o registro SNGPC deve ser enviado de forma assíncrona
    E a receita "REC-2024-001" deve ser marcada como "UTILIZADA" e retida

  # ── CENÁRIO 2: Receita Vencida ────────────────────────────────────────────

  Cenário: Tentativa de venda com receita vencida
    Dado que o cliente "Maria Santos" apresenta receita Azul número "REC-2023-999"
    E que a receita foi emitida há 35 dias (vencida — validade máxima 30 dias)
    Quando o farmacêutico tenta validar a receita "REC-2023-999"
    Então a receita deve ser rejeitada
    E a mensagem de erro deve conter "receita vencida"
    E a venda não deve ser finalizada
    E o estoque não deve ser alterado

  # ── CENÁRIO 3: Tipo de Receita Errado ────────────────────────────────────

  Cenário: Tentativa de venda com receita simples para controlado C1
    Dado que o cliente "Pedro Costa" apresenta receita Simples número "REC-2024-010"
    E que a receita foi emitida há 3 dias e está válida
    Quando o farmacêutico tenta validar a receita "REC-2024-010" para "Rivotril 2mg"
    Então a receita deve ser rejeitada
    E a mensagem de erro deve conter "receita tipo AZUL"
    E deve informar que o medicamento exige "receita Azul"

  # ── CENÁRIO 4: CPF Obrigatório ────────────────────────────────────────────

  Cenário: Tentativa de venda de controlado sem CPF do comprador
    Dado que a receita Azul "REC-2024-002" foi aprovada pelo farmacêutico
    Quando o balconista tenta finalizar a venda sem informar o CPF do comprador
    Então a venda deve ser recusada
    E a mensagem deve informar que "CPF do comprador é obrigatório para medicamento controlado"

  # ── CENÁRIO 5: Preço Acima do PMC ────────────────────────────────────────

  Cenário: Tentativa de venda com preço acima do PMC
    Dado que o "Rivotril 2mg" tem PMC definido em R$ 45,90
    E que a receita Azul "REC-2024-003" foi aprovada
    Quando o balconista tenta registrar a venda de 1 unidade por R$ 60,00
    Então a venda deve ser recusada
    E a mensagem deve informar que "preço excede o PMC de R$ 45,90"

  # ── CENÁRIO 6: Quantidade Acima do Máximo da Portaria 344 ────────────────

  Cenário: Tentativa de prescrever quantidade acima do máximo permitido
    Dado que a "Portaria 344/98" permite no máximo 2 embalagens por receita de "Rivotril 2mg"
    E que o Dr. "Marcos Lima" emitiu receita para 5 embalagens
    Quando o farmacêutico valida a receita
    Então a receita deve ser rejeitada
    E a mensagem deve informar "quantidade solicitada (5) excede o máximo permitido (2)"

  # ── CENÁRIO 7: Estoque Insuficiente ──────────────────────────────────────

  Cenário: Tentativa de venda quando estoque é insuficiente
    Dado que há apenas 2 unidades disponíveis de "Rivotril 2mg"
    E que a receita Azul "REC-2024-004" foi aprovada para 3 unidades
    Quando o balconista tenta finalizar a venda de 3 unidades
    Então a venda deve ser recusada
    E a mensagem deve informar "estoque insuficiente"

  # ── CENÁRIO 8: Seleção FEFO ───────────────────────────────────────────────

  Cenário: Seleção automática do lote pelo critério FEFO
    Dado que há dois lotes disponíveis de "Rivotril 2mg":
      | Lote         | Validade   | Quantidade |
      | LOT-2024-001 | 2025-06-30 | 20         |
      | LOT-2024-002 | 2026-12-31 | 30         |
    E que a receita Azul "REC-2024-005" foi aprovada
    Quando o balconista realiza a venda de 1 unidade
    Então o sistema deve descontar do lote "LOT-2024-001" (mais próximo do vencimento)
    E o lote "LOT-2024-002" não deve ser alterado

  # ── CENÁRIO 9: Alerta de Lote Próximo ao Vencimento ──────────────────────

  Cenário: Aviso ao vender lote próximo ao vencimento
    Dado que o lote "LOT-VENCENDO" de "Rivotril 2mg" vence em 10 dias
    E que a receita Azul "REC-2024-006" foi aprovada
    Quando o balconista finaliza a venda normalmente com CPF "987.654.321-00"
    Então a venda deve ser finalizada com sucesso
    E a resposta deve conter um aviso "lote LOT-VENCENDO vence em 10 dias"
    E o estoque deve ser decrementado normalmente
