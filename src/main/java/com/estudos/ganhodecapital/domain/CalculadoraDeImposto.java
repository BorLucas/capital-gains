package com.estudos.ganhodecapital.domain;

import com.estudos.ganhodecapital.domain.carteira.Carteira;
import com.estudos.ganhodecapital.domain.carteira.EventoOperacao;

import java.util.List;
import java.util.Objects;

/**
 * Calcula o imposto devido para uma sequencia de operacoes de acoes, seguindo as
 * regras do desafio "Ganho de Capital".
 *
 * <p>A logica de estado (preco medio, posicao e prejuizo acumulado) e as regras
 * de tributacao vivem na {@link Carteira}. Esta classe apenas percorre a lista,
 * transforma cada {@link Operacao} num {@link EventoOperacao} e coleta o
 * resultado. Sem estado proprio: uma unica instancia pode ser reutilizada.</p>
 */
public class CalculadoraDeImposto {

    public List<ResultadoOperacao> calcular(List<Operacao> operacoes) {
        Objects.requireNonNull(operacoes, "operacoes");

        Carteira carteira = new Carteira();
        return operacoes.stream()
                .map(operacao -> {
                    Objects.requireNonNull(operacao, "operacao");
                    Imposto imposto = carteira.aplicar(EventoOperacao.de(operacao));
                    return new ResultadoOperacao(operacao, imposto);
                })
                .toList();
    }
}
