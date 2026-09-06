package com.estudos.ganhodecapital.domain;

import com.estudos.ganhodecapital.domain.erro.OperacaoInvalidaException;

import java.math.BigDecimal;
import java.util.Objects;

/**
 * Uma operacao de compra ou venda de acoes. Os invariantes sao verificados no
 * construtor compacto (fail-fast): nenhuma {@code Operacao} invalida chega a
 * existir.
 *
 * @param tipo          compra ou venda
 * @param custoUnitario preco unitario da acao na operacao (>= 0)
 * @param quantidade    quantidade de acoes negociadas (> 0)
 */
public record Operacao(TipoOperacao tipo, Dinheiro custoUnitario, long quantidade) {

    public Operacao {
        Objects.requireNonNull(tipo, "tipo");
        Objects.requireNonNull(custoUnitario, "custoUnitario");
        if (custoUnitario.isNegativo()) {
            throw new OperacaoInvalidaException("unit-cost nao pode ser negativo");
        }
        if (quantidade <= 0) {
            throw new OperacaoInvalidaException("quantity deve ser maior que zero");
        }
    }

    public static Operacao compra(String custoUnitario, long quantidade) {
        return new Operacao(TipoOperacao.COMPRA, Dinheiro.de(custoUnitario), quantidade);
    }

    public static Operacao venda(String custoUnitario, long quantidade) {
        return new Operacao(TipoOperacao.VENDA, Dinheiro.de(custoUnitario), quantidade);
    }

    /** Valor financeiro total da operacao: custo unitario x quantidade. */
    public Dinheiro valorTotal() {
        return custoUnitario.vezes(quantidade);
    }

    public boolean isCompra() {
        return tipo == TipoOperacao.COMPRA;
    }

    public boolean isVenda() {
        return tipo == TipoOperacao.VENDA;
    }
}
