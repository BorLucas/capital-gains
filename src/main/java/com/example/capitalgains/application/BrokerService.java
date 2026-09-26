package com.example.capitalgains.application;

import com.example.capitalgains.broker.PlacedTrade;
import com.example.capitalgains.broker.PlacedTradeRepository;
import com.example.capitalgains.broker.UserAccount;
import com.example.capitalgains.broker.UserAccountRepository;
import com.example.capitalgains.domain.Money;
import com.example.capitalgains.domain.TaxCalculator;
import com.example.capitalgains.domain.Trade;
import com.example.capitalgains.domain.TradeBreakdown;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

/**
 * The broker simulator: users buy and sell tickers, and every sell is taxed by
 * the same {@link TaxCalculator} the challenge API uses. Each ticker is its own
 * position (its own average price and loss carried forward), and nothing but
 * the raw trades (with the fee each was charged) is stored: results and taxes
 * are always a replay. The brokerage fee comes from the user's settings, never
 * from the request.
 *
 * <p>This path is synchronous on purpose, like the CLI: a user placing an order
 * expects the result on screen right away, not a queued job to poll.</p>
 */
@Service
public class BrokerService {

    private final UserAccountRepository users;
    private final PlacedTradeRepository trades;
    private final Clock clock;
    private final TaxCalculator calculator = new TaxCalculator();

    public BrokerService(UserAccountRepository users, PlacedTradeRepository trades, Clock clock) {
        this.users = users;
        this.trades = trades;
        this.clock = clock;
    }

    /** What the trade would do, without executing it. */
    @Transactional(readOnly = true)
    public TradeView quote(Long userId, String ticker, Trade trade) {
        UserAccount user = users.findById(userId).orElseThrow(NotAuthenticatedException::new);
        String symbol = normalize(ticker);
        return TradeView.of(null, symbol, simulate(userId, symbol, withFee(trade, user)), null);
    }

    /**
     * Executes the trade. Throws {@code SellExceedsPortfolioException} (nothing
     * is stored) if it sells more than the position holds.
     */
    @Transactional
    public TradeView place(Long userId, String ticker, Trade trade) {
        UserAccount user = users.lockById(userId).orElseThrow(NotAuthenticatedException::new);
        String symbol = normalize(ticker);
        Trade charged = withFee(trade, user);
        TradeBreakdown breakdown = simulate(userId, symbol, charged);
        PlacedTrade saved = trades.save(PlacedTrade.of(userId, symbol, charged, Instant.now(clock)));
        return TradeView.of(saved.getId(), symbol, breakdown, saved.getExecutedAt());
    }

    @Transactional(readOnly = true)
    public AccountView account(Long userId) {
        Map<String, List<PlacedTrade>> byTicker = trades.findByUserIdOrderByIdAsc(userId).stream()
                .collect(Collectors.groupingBy(PlacedTrade::getTicker, TreeMap::new, Collectors.toList()));

        List<PositionView> positions = new ArrayList<>();
        List<TradeView> log = new ArrayList<>();
        byTicker.forEach((ticker, placed) -> {
            List<TradeBreakdown> walk = calculator.breakdown(placed.stream().map(PlacedTrade::toDomain).toList());
            for (int i = 0; i < placed.size(); i++) {
                log.add(TradeView.of(placed.get(i).getId(), ticker, walk.get(i), placed.get(i).getExecutedAt()));
            }
            positions.add(position(ticker, walk));
        });
        log.sort(Comparator.comparing(TradeView::id).reversed());

        BigDecimal invested = sum(positions.stream().map(PositionView::investedValue).toList());
        BigDecimal realized = sum(positions.stream().map(PositionView::realizedResult).toList());
        BigDecimal tax = sum(positions.stream().map(PositionView::taxPaid).toList());
        return new AccountView(invested, realized, tax, realized.subtract(tax), positions, log);
    }

    @Transactional(readOnly = true)
    public BrokerSettings settings(Long userId) {
        UserAccount user = users.findById(userId).orElseThrow(NotAuthenticatedException::new);
        return new BrokerSettings(user.getBrokerageFee());
    }

    /** Applies to orders placed from now on; past trades keep the fee they were charged. */
    @Transactional
    public BrokerSettings updateSettings(Long userId, BrokerSettings settings) {
        UserAccount user = users.findById(userId).orElseThrow(NotAuthenticatedException::new);
        user.setBrokerageFee(Money.of(settings.brokerageFee()).amount());
        return new BrokerSettings(user.getBrokerageFee());
    }

    private static Trade withFee(Trade trade, UserAccount user) {
        return trade.withFee(Money.of(user.getBrokerageFee()));
    }

    private TradeBreakdown simulate(Long userId, String ticker, Trade trade) {
        List<Trade> sequence = new ArrayList<>(trades.findByUserIdAndTickerOrderByIdAsc(userId, ticker).stream()
                .map(PlacedTrade::toDomain)
                .toList());
        sequence.add(trade);
        List<TradeBreakdown> walk = calculator.breakdown(sequence);
        return walk.get(walk.size() - 1);
    }

    private static PositionView position(String ticker, List<TradeBreakdown> walk) {
        TradeBreakdown last = walk.get(walk.size() - 1);
        Money realized = walk.stream().map(TradeBreakdown::result).reduce(Money.ZERO, Money::plus);
        Money tax = walk.stream().map(b -> b.tax().amount()).reduce(Money.ZERO, Money::plus);
        return new PositionView(
                ticker,
                last.positionAfter(),
                last.averagePriceAfter().amount(),
                last.averagePriceAfter().times(last.positionAfter()).amount(),
                last.accumulatedLossAfter().amount(),
                realized.amount(),
                tax.amount());
    }

    private static BigDecimal sum(List<BigDecimal> values) {
        return values.stream().reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private static String normalize(String ticker) {
        return ticker.trim().toUpperCase(Locale.ROOT);
    }
}
