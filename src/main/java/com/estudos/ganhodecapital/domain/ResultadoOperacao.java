package com.estudos.ganhodecapital.domain;

import java.util.Objects;

/**
 * Uma operacao e o imposto que ela gerou. Evita que as camadas de cima tenham
 * que "casar" duas listas paralelas (operacoes x impostos) por indice.
 */
public record ResultadoOperacao(Operacao operacao, Imposto imposto) {

    public ResultadoOperacao {
        Objects.requireNonNull(operacao, "operacao");
        Objects.requireNonNull(imposto, "imposto");
    }
}
