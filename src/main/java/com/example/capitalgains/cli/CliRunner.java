package com.example.capitalgains.cli;

import com.example.capitalgains.domain.TaxCalculator;
import com.example.capitalgains.domain.Trade;
import com.example.capitalgains.domain.TradeResult;
import com.example.capitalgains.format.TaxJson;
import com.example.capitalgains.format.TradeJson;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * Command-line mode (profile {@code cli}), in the challenge's original format:
 * each line of standard input is a JSON array of trades; for each line it
 * writes to output a JSON array with the tax for each trade.
 *
 * <pre>
 * java -jar target/capital-gains-0.0.1-SNAPSHOT.jar --spring.profiles.active=cli &lt; examples/input.txt
 * </pre>
 *
 * <p>Fail-fast: reads and validates <b>every</b> line, calculates
 * <b>everything</b>, and only then prints. An invalid line aborts before any
 * partial output.</p>
 *
 * <p>Reuses the {@link TaxCalculator} core and the shared format
 * ({@code format.*}); it does not know the web layer and does not write to the
 * history.</p>
 */
@Component
@Profile("cli")
public class CliRunner implements ApplicationRunner {

    private static final String BOM = "﻿";
    private static final TaxCalculator CALCULATOR = new TaxCalculator();

    private final ObjectMapper mapper;

    public CliRunner(ObjectMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public void run(ApplicationArguments args) throws Exception {
        process(System.in, System.out);
    }

    void process(InputStream input, PrintStream output) throws IOException {
        List<List<Trade>> simulations = readSimulations(input);

        List<List<TradeResult>> results = simulations.stream()
                .map(CALCULATOR::calculate)
                .toList();

        for (List<TradeResult> simulation : results) {
            output.println(mapper.writeValueAsString(TaxJson.fromResults(simulation)));
        }
    }

    private List<List<Trade>> readSimulations(InputStream input) throws IOException {
        List<List<Trade>> simulations = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(input, StandardCharsets.UTF_8))) {

            String line;
            boolean first = true;
            while ((line = reader.readLine()) != null) {
                if (first) {
                    line = line.startsWith(BOM) ? line.substring(BOM.length()) : line;
                    first = false;
                }
                if (line.isBlank()) {
                    continue;
                }
                List<TradeJson> request = mapper.readValue(line, new TypeReference<>() {});
                simulations.add(TradeJson.toDomain(request));
            }
        }
        return simulations;
    }
}
