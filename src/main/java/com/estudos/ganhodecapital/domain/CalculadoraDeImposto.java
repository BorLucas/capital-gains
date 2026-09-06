package com.estudos.ganhodecapital.domain;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

/**
 * Calcula o imposto devido para uma sequencia de operacoes de acoes,
 * seguindo as regras do desafio "Ganho de Capital":
 *
 * <ol>
 *   <li>Aliquota de 20% sobre o lucro das operacoes de venda.</li>
 *   <li>Compras nao pagam imposto, mas atualizam o preco medio ponderado:
 *       novo = ((qtd_atual * medio_atual) + (qtd_comprada * preco_compra)) / (qtd_atual + qtd_comprada),
 *       arredondado para 2 casas decimais.</li>
 *   <li>Prejuizo e sempre acumulado (inclusive em operacoes isentas) e abatido
 *       de lucros futuros antes de calcular o imposto.</li>
 *   <li>Nao ha imposto quando o valor total da venda (custo unitario x quantidade)
 *       e menor ou igual a R$ 20.000,00. Nessa situacao o prejuizo continua sendo
 *       acumulado, mas um lucro isento nao consome o prejuizo acumulado.</li>
 * </ol>
 *
 * Cada chamada de {@link #calcular(List)} e independente: o estado (preco medio,
 * quantidade em carteira e prejuizo acumulado) vale apenas para aquela lista.
 */
public class CalculadoraDeImposto {

    private static final BigDecimal ALIQUOTA = new BigDecimal("0.20");
    private static final BigDecimal LIMITE_ISENCAO = new BigDecimal("20000");
    private static final int ESCALA_PRECO = 2;

    public List<Imposto> calcular(List<Operacao> operacoes) {
        List<Imposto> impostos = new ArrayList<>(operacoes.size());

        BigDecimal precoMedio = BigDecimal.ZERO;
        long quantidadeEmCarteira = 0L;
        BigDecimal prejuizoAcumulado = BigDecimal.ZERO;

        for (Operacao operacao : operacoes) {
            if (operacao.tipo() == TipoOperacao.COMPRA) {
                precoMedio = novoPrecoMedio(quantidadeEmCarteira, precoMedio, operacao);
                quantidadeEmCarteira += operacao.quantidade();
                impostos.add(Imposto.zero());
                continue;
            }

            // --- VENDA ---
            if (operacao.quantidade() > quantidadeEmCarteira) {
                throw new IllegalArgumentException(
                        "nao e possivel vender mais acoes do que existem em carteira");
            }

            BigDecimal quantidade = BigDecimal.valueOf(operacao.quantidade());
            BigDecimal resultado = operacao.custoUnitario()
                    .subtract(precoMedio)
                    .multiply(quantidade); // > 0 lucro, < 0 prejuizo
            quantidadeEmCarteira -= operacao.quantidade();

            if (resultado.signum() < 0) {
                // Prejuizo: acumula sempre, mesmo em operacao isenta. Nao paga imposto.
                prejuizoAcumulado = prejuizoAcumulado.add(resultado.abs());
                impostos.add(Imposto.zero());
                continue;
            }

            if (operacao.valorTotal().compareTo(LIMITE_ISENCAO) <= 0) {
                // Venda isenta: lucro nao consome prejuizo acumulado e nao gera imposto.
                impostos.add(Imposto.zero());
                continue;
            }

            BigDecimal lucroTributavel = resultado.subtract(prejuizoAcumulado);
            if (lucroTributavel.signum() <= 0) {
                // Prejuizo acumulado ainda maior que o lucro: nada a pagar, sobra prejuizo.
                prejuizoAcumulado = lucroTributavel.negate();
                impostos.add(Imposto.zero());
                continue;
            }

            prejuizoAcumulado = BigDecimal.ZERO;
            BigDecimal imposto = lucroTributavel.multiply(ALIQUOTA).setScale(2, RoundingMode.HALF_UP);
            impostos.add(new Imposto(imposto));
        }

        return impostos;
    }

    private BigDecimal novoPrecoMedio(long quantidadeAtual, BigDecimal precoMedioAtual, Operacao compra) {
        if (quantidadeAtual == 0L) {
            return compra.custoUnitario().setScale(ESCALA_PRECO, RoundingMode.HALF_UP);
        }
        BigDecimal valorEmCarteira = precoMedioAtual.multiply(BigDecimal.valueOf(quantidadeAtual));
        BigDecimal valorComprado = compra.valorTotal();
        BigDecimal quantidadeTotal = BigDecimal.valueOf(quantidadeAtual + compra.quantidade());

        return valorEmCarteira.add(valorComprado)
                .divide(quantidadeTotal, ESCALA_PRECO, RoundingMode.HALF_UP);
    }
}
