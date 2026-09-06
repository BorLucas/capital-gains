package com.estudos.ganhodecapital.web.dto;

import com.estudos.ganhodecapital.domain.Operacao;
import com.estudos.ganhodecapital.domain.TipoOperacao;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;

import java.math.BigDecimal;

/**
 * Operacao no formato JSON do desafio:
 * <pre>{"operation":"buy", "unit-cost":10.00, "quantity": 100}</pre>
 */
public record OperacaoRequest(

        @NotBlank(message = "operation e obrigatorio (buy ou sell)")
        String operation,

        @NotNull(message = "unit-cost e obrigatorio")
        @PositiveOrZero(message = "unit-cost nao pode ser negativo")
        @JsonProperty("unit-cost")
        BigDecimal unitCost,

        @NotNull(message = "quantity e obrigatorio")
        @Positive(message = "quantity deve ser maior que zero")
        Long quantity
) {

    public Operacao paraDominio() {
        TipoOperacao tipo = TipoOperacao.doTexto(operation);
        return new Operacao(tipo, unitCost, quantity);
    }
}
