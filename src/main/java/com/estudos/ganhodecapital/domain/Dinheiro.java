package com.estudos.ganhodecapital.domain;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;

/**
 * Valor monetario em reais. Centraliza a regra de arredondamento do projeto:
 * <b>2 casas decimais, HALF_UP</b>. Todo {@code Dinheiro} ja nasce normalizado
 * nessa escala, entao dois valores "iguais" sao {@code equals} entre si.
 */
public record Dinheiro(BigDecimal valor) implements Comparable<Dinheiro> {

    public static final int ESCALA = 2;
    public static final RoundingMode ARREDONDAMENTO = RoundingMode.HALF_UP;
    public static final Dinheiro ZERO = new Dinheiro(BigDecimal.ZERO);

    public Dinheiro {
        Objects.requireNonNull(valor, "valor");
        valor = valor.setScale(ESCALA, ARREDONDAMENTO);
    }

    public static Dinheiro de(String valor) {
        return new Dinheiro(new BigDecimal(valor));
    }

    public static Dinheiro de(BigDecimal valor) {
        return new Dinheiro(valor);
    }

    public Dinheiro mais(Dinheiro outro) {
        return new Dinheiro(valor.add(outro.valor));
    }

    public Dinheiro menos(Dinheiro outro) {
        return new Dinheiro(valor.subtract(outro.valor));
    }

    public Dinheiro vezes(long quantidade) {
        return new Dinheiro(valor.multiply(BigDecimal.valueOf(quantidade)));
    }

    /** Aplica uma aliquota (ex.: {@code 0.20}) e arredonda o resultado. */
    public Dinheiro aplicarAliquota(BigDecimal aliquota) {
        return new Dinheiro(valor.multiply(aliquota));
    }

    /** Divide por um numero inteiro de acoes, arredondando para 2 casas. */
    public Dinheiro divididoPor(long divisor) {
        return new Dinheiro(valor.divide(BigDecimal.valueOf(divisor), ESCALA, ARREDONDAMENTO));
    }

    public Dinheiro modulo() {
        return new Dinheiro(valor.abs());
    }

    public boolean isNegativo() {
        return valor.signum() < 0;
    }

    public boolean isPositivo() {
        return valor.signum() > 0;
    }

    public boolean isZeroOuNegativo() {
        return valor.signum() <= 0;
    }

    public boolean menorOuIgualA(Dinheiro outro) {
        return compareTo(outro) <= 0;
    }

    @Override
    public int compareTo(Dinheiro outro) {
        return valor.compareTo(outro.valor);
    }

    @Override
    public String toString() {
        return valor.toPlainString();
    }
}
