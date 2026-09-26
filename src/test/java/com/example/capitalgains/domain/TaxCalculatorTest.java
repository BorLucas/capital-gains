package com.example.capitalgains.domain;

import com.example.capitalgains.domain.error.SellExceedsPortfolioException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TaxCalculatorTest {

    private final TaxCalculator calculator = new TaxCalculator();

    private List<String> taxes(List<Trade> trades) {
        return calculator.calculate(trades).stream()
                .map(r -> r.tax().toBigDecimal().toPlainString())
                .toList();
    }

    @Nested
    @DisplayName("Official challenge cases")
    class OfficialCases {

        @Test
        @DisplayName("Case 1 - sells below the exemption limit")
        void case1() {
            var ops = List.of(
                    Trade.buy("10.00", 100),
                    Trade.sell("15.00", 50),
                    Trade.sell("15.00", 50));

            assertThat(taxes(ops)).containsExactly("0.00", "0.00", "0.00");
        }

        @Test
        @DisplayName("Case 2 - taxed profit followed by a loss")
        void case2() {
            var ops = List.of(
                    Trade.buy("10.00", 10000),
                    Trade.sell("20.00", 5000),
                    Trade.sell("5.00", 5000));

            assertThat(taxes(ops)).containsExactly("0.00", "10000.00", "0.00");
        }

        @Test
        @DisplayName("Case 3 - loss offset against a future profit")
        void case3() {
            var ops = List.of(
                    Trade.buy("10.00", 10000),
                    Trade.sell("5.00", 5000),
                    Trade.sell("20.00", 3000));

            assertThat(taxes(ops)).containsExactly("0.00", "0.00", "1000.00");
        }

        @Test
        @DisplayName("Case 4 - weighted average price, sell without profit")
        void case4() {
            var ops = List.of(
                    Trade.buy("10.00", 10000),
                    Trade.buy("25.00", 5000),
                    Trade.sell("15.00", 10000));

            assertThat(taxes(ops)).containsExactly("0.00", "0.00", "0.00");
        }

        @Test
        @DisplayName("Case 5 - continuation of case 4 with a taxed profit")
        void case5() {
            var ops = List.of(
                    Trade.buy("10.00", 10000),
                    Trade.buy("25.00", 5000),
                    Trade.sell("15.00", 10000),
                    Trade.sell("25.00", 5000));

            assertThat(taxes(ops)).containsExactly("0.00", "0.00", "0.00", "10000.00");
        }

        @Test
        @DisplayName("Case 6 - large loss offset against several profits")
        void case6() {
            var ops = List.of(
                    Trade.buy("10.00", 10000),
                    Trade.sell("2.00", 5000),
                    Trade.sell("20.00", 2000),
                    Trade.sell("20.00", 2000),
                    Trade.sell("25.00", 1000));

            assertThat(taxes(ops)).containsExactly("0.00", "0.00", "0.00", "0.00", "3000.00");
        }

        @Test
        @DisplayName("Case 7 - case 6 followed by a new portfolio and a final exempt sell")
        void case7() {
            var ops = List.of(
                    Trade.buy("10.00", 10000),
                    Trade.sell("2.00", 5000),
                    Trade.sell("20.00", 2000),
                    Trade.sell("20.00", 2000),
                    Trade.sell("25.00", 1000),
                    Trade.buy("20.00", 10000),
                    Trade.sell("15.00", 5000),
                    Trade.sell("30.00", 4350),
                    Trade.sell("30.00", 650));

            assertThat(taxes(ops)).containsExactly(
                    "0.00", "0.00", "0.00", "0.00", "3000.00",
                    "0.00", "0.00", "3700.00", "0.00");
        }

        @Test
        @DisplayName("Case 8 - average price resets when the portfolio empties")
        void case8() {
            var ops = List.of(
                    Trade.buy("10.00", 10000),
                    Trade.sell("50.00", 10000),
                    Trade.buy("20.00", 10000),
                    Trade.sell("50.00", 10000));

            assertThat(taxes(ops)).containsExactly("0.00", "80000.00", "0.00", "60000.00");
        }

        @Test
        @DisplayName("Case 9 - high prices, exemption and loss combined")
        void case9() {
            var ops = List.of(
                    Trade.buy("5000.00", 10),
                    Trade.sell("4000.00", 5),
                    Trade.buy("15000.00", 5),
                    Trade.buy("4000.00", 2),
                    Trade.buy("23000.00", 2),
                    Trade.sell("20000.00", 1),
                    Trade.sell("12000.00", 10),
                    Trade.sell("15000.00", 3));

            assertThat(taxes(ops)).containsExactly(
                    "0.00", "0.00", "0.00", "0.00", "0.00",
                    "0.00", "1000.00", "2400.00");
        }
    }

    @Nested
    @DisplayName("Isolated rules")
    class IsolatedRules {

        @Test
        @DisplayName("a loss on an exempt sell is still accumulated")
        void exemptLossAccumulates() {
            var ops = List.of(
                    Trade.buy("10.00", 10000),
                    Trade.sell("1.00", 1000),   // exempt, loss 9000
                    Trade.sell("30.00", 5000)); // profit 100000, taxable 100000 - 9000

            assertThat(taxes(ops)).containsExactly("0.00", "0.00", "18200.00");
        }

        @Test
        @DisplayName("a profit on an exempt sell does not consume the accumulated loss")
        void exemptProfitDoesNotConsumeLoss() {
            var ops = List.of(
                    Trade.buy("10.00", 10000),
                    Trade.sell("1.00", 1000),
                    Trade.sell("30.00", 500),
                    Trade.sell("30.00", 5000));

            assertThat(taxes(ops)).containsExactly("0.00", "0.00", "0.00", "18200.00");
        }

        @Test
        @DisplayName("does not allow selling more than the portfolio holds")
        void doesNotSellMoreThanHeld() {
            var ops = List.of(
                    Trade.buy("10.00", 100),
                    Trade.sell("20.00", 200));

            assertThatThrownBy(() -> calculator.calculate(ops))
                    .isInstanceOf(SellExceedsPortfolioException.class);
        }

        @Test
        @DisplayName("a null list is rejected immediately")
        void nullList() {
            assertThatThrownBy(() -> calculator.calculate(null))
                    .isInstanceOf(NullPointerException.class);
        }
    }

    @Nested
    @DisplayName("Breakdown")
    class Breakdown {

        @Test
        @DisplayName("an exempt profitable sell reports the profit and no tax")
        void exemptSell() {
            var sell = calculator.breakdown(List.of(
                    Trade.buy("10.00", 10),
                    Trade.sell("50.00", 5))).get(1);

            assertThat(sell.averagePriceBefore()).isEqualTo(Money.of("10.00"));
            assertThat(sell.result()).isEqualTo(Money.of("200.00"));
            assertThat(sell.exempt()).isTrue();
            assertThat(sell.tax()).isEqualTo(Tax.zero());
            assertThat(sell.netResult()).isEqualTo(Money.of("200.00"));
            assertThat(sell.positionAfter()).isEqualTo(5);
            assertThat(sell.averagePriceAfter()).isEqualTo(Money.of("10.00"));
        }

        @Test
        @DisplayName("a taxed sell shows the loss it offset (official case 3)")
        void taxedSellOffsetsLoss() {
            var walk = calculator.breakdown(List.of(
                    Trade.buy("10.00", 10000),
                    Trade.sell("5.00", 5000),
                    Trade.sell("20.00", 3000)));

            assertThat(walk.get(1).result()).isEqualTo(Money.of("-25000.00"));
            assertThat(walk.get(1).accumulatedLossAfter()).isEqualTo(Money.of("25000.00"));

            var taxed = walk.get(2);
            assertThat(taxed.exempt()).isFalse();
            assertThat(taxed.result()).isEqualTo(Money.of("30000.00"));
            assertThat(taxed.accumulatedLossBefore()).isEqualTo(Money.of("25000.00"));
            assertThat(taxed.accumulatedLossAfter()).isEqualTo(Money.ZERO);
            assertThat(taxed.tax().amount()).isEqualTo(Money.of("1000.00"));
            assertThat(taxed.netResult()).isEqualTo(Money.of("29000.00"));
        }

        @Test
        @DisplayName("reports the same taxes as calculate")
        void agreesWithCalculate() {
            var ops = List.of(
                    Trade.buy("10.00", 10000),
                    Trade.sell("1.00", 1000),
                    Trade.sell("30.00", 500),
                    Trade.sell("30.00", 5000));

            assertThat(calculator.breakdown(ops).stream().map(b -> b.tax().toBigDecimal().toPlainString()))
                    .containsExactlyElementsOf(taxes(ops));
        }
    }
}
