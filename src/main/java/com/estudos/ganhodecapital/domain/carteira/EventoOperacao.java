package com.estudos.ganhodecapital.domain.carteira;

import com.estudos.ganhodecapital.domain.Operacao;

/**
 * Evento que dispara uma transicao na {@link Carteira}. Selado: a {@code Carteira}
 * trata todos os casos num {@code switch} exaustivo, sem {@code default}.
 */
public sealed interface EventoOperacao {

    Operacao operacao();

    record Comprar(Operacao operacao) implements EventoOperacao {}

    record Vender(Operacao operacao) implements EventoOperacao {}

    static EventoOperacao de(Operacao operacao) {
        return operacao.isCompra() ? new Comprar(operacao) : new Vender(operacao);
    }
}
