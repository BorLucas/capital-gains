package com.estudos.ganhodecapital.service;

import com.estudos.ganhodecapital.domain.CalculadoraDeImposto;
import com.estudos.ganhodecapital.domain.Imposto;
import com.estudos.ganhodecapital.domain.Operacao;
import com.estudos.ganhodecapital.historico.HistoricoService;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Orquestra o calculo de imposto e o registro no historico.
 * Cada "simulacao" e uma lista independente de operacoes.
 */
@Service
public class ImpostoService {

    private final CalculadoraDeImposto calculadora = new CalculadoraDeImposto();
    private final HistoricoService historico;

    public ImpostoService(HistoricoService historico) {
        this.historico = historico;
    }

    /** Calcula os impostos de uma unica simulacao e guarda no historico. */
    public List<Imposto> calcularSimulacao(List<Operacao> operacoes) {
        List<Imposto> impostos = calculadora.calcular(operacoes);
        historico.registrar(operacoes, impostos);
        return impostos;
    }

    /** Calcula varias simulacoes independentes (formato do desafio: uma lista por linha). */
    public List<List<Imposto>> calcularLote(List<List<Operacao>> simulacoes) {
        return simulacoes.stream()
                .map(this::calcularSimulacao)
                .toList();
    }
}
