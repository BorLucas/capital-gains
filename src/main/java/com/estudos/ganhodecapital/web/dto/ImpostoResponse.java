package com.estudos.ganhodecapital.web.dto;

import com.estudos.ganhodecapital.domain.Imposto;

import java.math.BigDecimal;

/**
 * Resposta no formato do desafio: <pre>{"tax":0.00}</pre>
 */
public record ImpostoResponse(BigDecimal tax) {

    public static ImpostoResponse de(Imposto imposto) {
        return new ImpostoResponse(imposto.valor());
    }
}
