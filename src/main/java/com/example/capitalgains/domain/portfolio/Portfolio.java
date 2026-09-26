package com.example.capitalgains.domain.portfolio;

import com.example.capitalgains.domain.Money;
import com.example.capitalgains.domain.Tax;
import com.example.capitalgains.domain.Trade;
import com.example.capitalgains.domain.error.SellExceedsPortfolioException;

import java.math.BigDecimal;

/**
 * Aggregate that evolves as trades are applied to it. Modeled as a small state
 * machine: {@link #apply(TradeEvent)} validates the transition (fail-fast) and
 * only then runs the corresponding action.
 *
 * <p>Extended state: {@link #state}, {@link #averagePrice}, {@link #quantity}
 * and {@link #accumulatedLoss}. Each instance represents a single simulation
 * and is not thread-safe.</p>
 *
 * <p>A buy's fee is part of its cost, so it raises the average price. Rules
 * applied on sell transitions:</p>
 * <ol>
 *   <li>result = (sell total - fee) - average price x quantity;</li>
 *   <li>a loss (result &lt; 0) is always accumulated, even on an exempt sell;</li>
 *   <li>an exempt sell (gross total value &le; 20,000, before the fee) pays no tax, and an exempt
 *       profit does not consume the accumulated loss;</li>
 *   <li>outside the exemption, the profit offsets the accumulated loss and 20%
 *       of what remains becomes tax.</li>
 * </ol>
 */
public final class Portfolio {

    private static final BigDecimal TAX_RATE = new BigDecimal("0.20");
    private static final Money EXEMPTION_LIMIT = Money.of("20000");

    private PortfolioState state = PortfolioState.EMPTY;
    private Money averagePrice = Money.ZERO;
    private long quantity = 0L;
    private Money accumulatedLoss = Money.ZERO;

    /**
     * Validates the transition and applies the event, returning the tax it
     * generated ({@link Tax#zero()} for buys and untaxed sells).
     *
     * @throws SellExceedsPortfolioException if the sell is larger than the position
     */
    public Tax apply(TradeEvent event) {
        ensureValidTransition(event);
        return switch (event) {
            case TradeEvent.Buy buy -> buy(buy.trade());
            case TradeEvent.Sell sell -> sell(sell.trade());
        };
    }

    /** Transition guard: blocks the invalid path before any calculation. */
    private void ensureValidTransition(TradeEvent event) {
        if (event instanceof TradeEvent.Sell sell) {
            long wantsToSell = sell.trade().quantity();
            if (state == PortfolioState.EMPTY || wantsToSell > quantity) {
                throw new SellExceedsPortfolioException(wantsToSell, quantity);
            }
        }
    }

    private Tax buy(Trade buy) {
        averagePrice = newAveragePrice(buy);
        quantity += buy.quantity();
        state = PortfolioState.HOLDING;
        return Tax.zero();
    }

    private Tax sell(Trade sell) {
        Money result = resultOf(sell);
        reducePosition(sell.quantity());

        if (result.isNegative()) {
            accumulatedLoss = accumulatedLoss.plus(result.abs());
            return Tax.zero();
        }
        if (isExempt(sell)) {
            return Tax.zero();
        }

        Money taxableProfit = result.minus(accumulatedLoss);
        if (taxableProfit.isZeroOrNegative()) {
            accumulatedLoss = taxableProfit.abs();
            return Tax.zero();
        }

        accumulatedLoss = Money.ZERO;
        return Tax.of(taxableProfit.applyRate(TAX_RATE));
    }

    /** Result of a sell net of its fee, against the current average price; negative is a loss. */
    public Money resultOf(Trade sell) {
        return sell.netValue().minus(averagePrice.times(sell.quantity()));
    }

    /** A sell whose total value is within the exemption limit pays no tax on its profit. */
    public static boolean isExempt(Trade sell) {
        return sell.totalValue().isLessThanOrEqualTo(EXEMPTION_LIMIT);
    }

    private void reducePosition(long quantitySold) {
        quantity -= quantitySold;
        if (quantity == 0L) {
            averagePrice = Money.ZERO;
            state = PortfolioState.EMPTY;
        }
    }

    private Money newAveragePrice(Trade buy) {
        if (quantity == 0L) {
            return buy.netValue().dividedBy(buy.quantity());
        }
        Money valueHeld = averagePrice.times(quantity);
        Money valueBought = buy.netValue();
        long totalQuantity = quantity + buy.quantity();
        return valueHeld.plus(valueBought).dividedBy(totalQuantity);
    }

    public PortfolioState state() {
        return state;
    }

    public long quantity() {
        return quantity;
    }

    public Money averagePrice() {
        return averagePrice;
    }

    public Money accumulatedLoss() {
        return accumulatedLoss;
    }
}
