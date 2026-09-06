package com.estudos.ganhodecapital.domain;

/**
 * Tipo de operacao no mercado de acoes.
 * No JSON de entrada do desafio os valores sao "buy" e "sell".
 */
public enum TipoOperacao {
    COMPRA,
    VENDA;

    public static TipoOperacao doTexto(String valor) {
        if (valor == null) {
            throw new IllegalArgumentException("operacao nao informada");
        }
        return switch (valor.trim().toLowerCase()) {
            case "buy", "compra" -> COMPRA;
            case "sell", "venda" -> VENDA;
            default -> throw new IllegalArgumentException("operacao invalida: " + valor);
        };
    }
}
