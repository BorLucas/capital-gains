package com.estudos.ganhodecapital.historico.dto;

import com.estudos.ganhodecapital.domain.TipoOperacao;
import com.estudos.ganhodecapital.historico.Simulacao;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

/** Detalhe de uma simulacao do historico, com cada operacao e seu imposto. */
public record SimulacaoDetalheResponse(
        Long id,
        Instant criadaEm,
        BigDecimal impostoTotal,
        List<Item> operacoes
) {

    public record Item(
            String operation,
            @JsonProperty("unit-cost") BigDecimal unitCost,
            long quantity,
            BigDecimal tax
    ) {}

    public static SimulacaoDetalheResponse de(Simulacao s) {
        List<Item> itens = s.getItens().stream()
                .map(i -> new Item(
                        i.getTipo() == TipoOperacao.COMPRA ? "buy" : "sell",
                        i.getCustoUnitario(),
                        i.getQuantidade(),
                        i.getImposto()))
                .toList();
        return new SimulacaoDetalheResponse(s.getId(), s.getCriadaEm(), s.getImpostoTotal(), itens);
    }
}
