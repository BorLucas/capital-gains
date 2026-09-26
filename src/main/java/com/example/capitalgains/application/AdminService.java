package com.example.capitalgains.application;

import com.example.capitalgains.broker.BrokerConfig;
import com.example.capitalgains.broker.BrokerConfigRepository;
import com.example.capitalgains.broker.Role;
import com.example.capitalgains.broker.Ticker;
import com.example.capitalgains.broker.TickerRepository;
import com.example.capitalgains.broker.UserAccount;
import com.example.capitalgains.broker.UserAccountRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Locale;

/**
 * Everything only an ADMIN may do: manage users and their roles, the ticker
 * catalog, and the broker-wide fees and tax rules.
 *
 * <p>Every method takes the acting user and checks the role itself, against
 * the database, on every call. The web layer does not have to remember to
 * guard a route, and a demoted admin loses access immediately.</p>
 */
@Service
public class AdminService {

    private final UserAccountRepository users;
    private final TickerRepository tickers;
    private final BrokerConfigRepository config;
    private final AuthService auth;

    public AdminService(UserAccountRepository users, TickerRepository tickers,
                        BrokerConfigRepository config, AuthService auth) {
        this.users = users;
        this.tickers = tickers;
        this.config = config;
        this.auth = auth;
    }

    // ---- users ----

    @Transactional(readOnly = true)
    public List<UserView> users(Long actorId) {
        requireAdmin(actorId);
        return users.findAllByOrderByUsernameAsc().stream().map(UserView::of).toList();
    }

    @Transactional
    public UserView createUser(Long actorId, String username, String password, Role role) {
        requireAdmin(actorId);
        return auth.register(username, password, role);
    }

    @Transactional
    public UserView changeRole(Long actorId, Long userId, Role role) {
        requireAdmin(actorId);
        if (actorId.equals(userId)) {
            throw new OwnRoleChangeException();
        }
        UserAccount user = users.findById(userId).orElseThrow(() -> new UserNotFoundException(userId));
        user.changeRole(role);
        return UserView.of(user);
    }

    // ---- tickers ----

    @Transactional(readOnly = true)
    public List<TickerView> tickers(Long actorId) {
        requireAdmin(actorId);
        return tickers.findAllByOrderBySymbolAsc().stream().map(TickerView::of).toList();
    }

    @Transactional
    public TickerView listTicker(Long actorId, String symbol, String name, BigDecimal referencePrice) {
        requireAdmin(actorId);
        String normalized = symbol.trim().toUpperCase(Locale.ROOT);
        if (tickers.existsById(normalized)) {
            throw new TickerAlreadyListedException(normalized);
        }
        return TickerView.of(tickers.save(new Ticker(normalized, name.trim(), referencePrice)));
    }

    @Transactional
    public TickerView updateTicker(Long actorId, String symbol, String name, BigDecimal referencePrice, boolean active) {
        requireAdmin(actorId);
        String normalized = symbol.trim().toUpperCase(Locale.ROOT);
        Ticker ticker = tickers.findById(normalized)
                .orElseThrow(() -> new TickerNotTradableException("ticker not listed: " + normalized));
        ticker.update(name.trim(), referencePrice, active);
        return TickerView.of(ticker);
    }

    // ---- fees and tax rules ----

    @Transactional(readOnly = true)
    public RulesView rules(Long actorId) {
        requireAdmin(actorId);
        BrokerConfig current = current();
        return RulesView.of(current.fees(), current.rules());
    }

    /** Applies to orders placed from now on; past trades keep the fee and rules they were placed under. */
    @Transactional
    public RulesView updateRules(Long actorId, RulesView rules) {
        requireAdmin(actorId);
        BrokerConfig current = current();
        current.apply(rules.fees(), rules.rules()); // domain constructors validate the ranges
        return RulesView.of(current.fees(), current.rules());
    }

    private BrokerConfig current() {
        return config.findById(BrokerConfig.ID)
                .orElseThrow(() -> new IllegalStateException("broker config missing: BrokerBootstrap did not run"));
    }

    private void requireAdmin(Long actorId) {
        UserAccount actor = users.findById(actorId).orElseThrow(NotAuthenticatedException::new);
        if (!actor.isAdmin()) {
            throw new AdminOnlyException();
        }
    }
}
