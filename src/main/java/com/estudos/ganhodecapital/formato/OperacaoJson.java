package com.estudos.ganhodecapital.formato;

import com.estudos.ganhodecapital.domain.Dinheiro;
import com.estudos.ganhodecapital.domain.Operacao;
import com.estudos.ganhodecapital.domain.TipoOperacao;
import com.estudos.ganhodecapital.domain.erro.OperacaoInvalidaException;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;

import java.math.BigDecimal;
import java.util.List;

/**
 * Operacao no formato JSON do desafio, compartilhado pela API e pela CLI:
 * <pre>{"operation":"buy", "unit-cost":10.00, "quantity": 100}</pre>
 *
 * <p>As anotacoes de bean validation dao mensagens 400 amigaveis na API. O
 * {@link #paraDominio()} NAO depende delas: valida os proprios invariantes
 * (fail-fast) para que a CLI, que nao passa pelo validador, tambem esteja
 * protegida.</p>
 */
public record OperacaoJson(

        @NotBlank(message = "operation e obrigatorio (buy ou sell)")
        String operation,

        @NotNull(message = "unit-cost e obrigatorio")
        @PositiveOrZero(message = "unit-cost nao pode ser negativo")
        @Digits(integer = 15, fraction = 2, message = "unit-cost deve ter no maximo 2 casas decimais")
        @JsonProperty("unit-cost")
        BigDecimal unitCost,

        @NotNull(message = "quantity e obrigatorio")
        @Positive(message = "quantity deve ser maior que zero")
        Long quantity
) {

    public Operacao paraDominio() {
        if (unitCost == null) {
            throw new OperacaoInvalidaException("unit-cost e obrigatorio");
        }
        if (quantity == null) {
            throw new OperacaoInvalidaException("quantity e obrigatorio");
        }
        if (unitCost.stripTrailingZeros().scale() > Dinheiro.ESCALA) {
            throw new OperacaoInvalidaException("unit-cost deve ter no maximo 2 casas decimais");
        }
        TipoOperacao tipo = TipoOperacao.doCodigo(operation);
        return new Operacao(tipo, Dinheiro.de(unitCost), quantity);
    }

    public static List<Operacao> paraDominio(List<OperacaoJson> operacoes) {
        return operacoes.stream().map(OperacaoJson::paraDominio).toList();
    }
}
