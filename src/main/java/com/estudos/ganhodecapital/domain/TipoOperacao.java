package com.estudos.ganhodecapital.domain;

import com.estudos.ganhodecapital.domain.erro.OperacaoInvalidaException;

/**
 * Tipo de operacao no mercado de acoes. O codigo ({@code "buy"} / {@code "sell"})
 * e o formato usado na entrada e na saida do desafio; a conversao nas duas
 * direcoes mora aqui, e nao espalhada pelos DTOs.
 */
public enum TipoOperacao {

    COMPRA("buy"),
    VENDA("sell");

    private final String codigo;

    TipoOperacao(String codigo) {
        this.codigo = codigo;
    }

    public String codigo() {
        return codigo;
    }

    public static TipoOperacao doCodigo(String codigo) {
        if (codigo == null) {
            throw new OperacaoInvalidaException("operation e obrigatorio (buy ou sell)");
        }
        return switch (codigo.trim().toLowerCase()) {
            case "buy", "compra" -> COMPRA;
            case "sell", "venda" -> VENDA;
            default -> throw new OperacaoInvalidaException("operation invalido: " + codigo);
        };
    }
}
