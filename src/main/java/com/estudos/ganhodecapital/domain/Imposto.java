package com.estudos.ganhodecapital.domain;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;

/**
 * Imposto devido por uma operacao. Sempre com 2 casas decimais.
 */
public record Imposto(BigDecimal valor) {

    private static final Imposto ZERO = new Imposto(BigDecimal.ZERO);

    public Imposto {
        Objects.requireNonNull(valor, "valor");
        valor = valor.setScale(2, RoundingMode.HALF_UP);
    }

    public static Imposto zero() {
        return ZERO;
    }
}
