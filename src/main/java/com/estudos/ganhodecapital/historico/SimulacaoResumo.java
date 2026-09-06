package com.estudos.ganhodecapital.historico;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Projecao para a listagem do historico: o Spring Data preenche estes campos
 * direto da consulta, sem materializar a entidade nem carregar a colecao de itens.
 */
public interface SimulacaoResumo {

    Long getId();

    Instant getCriadaEm();

    int getQuantidadeOperacoes();

    BigDecimal getImpostoTotal();
}
