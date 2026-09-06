package com.estudos.ganhodecapital.historico;

public class SimulacaoNaoEncontradaException extends RuntimeException {

    public SimulacaoNaoEncontradaException(Long id) {
        super("simulacao nao encontrada: " + id);
    }
}
