package com.estudos.ganhodecapital.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CalculadoraDeImpostoTest {

    private final CalculadoraDeImposto calculadora = new CalculadoraDeImposto();

    private List<String> impostosComoTexto(List<Operacao> operacoes) {
        return calculadora.calcular(operacoes).stream()
                .map(i -> i.valor().toPlainString())
                .toList();
    }

    @Nested
    @DisplayName("Casos oficiais do desafio")
    class CasosOficiais {

        @Test
        @DisplayName("Caso 1 - vendas abaixo do limite de isencao")
        void caso1() {
            var ops = List.of(
                    Operacao.compra("10.00", 100),
                    Operacao.venda("15.00", 50),
                    Operacao.venda("15.00", 50));

            assertThat(impostosComoTexto(ops)).containsExactly("0.00", "0.00", "0.00");
        }

        @Test
        @DisplayName("Caso 2 - lucro tributado e prejuizo posterior")
        void caso2() {
            var ops = List.of(
                    Operacao.compra("10.00", 10000),
                    Operacao.venda("20.00", 5000),
                    Operacao.venda("5.00", 5000));

            assertThat(impostosComoTexto(ops)).containsExactly("0.00", "10000.00", "0.00");
        }

        @Test
        @DisplayName("Caso 3 - prejuizo abatido de lucro futuro")
        void caso3() {
            var ops = List.of(
                    Operacao.compra("10.00", 10000),
                    Operacao.venda("5.00", 5000),
                    Operacao.venda("20.00", 3000));

            assertThat(impostosComoTexto(ops)).containsExactly("0.00", "0.00", "1000.00");
        }

        @Test
        @DisplayName("Caso 4 - preco medio ponderado, venda sem lucro")
        void caso4() {
            var ops = List.of(
                    Operacao.compra("10.00", 10000),
                    Operacao.compra("25.00", 5000),
                    Operacao.venda("15.00", 10000));

            assertThat(impostosComoTexto(ops)).containsExactly("0.00", "0.00", "0.00");
        }

        @Test
        @DisplayName("Caso 5 - continuacao do caso 4 com lucro tributado")
        void caso5() {
            var ops = List.of(
                    Operacao.compra("10.00", 10000),
                    Operacao.compra("25.00", 5000),
                    Operacao.venda("15.00", 10000),
                    Operacao.venda("25.00", 5000));

            assertThat(impostosComoTexto(ops)).containsExactly("0.00", "0.00", "0.00", "10000.00");
        }

        @Test
        @DisplayName("Caso 6 - prejuizo grande abatido de varios lucros")
        void caso6() {
            var ops = List.of(
                    Operacao.compra("10.00", 10000),
                    Operacao.venda("2.00", 5000),
                    Operacao.venda("20.00", 2000),
                    Operacao.venda("20.00", 2000),
                    Operacao.venda("25.00", 1000));

            assertThat(impostosComoTexto(ops))
                    .containsExactly("0.00", "0.00", "0.00", "0.00", "3000.00");
        }

        @Test
        @DisplayName("Caso 7 - caso 6 seguido de nova carteira e venda isenta final")
        void caso7() {
            var ops = List.of(
                    Operacao.compra("10.00", 10000),
                    Operacao.venda("2.00", 5000),
                    Operacao.venda("20.00", 2000),
                    Operacao.venda("20.00", 2000),
                    Operacao.venda("25.00", 1000),
                    Operacao.compra("20.00", 10000),
                    Operacao.venda("15.00", 5000),
                    Operacao.venda("30.00", 4350),
                    Operacao.venda("30.00", 650));

            assertThat(impostosComoTexto(ops)).containsExactly(
                    "0.00", "0.00", "0.00", "0.00", "3000.00",
                    "0.00", "0.00", "3700.00", "0.00");
        }

        @Test
        @DisplayName("Caso 8 - preco medio zera quando a carteira zera")
        void caso8() {
            var ops = List.of(
                    Operacao.compra("10.00", 10000),
                    Operacao.venda("50.00", 10000),
                    Operacao.compra("20.00", 10000),
                    Operacao.venda("50.00", 10000));

            assertThat(impostosComoTexto(ops))
                    .containsExactly("0.00", "80000.00", "0.00", "60000.00");
        }

        @Test
        @DisplayName("Caso 9 - precos altos, isencao e prejuizo combinados")
        void caso9() {
            var ops = List.of(
                    Operacao.compra("5000.00", 10),
                    Operacao.venda("4000.00", 5),
                    Operacao.compra("15000.00", 5),
                    Operacao.compra("4000.00", 2),
                    Operacao.compra("23000.00", 2),
                    Operacao.venda("20000.00", 1),
                    Operacao.venda("12000.00", 10),
                    Operacao.venda("15000.00", 3));

            assertThat(impostosComoTexto(ops)).containsExactly(
                    "0.00", "0.00", "0.00", "0.00", "0.00",
                    "0.00", "1000.00", "2400.00");
        }
    }

    @Nested
    @DisplayName("Regras isoladas")
    class RegrasIsoladas {

        @Test
        @DisplayName("compra nunca paga imposto")
        void compraNaoPagaImposto() {
            assertThat(impostosComoTexto(List.of(Operacao.compra("100.00", 1000))))
                    .containsExactly("0.00");
        }

        @Test
        @DisplayName("prejuizo em venda isenta ainda e acumulado")
        void prejuizoIsentoAcumula() {
            var ops = List.of(
                    Operacao.compra("10.00", 10000),
                    Operacao.venda("1.00", 1000),   // isenta, prejuizo 9000
                    Operacao.venda("30.00", 5000)); // lucro 100000, tributavel 100000 - 9000

            // (100000 - 9000) * 0.20 = 18200.00
            assertThat(impostosComoTexto(ops)).containsExactly("0.00", "0.00", "18200.00");
        }

        @Test
        @DisplayName("lucro em venda isenta nao consome prejuizo acumulado")
        void lucroIsentoNaoConsomePrejuizo() {
            var ops = List.of(
                    Operacao.compra("10.00", 10000),
                    Operacao.venda("1.00", 1000),    // isenta, prejuizo 9000
                    Operacao.venda("30.00", 500),    // isenta (15000), lucro NAO abate prejuizo
                    Operacao.venda("30.00", 5000));  // lucro 100000, tributavel 100000 - 9000

            assertThat(impostosComoTexto(ops)).containsExactly("0.00", "0.00", "0.00", "18200.00");
        }

        @Test
        @DisplayName("nao permite vender mais do que ha em carteira")
        void naoVendeMaisDoQueTem() {
            var ops = List.of(
                    Operacao.compra("10.00", 100),
                    Operacao.venda("20.00", 200));

            assertThatThrownBy(() -> calculadora.calcular(ops))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("imposto e exatamente 20% do lucro liquido")
        void vintePorCento() {
            var ops = List.of(
                    Operacao.compra("10.00", 10000),
                    Operacao.venda("45.00", 5000)); // lucro (45-10)*5000 = 175000

            var impostos = calculadora.calcular(ops);
            assertThat(impostos.get(1).valor()).isEqualByComparingTo(new BigDecimal("35000.00"));
        }
    }
}
