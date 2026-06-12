package br.com.farmacia.domain.compra.enums;

public enum TipoDivergenciaConferencia {
    ITEM_NAO_PEDIDO,
    QUANTIDADE_EXCEDENTE,
    PRECO_DIFERENTE,
    ITEM_AUSENTE_NA_NOTA // H-06: item do pedido não foi entregue pelo fornecedor
}
