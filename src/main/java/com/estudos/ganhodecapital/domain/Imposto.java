package com.estudos.ganhodecapital.domain;

import java.math.BigDecimal;
import java.util.Objects;

/**
 * Imposto devido por uma operacao. Tipo nominal proprio (em vez de {@link Dinheiro}
 * cru) para nao confundir "dinheiro" com "imposto a pagar" nas assinaturas.
 */
public record Imposto(Dinheiro valor) {

    public static final Imposto ZERO = new Imposto(Dinheiro.ZERO);

    public Imposto {
        Objects.requireNonNull(valor, "valor");
    }

    public static Imposto zero() {
        return ZERO;
    }

    public static Imposto de(Dinheiro valor) {
        return valor.isPositivo() ? new Imposto(valor) : ZERO;
    }

    public BigDecimal emReais() {
        return valor.valor();
    }
}
