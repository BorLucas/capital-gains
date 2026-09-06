package com.example.capitalgains.web;

import com.example.capitalgains.application.OrderQueryService;
import com.example.capitalgains.application.SubmitOrderService;
import com.example.capitalgains.domain.Trade;
import com.example.capitalgains.format.TradeJson;
import com.example.capitalgains.orders.OrderView;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.List;
import java.util.UUID;

/**
 * Asynchronous tax assessment, modeled like placing broker orders: the request
 * is <b>accepted</b> ({@code 202}) and queued, and the client polls the order
 * until it is {@code COMPLETED} (with a link to the stored simulation) or
 * {@code FAILED} (with a reason).
 */
@RestController
@RequestMapping(value = "/api/taxes/orders", produces = MediaType.APPLICATION_JSON_VALUE)
@Tag(name = "Orders", description = "Asynchronous capital-gains tax assessment")
public class OrderController {

    private final SubmitOrderService submit;
    private final OrderQueryService query;

    public OrderController(SubmitOrderService submit, OrderQueryService query) {
        this.submit = submit;
        this.query = query;
    }

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Submits one simulation for assessment; returns 202 with the queued order")
    public ResponseEntity<OrderView> submitOrder(
            @RequestBody @NotEmpty(message = "provide at least one trade")
            List<@Valid TradeJson> trades) {

        UUID id = submit.submit(TradeJson.toDomain(trades));
        return ResponseEntity
                .accepted()
                .location(URI.create("/api/taxes/orders/" + id))
                .body(query.view(id));
    }

    @PostMapping(path = "/batch", consumes = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Submits a basket of simulations at once; returns 202 with every queued order")
    public ResponseEntity<List<OrderView>> submitBatch(
            @RequestBody @NotEmpty(message = "provide at least one simulation")
            List<@NotEmpty List<@Valid TradeJson>> simulations) {

        List<List<Trade>> domain = simulations.stream()
                .map(TradeJson::toDomain)
                .toList();

        List<OrderView> orders = submit.submitBatch(domain).stream()
                .map(query::view)
                .toList();
        return ResponseEntity.accepted().body(orders);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Polls the current state of an order")
    public OrderView get(@PathVariable UUID id) {
        return query.view(id);
    }
}
