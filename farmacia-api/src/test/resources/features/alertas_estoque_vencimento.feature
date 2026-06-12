# language: pt
@alertas
Funcionalidade: Alertas Automáticos de Estoque e Vencimento
  Como gerente de farmácia
  Quero ser alertado automaticamente sobre problemas de estoque
  Para garantir que nunca vendamos medicamentos vencidos ou fiquemos sem estoque

  # ── CENÁRIO 1: Lote Vencido ───────────────────────────────────────────────

  Cenário: Sistema bloqueia automaticamente lotes vencidos às 00:30
    Dado que existe o lote "LOT-VENCIDO-001" do medicamento "Amoxicilina 500mg"
    E que esse lote tinha validade para ontem e tinha 40 unidades disponíveis
    Quando o scheduler de verificação de vencimentos é executado
    Então o lote "LOT-VENCIDO-001" deve ter status alterado para "VENCIDO"
    E as 40 unidades devem ser removidas do saldo consolidado
    E um alerta do tipo "LOTE_VENCIDO" deve ser gerado para o gerente
    E o lote não deve mais aparecer como disponível para dispensação

  # ── CENÁRIO 2: Alerta Crítico (30 dias) ──────────────────────────────────

  Cenário: Alerta crítico para lote que vence em menos de 30 dias
    Dado que o lote "LOT-CRITICO-001" de "Rivotril 2mg" vence em 15 dias
    E que não existe alerta aberto para esse lote
    Quando o scheduler de alertas de vencimento é executado
    Então deve ser gerado um alerta com urgência "CRÍTICO" para "LOT-CRITICO-001"
    E o alerta deve estar com status "ABERTO"
    E o alerta deve estar não lido

  # ── CENÁRIO 3: Alerta Atenção (60 dias) ──────────────────────────────────

  Cenário: Alerta de atenção para lote que vence entre 31 e 60 dias
    Dado que o lote "LOT-ATENCAO-001" vence em 45 dias
    E que não existe alerta aberto para esse lote
    Quando o scheduler de alertas de vencimento é executado
    Então deve ser gerado um alerta com urgência "ATENÇÃO"

  # ── CENÁRIO 4: Sem Duplicidade de Alertas ────────────────────────────────

  Cenário: Não gerar alerta duplicado para lote com alerta já aberto
    Dado que o lote "LOT-CRITICO-001" já tem um alerta "VENCIMENTO_PROXIMO" em aberto
    Quando o scheduler de alertas de vencimento é executado novamente
    Então nenhum novo alerta deve ser criado para "LOT-CRITICO-001"

  # ── CENÁRIO 5: Estoque Mínimo ─────────────────────────────────────────────

  Cenário: Alerta quando estoque cai abaixo do mínimo configurado
    Dado que "Dipirona 500mg" tem estoque mínimo configurado como 10 unidades
    E que o estoque atual é de 3 unidades
    E que não existe alerta de estoque mínimo aberto para "Dipirona 500mg"
    Quando o scheduler de estoque mínimo é executado
    Então deve ser gerado um alerta do tipo "ESTOQUE_MINIMO"
    E a mensagem do alerta deve informar "3 unidades (mínimo: 10)"

  # ── CENÁRIO 6: Estoque Zerado ─────────────────────────────────────────────

  Cenário: Alerta urgente quando medicamento fica sem estoque
    Dado que o estoque de "Morfina 10mg" está zerado
    E que não existe alerta de estoque zerado aberto
    Quando o scheduler de estoque zerado é executado
    Então deve ser gerado um alerta do tipo "ESTOQUE_ZERADO"
    E a mensagem deve conter "ESTOQUE ZERADO"
