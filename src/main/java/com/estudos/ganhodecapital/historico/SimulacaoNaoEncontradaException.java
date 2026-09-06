package com.estudos.ganhodecapital.historico;

import com.estudos.ganhodecapital.domain.erro.GanhoDeCapitalException;

public class SimulacaoNaoEncontradaException extends GanhoDeCapitalException {

    public SimulacaoNaoEncontradaException(Long id) {
        super("simulacao nao encontrada: " + id);
    }
}
