package com.estudos.ganhodecapital.domain.erro;

/**
 * Raiz da hierarquia de erros de negocio do projeto. Toda excecao de dominio
 * herda daqui, o que permite um tratamento uniforme na borda (web/CLI) sem
 * depender de {@link IllegalArgumentException} generica.
 */
public abstract class GanhoDeCapitalException extends RuntimeException {

    protected GanhoDeCapitalException(String mensagem) {
        super(mensagem);
    }
}
