package com.estudos.ganhodecapital.application;

import com.estudos.ganhodecapital.domain.ResultadoOperacao;

import java.util.List;

/**
 * Resultado de calcular e registrar uma simulacao: o id gerado no historico
 * mais o imposto de cada operacao.
 */
public record SimulacaoCalculada(Long id, List<ResultadoOperacao> resultados) {
}
