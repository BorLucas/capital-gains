package com.estudos.ganhodecapital.formato;

import com.estudos.ganhodecapital.domain.ResultadoOperacao;

import java.math.BigDecimal;
import java.util.List;

/**
 * Saida no formato do desafio: <pre>{"tax":0.00}</pre>
 */
public record ImpostoJson(BigDecimal tax) {

    public static List<ImpostoJson> deResultados(List<ResultadoOperacao> resultados) {
        return resultados.stream()
                .map(r -> new ImpostoJson(r.imposto().emReais()))
                .toList();
    }
}
