package com.estudos.ganhodecapital.domain.carteira;

import com.estudos.ganhodecapital.domain.Dinheiro;
import com.estudos.ganhodecapital.domain.Imposto;
import com.estudos.ganhodecapital.domain.Operacao;
import com.estudos.ganhodecapital.domain.erro.VendaSuperaCarteiraException;

import java.math.BigDecimal;

/**
 * Agregado que evolui conforme as operacoes vao sendo aplicadas. Modelado como
 * uma pequena maquina de estados: {@link #aplicar(EventoOperacao)} valida a
 * transicao (fail-fast) e so entao executa a acao correspondente.
 *
 * <p>Estado interno (extended state): {@link #estado}, {@link #precoMedio},
 * {@link #quantidade} e {@link #prejuizoAcumulado}. Cada instancia representa
 * uma unica simulacao e nao e thread-safe.</p>
 *
 * <p>Regras aplicadas nas transicoes de venda:</p>
 * <ol>
 *   <li>resultado = (preco de venda - preco medio) x quantidade;</li>
 *   <li>prejuizo (resultado &lt; 0) e sempre acumulado, mesmo em venda isenta;</li>
 *   <li>venda isenta (valor total &le; R$ 20.000) nao paga imposto e um lucro
 *       isento nao consome o prejuizo acumulado;</li>
 *   <li>fora da isencao, o lucro abate o prejuizo acumulado e 20% do que sobrar
 *       vira imposto.</li>
 * </ol>
 */
public final class Carteira {

    private static final BigDecimal ALIQUOTA = new BigDecimal("0.20");
    private static final Dinheiro LIMITE_ISENCAO = Dinheiro.de("20000");

    private EstadoCarteira estado = EstadoCarteira.VAZIA;
    private Dinheiro precoMedio = Dinheiro.ZERO;
    private long quantidade = 0L;
    private Dinheiro prejuizoAcumulado = Dinheiro.ZERO;

    /**
     * Valida a transicao e aplica o evento, devolvendo o imposto gerado
     * ({@link Imposto#zero()} para compras e vendas nao tributadas).
     *
     * @throws VendaSuperaCarteiraException se a venda for maior que a posicao
     */
    public Imposto aplicar(EventoOperacao evento) {
        garantirTransicaoValida(evento);
        return switch (evento) {
            case EventoOperacao.Comprar compra -> comprar(compra.operacao());
            case EventoOperacao.Vender venda -> vender(venda.operacao());
        };
    }

    /** Guarda de transicao: barra o caminho invalido antes de qualquer calculo. */
    private void garantirTransicaoValida(EventoOperacao evento) {
        if (evento instanceof EventoOperacao.Vender venda) {
            long querVender = venda.operacao().quantidade();
            if (estado == EstadoCarteira.VAZIA || querVender > quantidade) {
                throw new VendaSuperaCarteiraException(querVender, quantidade);
            }
        }
    }

    private Imposto comprar(Operacao compra) {
        precoMedio = novoPrecoMedio(compra);
        quantidade += compra.quantidade();
        estado = EstadoCarteira.COMPRADA;
        return Imposto.zero();
    }

    private Imposto vender(Operacao venda) {
        Dinheiro resultado = venda.custoUnitario().menos(precoMedio).vezes(venda.quantidade());
        reduzirPosicao(venda.quantidade());

        if (resultado.isNegativo()) {
            prejuizoAcumulado = prejuizoAcumulado.mais(resultado.modulo());
            return Imposto.zero();
        }
        if (venda.valorTotal().menorOuIgualA(LIMITE_ISENCAO)) {
            return Imposto.zero();
        }

        Dinheiro lucroTributavel = resultado.menos(prejuizoAcumulado);
        if (lucroTributavel.isZeroOuNegativo()) {
            prejuizoAcumulado = lucroTributavel.modulo();
            return Imposto.zero();
        }

        prejuizoAcumulado = Dinheiro.ZERO;
        return Imposto.de(lucroTributavel.aplicarAliquota(ALIQUOTA));
    }

    private void reduzirPosicao(long quantidadeVendida) {
        quantidade -= quantidadeVendida;
        if (quantidade == 0L) {
            precoMedio = Dinheiro.ZERO;
            estado = EstadoCarteira.VAZIA;
        }
    }

    private Dinheiro novoPrecoMedio(Operacao compra) {
        if (quantidade == 0L) {
            return compra.custoUnitario();
        }
        Dinheiro valorEmCarteira = precoMedio.vezes(quantidade);
        Dinheiro valorComprado = compra.valorTotal();
        long quantidadeTotal = quantidade + compra.quantidade();
        return valorEmCarteira.mais(valorComprado).divididoPor(quantidadeTotal);
    }

    public EstadoCarteira estado() {
        return estado;
    }

    public long quantidade() {
        return quantidade;
    }

    public Dinheiro precoMedio() {
        return precoMedio;
    }

    public Dinheiro prejuizoAcumulado() {
        return prejuizoAcumulado;
    }
}
