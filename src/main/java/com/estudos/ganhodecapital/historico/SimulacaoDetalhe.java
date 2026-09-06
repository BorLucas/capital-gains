package com.estudos.ganhodecapital.historico;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

/**
 * Read model do detalhe de uma simulacao: cada operacao no formato do desafio,
 * junto do imposto que gerou. Montado dentro da transacao de leitura, para a
 * borda web nunca tocar na entidade JPA diretamente.
 */
public record SimulacaoDetalhe(
        Long id,
        Instant criadaEm,
        BigDecimal impostoTotal,
        List<Operacao> operacoes
) {

    public record Operacao(
            String operation,
            @JsonProperty("unit-cost") BigDecimal unitCost,
            long quantity,
            BigDecimal tax
    ) {}

    public static SimulacaoDetalhe de(Simulacao simulacao) {
        List<Operacao> operacoes = simulacao.getItens().stream()
                .map(item -> new Operacao(
                        item.getTipo().codigo(),
                        item.getCustoUnitario(),
                        item.getQuantidade(),
                        item.getImposto()))
                .toList();
        return new SimulacaoDetalhe(
                simulacao.getId(),
                simulacao.getCriadaEm(),
                simulacao.getImpostoTotal(),
                operacoes);
    }
}
