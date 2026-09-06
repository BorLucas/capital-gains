package com.estudos.ganhodecapital.application;

import com.estudos.ganhodecapital.domain.CalculadoraDeImposto;
import com.estudos.ganhodecapital.domain.Operacao;
import com.estudos.ganhodecapital.domain.ResultadoOperacao;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.IntStream;

/**
 * Caso de uso: calcular o imposto de uma ou varias simulacoes e registra-las no
 * historico.
 *
 * <p>Fail-fast no lote: <b>todas</b> as simulacoes sao calculadas (e podem
 * falhar) antes de qualquer gravacao, e a gravacao acontece numa unica
 * transacao. Nunca sobra uma simulacao persistida pela metade.</p>
 */
@Service
public class CalcularImpostoService {

    private static final CalculadoraDeImposto CALCULADORA = new CalculadoraDeImposto();

    private final HistoricoService historico;

    public CalcularImpostoService(HistoricoService historico) {
        this.historico = historico;
    }

    public SimulacaoCalculada calcular(List<Operacao> operacoes) {
        List<ResultadoOperacao> resultados = CALCULADORA.calcular(operacoes);
        Long id = historico.registrar(resultados).getId();
        return new SimulacaoCalculada(id, resultados);
    }

    public List<SimulacaoCalculada> calcularLote(List<List<Operacao>> simulacoes) {
        List<List<ResultadoOperacao>> resultados = simulacoes.stream()
                .map(CALCULADORA::calcular)
                .toList();

        List<Long> ids = historico.registrarTodas(resultados);

        return IntStream.range(0, resultados.size())
                .mapToObj(i -> new SimulacaoCalculada(ids.get(i), resultados.get(i)))
                .toList();
    }
}
