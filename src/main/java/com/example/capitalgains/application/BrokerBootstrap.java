package com.example.capitalgains.application;

import com.example.capitalgains.broker.BrokerConfig;
import com.example.capitalgains.broker.BrokerConfigRepository;
import com.example.capitalgains.broker.Role;
import com.example.capitalgains.broker.Ticker;
import com.example.capitalgains.broker.TickerRepository;
import com.example.capitalgains.broker.UserAccountRepository;
import com.example.capitalgains.domain.FeeSchedule;
import com.example.capitalgains.domain.TaxRules;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.security.SecureRandom;
import java.util.List;

/**
 * Makes a fresh broker usable: the default fees and tax rules, a starter ticker
 * catalog, and a first admin. Each step only runs when its data is missing, so
 * restarting over a persisted database changes nothing.
 *
 * <p>The first admin is {@code broker.admin.username} /
 * {@code broker.admin.password}. With no password configured, a random one is
 * generated and logged once: there is never a well-known default password.</p>
 */
@Component
@Profile("!cli")
public class BrokerBootstrap implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(BrokerBootstrap.class);

    /** Starter catalog with simulated reference prices; admins add and edit the rest. */
    private static final List<Ticker> STARTER_TICKERS = List.of(
            new Ticker("PETR4", "Petrobras PN", new BigDecimal("38.50")),
            new Ticker("VALE3", "Vale ON", new BigDecimal("62.10")),
            new Ticker("ITUB4", "Itau Unibanco PN", new BigDecimal("34.20")),
            new Ticker("BBDC4", "Bradesco PN", new BigDecimal("13.80")),
            new Ticker("BBAS3", "Banco do Brasil ON", new BigDecimal("27.90")),
            new Ticker("WEGE3", "WEG ON", new BigDecimal("52.30")),
            new Ticker("ABEV3", "Ambev ON", new BigDecimal("12.40")),
            new Ticker("MGLU3", "Magazine Luiza ON", new BigDecimal("9.75")));

    private final BrokerConfigRepository config;
    private final TickerRepository tickers;
    private final UserAccountRepository users;
    private final AuthService auth;
    private final String adminUsername;
    private final String adminPassword;

    public BrokerBootstrap(BrokerConfigRepository config, TickerRepository tickers, UserAccountRepository users,
                           AuthService auth,
                           @Value("${broker.admin.username:admin}") String adminUsername,
                           @Value("${broker.admin.password:}") String adminPassword) {
        this.config = config;
        this.tickers = tickers;
        this.users = users;
        this.auth = auth;
        this.adminUsername = adminUsername;
        this.adminPassword = adminPassword;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (!config.existsById(BrokerConfig.ID)) {
            config.save(new BrokerConfig(FeeSchedule.fixed("5.00"), TaxRules.CHALLENGE));
        }
        if (tickers.count() == 0) {
            STARTER_TICKERS.forEach(t -> tickers.save(new Ticker(t.getSymbol(), t.getName(), t.getReferencePrice())));
        }
        if (!users.existsByRole(Role.ADMIN)) {
            createFirstAdmin();
        }
    }

    private void createFirstAdmin() {
        var existing = users.findByUsername(adminUsername);
        if (existing.isPresent()) {
            existing.get().changeRole(Role.ADMIN);
            log.warn("No admin existed: promoted existing user '{}' to ADMIN", adminUsername);
            return;
        }
        boolean generated = adminPassword.isBlank();
        String password = generated ? randomPassword() : adminPassword;
        auth.register(adminUsername, password, Role.ADMIN);
        if (generated) {
            log.warn("Created the first admin '{}' with a generated password: {}  "
                    + "(set broker.admin.password to choose one; change it in Settings)", adminUsername, password);
        } else {
            log.info("Created the first admin '{}' from broker.admin.password", adminUsername);
        }
    }

    private static String randomPassword() {
        String alphabet = "abcdefghjkmnpqrstuvwxyzABCDEFGHJKMNPQRSTUVWXYZ23456789";
        SecureRandom random = new SecureRandom();
        StringBuilder password = new StringBuilder();
        for (int i = 0; i < 14; i++) {
            password.append(alphabet.charAt(random.nextInt(alphabet.length())));
        }
        return password.toString();
    }
}
