package com.example.capitalgains.application;

import com.example.capitalgains.broker.BrokerConfig;
import com.example.capitalgains.broker.BrokerConfigRepository;
import com.example.capitalgains.broker.PlacedTrade;
import com.example.capitalgains.broker.PlacedTradeRepository;
import com.example.capitalgains.broker.Ticker;
import com.example.capitalgains.broker.TickerRepository;
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
 * The broker simulator: users buy and sell listed tickers, and every sell is
 * taxed by the same {@link TaxCalculator} the challenge API uses. Each ticker
 * is its own position (its own average price and loss carried forward).
 *
 * <p>The fee and the tax rules come from the admin's {@link BrokerConfig},
 * never from the request, and are stamped on each trade when it is placed.
 * Nothing else is stored: results and taxes are always a replay.</p>
 *
 * <p>This path is synchronous on purpose, like the CLI: a user placing an order
 * expects the result on screen right away, not a queued job to poll.</p>
 */
@Service
public class BrokerService {

    private final UserAccountRepository users;
    private final PlacedTradeRepository trades;
    private final TickerRepository tickers;
    private final BrokerConfigRepository config;
    private final Clock clock;
    private final TaxCalculator calculator = new TaxCalculator();

    public BrokerService(UserAccountRepository users, PlacedTradeRepository trades, TickerRepository tickers,
                         BrokerConfigRepository config, Clock clock) {
        this.users = users;
        this.trades = trades;
        this.tickers = tickers;
        this.config = config;
        this.clock = clock;
    }

    /** What the trade would do, without executing it. */
    @Transactional(readOnly = true)
    public TradeView quote(Long userId, String ticker, Trade trade) {
        users.findById(userId).orElseThrow(NotAuthenticatedException::new);
        String symbol = tradable(ticker, trade);
        return TradeView.of(null, symbol, simulate(userId, symbol, charged(trade)), null);
    }

    /**
     * Executes the trade. Throws {@code SellExceedsPortfolioException} (nothing
     * is stored) if it sells more than the position holds.
     */
    @Transactional
    public TradeView place(Long userId, String ticker, Trade trade) {
        users.lockById(userId).orElseThrow(NotAuthenticatedException::new);
        String symbol = tradable(ticker, trade);
        Trade charged = charged(trade);
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

    /** Tickers open for buying (the order ticket's list). */
    @Transactional(readOnly = true)
    public List<TickerView> activeTickers() {
        return tickers.findByActiveTrueOrderBySymbolAsc().stream().map(TickerView::of).toList();
    }

    /** The fees and tax rules new orders are placed under. */
    @Transactional(readOnly = true)
    public RulesView rules() {
        BrokerConfig current = currentConfig();
        return RulesView.of(current.fees(), current.rules());
    }

    private BrokerConfig currentConfig() {
        return config.findById(BrokerConfig.ID)
                .orElseThrow(() -> new IllegalStateException("broker config missing: BrokerBootstrap did not run"));
    }

    /** Only a listed ticker can be traded; a delisted one can still be sold, never bought. */
    private String tradable(String ticker, Trade trade) {
        String symbol = ticker.trim().toUpperCase(Locale.ROOT);
        Ticker listed = tickers.findById(symbol)
                .orElseThrow(() -> new TickerNotTradableException("ticker not listed: " + symbol));
        if (!listed.isActive() && trade.isBuy()) {
            throw new TickerNotTradableException(symbol + " is delisted: it can be sold but not bought");
        }
        return symbol;
    }

    private Trade charged(Trade trade) {
        BrokerConfig current = currentConfig();
        return trade.withFee(current.fees().feeFor(trade.totalValue())).withRules(current.rules());
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
}
