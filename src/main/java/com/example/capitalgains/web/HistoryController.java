package com.example.capitalgains.web;

import com.example.capitalgains.application.HistoryService;
import com.example.capitalgains.history.SimulationDetail;
import com.example.capitalgains.history.SimulationSummary;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping(value = "/api/simulations", produces = MediaType.APPLICATION_JSON_VALUE)
@Tag(name = "History", description = "Lookup of already-calculated simulations")
public class HistoryController {

    private final HistoryService service;

    public HistoryController(HistoryService service) {
        this.service = service;
    }

    @GetMapping
    @Operation(summary = "Lists the saved simulations, newest to oldest")
    public List<SimulationSummary> list(
            @ParameterObject @PageableDefault(size = 20) Pageable page) {
        return service.list(page);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Details a simulation: each trade and the tax it generated")
    public SimulationDetail get(@PathVariable Long id) {
        return service.findDetail(id);
    }
}
