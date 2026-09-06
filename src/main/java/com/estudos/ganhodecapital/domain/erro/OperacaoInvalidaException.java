package com.estudos.ganhodecapital.domain.erro;

/**
 * Dados de uma operacao estao ausentes ou fora do dominio permitido
 * (ex.: quantidade nula, custo negativo, tipo desconhecido).
 */
public class OperacaoInvalidaException extends GanhoDeCapitalException {

    public OperacaoInvalidaException(String mensagem) {
        super(mensagem);
    }
}
