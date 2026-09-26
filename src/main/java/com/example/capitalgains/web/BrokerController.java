package com.example.capitalgains.web;

import com.example.capitalgains.application.AccountView;
import com.example.capitalgains.application.BrokerService;
import com.example.capitalgains.application.BrokerSettings;
import com.example.capitalgains.application.TradeView;
import com.example.capitalgains.domain.Trade;
import com.example.capitalgains.format.TradeJson;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;

/**
 * The logged-in user's broker: place buys/sells on a ticker, preview what a
 * trade would pay before placing it, and read positions and the trade log.
 * Every route needs a session (401 otherwise).
 */
@RestController
@RequestMapping(value = "/api/broker", produces = MediaType.APPLICATION_JSON_VALUE)
@Tag(name = "Broker", description = "Simulated brokerage account, taxed by the capital-gains rules")
public class BrokerController {

    /** The challenge's trade format plus the ticker it applies to. */
    public record TickerTrade(
            @NotBlank(message = "ticker is required")
            @Pattern(regexp = "^[A-Za-z0-9]{1,10}$", message = "ticker must be 1-10 letters or digits")
            String ticker,

            @NotBlank(message = "operation is required (buy or sell)")
            String operation,

            @NotNull(message = "unit-cost is required")
            @PositiveOrZero(message = "unit-cost cannot be negative")
            @Digits(integer = 15, fraction = 2, message = "unit-cost must have at most 2 decimal places")
            @JsonProperty("unit-cost")
            BigDecimal unitCost,

            @NotNull(message = "quantity is required")
            @Positive(message = "quantity must be greater than zero")
            Long quantity) {

        Trade toDomain() {
            return new TradeJson(operation, unitCost, quantity).toDomain();
        }
    }

    public record Settings(
            @NotNull(message = "brokerageFee is required")
            @DecimalMin(value = "0.00", message = "brokerageFee cannot be negative")
            @DecimalMax(value = "1000.00", message = "brokerageFee cannot exceed 1000.00")
            @Digits(integer = 4, fraction = 2, message = "brokerageFee must have at most 2 decimal places")
            BigDecimal brokerageFee) {
    }

    private final BrokerService broker;

    public BrokerController(BrokerService broker) {
        this.broker = broker;
    }

    @GetMapping("/account")
    @Operation(summary = "Totals, positions per ticker, and the trade log (newest first)")
    public AccountView account(HttpSession session) {
        return broker.account(Sessions.userId(session));
    }

    @GetMapping("/settings")
    @Operation(summary = "The user's broker settings (brokerage fee per order)")
    public BrokerSettings settings(HttpSession session) {
        return broker.settings(Sessions.userId(session));
    }

    @PutMapping(path = "/settings", consumes = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Updates the broker settings; applies to orders placed from now on")
    public BrokerSettings updateSettings(@RequestBody @Valid Settings body, HttpSession session) {
        return broker.updateSettings(Sessions.userId(session), new BrokerSettings(body.brokerageFee()));
    }

    @PostMapping(path = "/quote", consumes = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Previews a trade: result, exemption and tax. Nothing is executed")
    public TradeView quote(@RequestBody @Valid TickerTrade body, HttpSession session) {
        return broker.quote(Sessions.userId(session), body.ticker(), body.toDomain());
    }

    @PostMapping(path = "/trades", consumes = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Executes a trade; 422 when selling more than the position")
    public ResponseEntity<TradeView> place(@RequestBody @Valid TickerTrade body, HttpSession session) {
        TradeView placed = broker.place(Sessions.userId(session), body.ticker(), body.toDomain());
        return ResponseEntity.status(HttpStatus.CREATED).body(placed);
    }
}
