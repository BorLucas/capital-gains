package com.example.capitalgains.web;

import com.example.capitalgains.application.CalculateTaxService;
import com.example.capitalgains.application.CalculatedSimulation;
import com.example.capitalgains.domain.Trade;
import com.example.capitalgains.format.TaxJson;
import com.example.capitalgains.format.TradeJson;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping(value = "/api/taxes", produces = MediaType.APPLICATION_JSON_VALUE)
@Tag(name = "Taxes", description = "Capital-gains tax calculation")
public class TaxController {

    private final CalculateTaxService service;

    public TaxController(CalculateTaxService service) {
        this.service = service;
    }

    /**
     * One simulation: calculates the tax for each trade and stores it in the
     * history. Responds {@code 201 Created} with {@code Location} pointing to
     * the saved simulation; the body keeps the challenge format.
     */
    @PostMapping(path = "/simulation", consumes = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Calculates the tax for each trade of a simulation and saves it to the history")
    public ResponseEntity<List<TaxJson>> calculateSimulation(
            @RequestBody @NotEmpty(message = "provide at least one trade")
            List<@Valid TradeJson> trades) {

        List<Trade> domain = TradeJson.toDomain(trades);
        CalculatedSimulation calculated = service.calculate(domain);

        return ResponseEntity
                .created(URI.create("/api/simulations/" + calculated.id()))
                .body(TaxJson.fromResults(calculated.results()));
    }

    /**
     * A batch of independent simulations, like the several lines of the
     * challenge's input. Calculates everything before writing; writes
     * everything in one transaction.
     */
    @PostMapping(path = "/batch", consumes = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Calculates several independent simulations at once (one list per challenge line)")
    public List<List<TaxJson>> calculateBatch(
            @RequestBody @NotEmpty(message = "provide at least one simulation")
            List<@NotEmpty List<@Valid TradeJson>> simulations) {

        List<List<Trade>> domain = simulations.stream()
                .map(TradeJson::toDomain)
                .toList();

        return service.calculateBatch(domain).stream()
                .map(calculated -> TaxJson.fromResults(calculated.results()))
                .toList();
    }
}
