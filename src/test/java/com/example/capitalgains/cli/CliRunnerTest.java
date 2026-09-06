package com.example.capitalgains.cli;

import com.example.capitalgains.domain.error.InvalidTradeException;
import com.example.capitalgains.domain.error.SellExceedsPortfolioException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CliRunnerTest {

    private final CliRunner cli = new CliRunner(new ObjectMapper());

    private String run(String input) throws Exception {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        cli.process(new ByteArrayInputStream(input.getBytes(StandardCharsets.UTF_8)),
                new PrintStream(buffer, true, StandardCharsets.UTF_8));
        return buffer.toString(StandardCharsets.UTF_8).trim();
    }

    @Test
    void processesOneLine() throws Exception {
        String input = "[{\"operation\":\"buy\", \"unit-cost\":10.00, \"quantity\": 10000}, "
                + "{\"operation\":\"sell\", \"unit-cost\":20.00, \"quantity\": 5000}, "
                + "{\"operation\":\"sell\", \"unit-cost\":5.00, \"quantity\": 5000}]";

        assertThat(run(input))
                .isEqualTo("[{\"tax\":0.00},{\"tax\":10000.00},{\"tax\":0.00}]");
    }

    @Test
    void processesSeveralIndependentLines() throws Exception {
        String input = """
                [{"operation":"buy", "unit-cost":10.00, "quantity": 100}, {"operation":"sell", "unit-cost":15.00, "quantity": 50}, {"operation":"sell", "unit-cost":15.00, "quantity": 50}]
                [{"operation":"buy", "unit-cost":10.00, "quantity": 10000}, {"operation":"sell", "unit-cost":20.00, "quantity": 5000}, {"operation":"sell", "unit-cost":5.00, "quantity": 5000}]
                """;

        assertThat(run(input).lines().toList()).containsExactly(
                "[{\"tax\":0.00},{\"tax\":0.00},{\"tax\":0.00}]",
                "[{\"tax\":0.00},{\"tax\":10000.00},{\"tax\":0.00}]");
    }

    @Test
    void ignoresBlankLines() throws Exception {
        String input = "\n\n[{\"operation\":\"buy\", \"unit-cost\":10.00, \"quantity\": 100}]\n\n";
        assertThat(run(input)).isEqualTo("[{\"tax\":0.00}]");
    }

    @Test
    void toleratesBomAtStartOfFile() throws Exception {
        String input = "﻿[{\"operation\":\"buy\", \"unit-cost\":10.00, \"quantity\": 100}]";
        assertThat(run(input)).isEqualTo("[{\"tax\":0.00}]");
    }

    @Test
    void aFailureOnOneLineAbortsBeforeAnyOutput() throws Exception {
        String input = """
                [{"operation":"buy", "unit-cost":10.00, "quantity": 100}]
                [{"operation":"sell", "unit-cost":20.00, "quantity": 999}]
                """;

        assertThatThrownBy(() -> run(input)).isInstanceOf(SellExceedsPortfolioException.class);
    }

    @Test
    void aTradeWithoutQuantityIsRejected() {
        String input = "[{\"operation\":\"buy\", \"unit-cost\":10.00}]";
        assertThatThrownBy(() -> run(input)).isInstanceOf(InvalidTradeException.class);
    }

    @Test
    void aUnitCostWithMoreThanTwoDecimalPlacesIsRejected() {
        String input = "[{\"operation\":\"buy\", \"unit-cost\":10.005, \"quantity\": 100}]";
        assertThatThrownBy(() -> run(input)).isInstanceOf(InvalidTradeException.class);
    }
}
