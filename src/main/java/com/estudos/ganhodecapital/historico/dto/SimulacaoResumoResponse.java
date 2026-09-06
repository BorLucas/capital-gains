package com.estudos.ganhodecapital.historico.dto;

import com.estudos.ganhodecapital.historico.Simulacao;

import java.math.BigDecimal;
import java.time.Instant;

/** Item da listagem do historico. */
public record SimulacaoResumoResponse(
        Long id,
        Instant criadaEm,
        int quantidadeOperacoes,
        BigDecimal impostoTotal
) {

    public static SimulacaoResumoResponse de(Simulacao s) {
        return new SimulacaoResumoResponse(s.getId(), s.getCriadaEm(), s.getItens().size(), s.getImpostoTotal());
    }
}
