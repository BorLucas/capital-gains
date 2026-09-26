package com.example.capitalgains.web;

import com.example.capitalgains.application.AdminService;
import com.example.capitalgains.application.RulesView;
import com.example.capitalgains.application.TickerView;
import com.example.capitalgains.application.UserView;
import com.example.capitalgains.broker.Role;
import com.example.capitalgains.domain.FeeType;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.List;

/**
 * Admin console: users and roles, the ticker catalog, fees and tax rules.
 * 401 without a session, 403 for a non-admin — enforced by {@link AdminService}
 * on every call, not by this controller.
 */
@RestController
@RequestMapping(value = "/api/admin", produces = MediaType.APPLICATION_JSON_VALUE)
@Tag(name = "Admin", description = "Users, roles, tickers, fees and tax rules (ADMIN only)")
public class AdminController {

    public record NewUser(
            @NotBlank(message = "username is required")
            @Pattern(regexp = "^[A-Za-z0-9._-]{3,30}$",
                    message = "username must be 3-30 letters, digits, dots, dashes or underscores")
            String username,

            @NotBlank(message = "password is required")
            @Size(min = 6, max = 128, message = "password must have at least 6 characters")
            String password,

            @NotNull(message = "role is required (USER or ADMIN)")
            Role role) {
    }

    public record RoleChange(@NotNull(message = "role is required (USER or ADMIN)") Role role) {
    }

    public record NewTicker(
            @NotBlank(message = "symbol is required")
            @Pattern(regexp = "^[A-Za-z0-9]{1,10}$", message = "symbol must be 1-10 letters or digits")
            String symbol,

            @NotBlank(message = "name is required")
            @Size(max = 60, message = "name must have at most 60 characters")
            String name,

            @NotNull(message = "referencePrice is required")
            @PositiveOrZero(message = "referencePrice cannot be negative")
            @Digits(integer = 15, fraction = 2, message = "referencePrice must have at most 2 decimal places")
            BigDecimal referencePrice) {
    }

    public record TickerUpdate(
            @NotBlank(message = "name is required")
            @Size(max = 60, message = "name must have at most 60 characters")
            String name,

            @NotNull(message = "referencePrice is required")
            @PositiveOrZero(message = "referencePrice cannot be negative")
            @Digits(integer = 15, fraction = 2, message = "referencePrice must have at most 2 decimal places")
            BigDecimal referencePrice,

            boolean active) {
    }

    /** Ranges (fee >= 0, percent fee <= 100, tax rate 0-100) are checked by the domain. */
    public record Rules(
            @NotNull(message = "feeType is required (FIXED or PERCENT)")
            FeeType feeType,

            @NotNull(message = "feeValue is required")
            @Digits(integer = 10, fraction = 4, message = "feeValue must have at most 4 decimal places")
            BigDecimal feeValue,

            @NotNull(message = "taxRatePercent is required")
            @Digits(integer = 3, fraction = 2, message = "taxRatePercent must have at most 2 decimal places")
            BigDecimal taxRatePercent,

            @NotNull(message = "exemptionLimit is required")
            @Digits(integer = 15, fraction = 2, message = "exemptionLimit must have at most 2 decimal places")
            BigDecimal exemptionLimit) {
    }

    private final AdminService admin;

    public AdminController(AdminService admin) {
        this.admin = admin;
    }

    @GetMapping("/users")
    @Operation(summary = "All users with their role")
    public List<UserView> users(HttpSession session) {
        return admin.users(Sessions.userId(session));
    }

    @PostMapping(path = "/users", consumes = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Creates a user with a role; 409 if the username is taken")
    public ResponseEntity<UserView> createUser(@RequestBody @Valid NewUser body, HttpSession session) {
        UserView created = admin.createUser(Sessions.userId(session), body.username(), body.password(), body.role());
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PutMapping(path = "/users/{id}/role", consumes = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Changes a user's role; 409 for your own")
    public UserView changeRole(@PathVariable Long id, @RequestBody @Valid RoleChange body, HttpSession session) {
        return admin.changeRole(Sessions.userId(session), id, body.role());
    }

    @GetMapping("/tickers")
    @Operation(summary = "The whole catalog, delisted tickers included")
    public List<TickerView> tickers(HttpSession session) {
        return admin.tickers(Sessions.userId(session));
    }

    @PostMapping(path = "/tickers", consumes = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Lists a new ticker; 409 if it already exists")
    public ResponseEntity<TickerView> listTicker(@RequestBody @Valid NewTicker body, HttpSession session) {
        TickerView listed = admin.listTicker(Sessions.userId(session), body.symbol(), body.name(), body.referencePrice());
        return ResponseEntity.status(HttpStatus.CREATED).body(listed);
    }

    @PutMapping(path = "/tickers/{symbol}", consumes = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Edits a ticker; active=false delists it (holders can still sell)")
    public TickerView updateTicker(@PathVariable String symbol, @RequestBody @Valid TickerUpdate body,
                                   HttpSession session) {
        return admin.updateTicker(Sessions.userId(session), symbol, body.name(), body.referencePrice(), body.active());
    }

    @GetMapping("/rules")
    @Operation(summary = "The broker's fee schedule and tax rules")
    public RulesView rules(HttpSession session) {
        return admin.rules(Sessions.userId(session));
    }

    @PutMapping(path = "/rules", consumes = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Changes fees and tax rules for orders placed from now on")
    public RulesView updateRules(@RequestBody @Valid Rules body, HttpSession session) {
        return admin.updateRules(Sessions.userId(session),
                new RulesView(body.feeType(), body.feeValue(), body.taxRatePercent(), body.exemptionLimit()));
    }
}
