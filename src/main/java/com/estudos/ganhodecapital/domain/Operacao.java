package com.estudos.ganhodecapital.domain;

import java.math.BigDecimal;
import java.util.Objects;

/**
 * Uma operacao de compra ou venda de acoes.
 *
 * @param tipo          compra ou venda
 * @param custoUnitario preco unitario da acao na operacao (>= 0)
 * @param quantidade    quantidade de acoes negociadas (> 0)
 */
public record Operacao(TipoOperacao tipo, BigDecimal custoUnitario, long quantidade) {

    public Operacao {
        Objects.requireNonNull(tipo, "tipo");
        Objects.requireNonNull(custoUnitario, "custoUnitario");
        if (custoUnitario.signum() < 0) {
            throw new IllegalArgumentException("custoUnitario nao pode ser negativo");
        }
        if (quantidade <= 0) {
            throw new IllegalArgumentException("quantidade deve ser maior que zero");
        }
    }

    public static Operacao compra(String custoUnitario, long quantidade) {
        return new Operacao(TipoOperacao.COMPRA, new BigDecimal(custoUnitario), quantidade);
    }

    public static Operacao venda(String custoUnitario, long quantidade) {
        return new Operacao(TipoOperacao.VENDA, new BigDecimal(custoUnitario), quantidade);
    }

    /** Valor financeiro total da operacao: custo unitario x quantidade. */
    public BigDecimal valorTotal() {
        return custoUnitario.multiply(BigDecimal.valueOf(quantidade));
    }
}
