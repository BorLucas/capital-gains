package com.estudos.ganhodecapital.cli;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

class CliRunnerTest {

    private final CliRunner cli = new CliRunner(new ObjectMapper());

    private String rodar(String entrada) throws Exception {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        cli.processar(entrada.getBytes(StandardCharsets.UTF_8), new PrintStream(buffer, true, StandardCharsets.UTF_8));
        return buffer.toString(StandardCharsets.UTF_8).trim();
    }

    @Test
    void processaUmaLinha() throws Exception {
        String entrada = "[{\"operation\":\"buy\", \"unit-cost\":10.00, \"quantity\": 10000}, "
                + "{\"operation\":\"sell\", \"unit-cost\":20.00, \"quantity\": 5000}, "
                + "{\"operation\":\"sell\", \"unit-cost\":5.00, \"quantity\": 5000}]";

        assertThat(rodar(entrada))
                .isEqualTo("[{\"tax\":0.00},{\"tax\":10000.00},{\"tax\":0.00}]");
    }

    @Test
    void processaVariasLinhasIndependentes() throws Exception {
        String entrada = """
                [{"operation":"buy", "unit-cost":10.00, "quantity": 100}, {"operation":"sell", "unit-cost":15.00, "quantity": 50}, {"operation":"sell", "unit-cost":15.00, "quantity": 50}]
                [{"operation":"buy", "unit-cost":10.00, "quantity": 10000}, {"operation":"sell", "unit-cost":20.00, "quantity": 5000}, {"operation":"sell", "unit-cost":5.00, "quantity": 5000}]
                """;

        assertThat(rodar(entrada).lines().toList()).containsExactly(
                "[{\"tax\":0.00},{\"tax\":0.00},{\"tax\":0.00}]",
                "[{\"tax\":0.00},{\"tax\":10000.00},{\"tax\":0.00}]");
    }

    @Test
    void ignoraLinhasEmBranco() throws Exception {
        String entrada = "\n\n[{\"operation\":\"buy\", \"unit-cost\":10.00, \"quantity\": 100}]\n\n";
        assertThat(rodar(entrada)).isEqualTo("[{\"tax\":0.00}]");
    }
}
