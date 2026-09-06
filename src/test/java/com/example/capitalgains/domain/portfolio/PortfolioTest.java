package com.example.capitalgains.domain.portfolio;

import com.example.capitalgains.domain.Money;
import com.example.capitalgains.domain.Trade;
import com.example.capitalgains.domain.error.SellExceedsPortfolioException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PortfolioTest {

    private final Portfolio portfolio = new Portfolio();

    private void apply(Trade trade) {
        portfolio.apply(TradeEvent.of(trade));
    }

    @Test
    @DisplayName("starts EMPTY")
    void startsEmpty() {
        assertThat(portfolio.state()).isEqualTo(PortfolioState.EMPTY);
        assertThat(portfolio.quantity()).isZero();
    }

    @Test
    @DisplayName("EMPTY --Buy--> HOLDING sets the average price")
    void buySetsAveragePrice() {
        apply(Trade.buy("10.00", 100));

        assertThat(portfolio.state()).isEqualTo(PortfolioState.HOLDING);
        assertThat(portfolio.quantity()).isEqualTo(100);
        assertThat(portfolio.averagePrice()).isEqualTo(Money.of("10.00"));
    }

    @Test
    @DisplayName("HOLDING --Buy--> HOLDING recalculates the weighted average price")
    void buyRecalculatesAveragePrice() {
        apply(Trade.buy("10.00", 10000));
        apply(Trade.buy("25.00", 5000));

        assertThat(portfolio.averagePrice()).isEqualTo(Money.of("15.00"));
        assertThat(portfolio.quantity()).isEqualTo(15000);
    }

    @Test
    @DisplayName("HOLDING --Full sell--> back to EMPTY and resets the average price")
    void fullSellGoesBackToEmpty() {
        apply(Trade.buy("10.00", 100));
        apply(Trade.sell("50.00", 100));

        assertThat(portfolio.state()).isEqualTo(PortfolioState.EMPTY);
        assertThat(portfolio.quantity()).isZero();
        assertThat(portfolio.averagePrice()).isEqualTo(Money.ZERO);
    }

    @Test
    @DisplayName("HOLDING --Partial sell--> stays HOLDING with the same average price")
    void partialSellStaysHolding() {
        apply(Trade.buy("10.00", 100));
        apply(Trade.sell("50.00", 40));

        assertThat(portfolio.state()).isEqualTo(PortfolioState.HOLDING);
        assertThat(portfolio.quantity()).isEqualTo(60);
        assertThat(portfolio.averagePrice()).isEqualTo(Money.of("10.00"));
    }

    @Test
    @DisplayName("illegal transition: Sell while the portfolio is EMPTY")
    void sellWhileEmpty() {
        assertThatThrownBy(() -> apply(Trade.sell("10.00", 1)))
                .isInstanceOf(SellExceedsPortfolioException.class);
    }

    @Test
    @DisplayName("the guard fails before touching the state")
    void guardDoesNotChangeState() {
        apply(Trade.buy("10.00", 100));

        assertThatThrownBy(() -> apply(Trade.sell("10.00", 500)))
                .isInstanceOf(SellExceedsPortfolioException.class);

        assertThat(portfolio.quantity()).isEqualTo(100);
        assertThat(portfolio.state()).isEqualTo(PortfolioState.HOLDING);
    }

    @Test
    @DisplayName("the loss stays accumulated in the extended state")
    void accumulatedLoss() {
        apply(Trade.buy("10.00", 10000));
        apply(Trade.sell("2.00", 5000)); // loss (10-2)*5000 = 40000

        assertThat(portfolio.accumulatedLoss()).isEqualTo(Money.of("40000.00"));
    }

    @Test
    @DisplayName("a buy always generates zero tax")
    void buyGeneratesZeroTax() {
        var tax = portfolio.apply(TradeEvent.of(Trade.buy("10.00", 100)));
        assertThat(tax.toBigDecimal().signum()).isZero();
    }
}
