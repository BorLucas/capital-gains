package com.estudos.ganhodecapital.web;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class ImpostoControllerTest {

    @Autowired
    MockMvc mockMvc;

    @Test
    void calculaUmaSimulacao() throws Exception {
        String body = """
                [
                  {"operation":"buy",  "unit-cost":10.00, "quantity":10000},
                  {"operation":"sell", "unit-cost":20.00, "quantity":5000},
                  {"operation":"sell", "unit-cost":5.00,  "quantity":5000}
                ]
                """;

        mockMvc.perform(post("/api/impostos/simulacao").contentType("application/json").content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].tax").value(0.00))
                .andExpect(jsonPath("$[1].tax").value(10000.00))
                .andExpect(jsonPath("$[2].tax").value(0.00));
    }

    @Test
    void calculaLoteDeSimulacoes() throws Exception {
        String body = """
                [
                  [ {"operation":"buy","unit-cost":10.00,"quantity":100},
                    {"operation":"sell","unit-cost":15.00,"quantity":50},
                    {"operation":"sell","unit-cost":15.00,"quantity":50} ],
                  [ {"operation":"buy","unit-cost":10.00,"quantity":10000},
                    {"operation":"sell","unit-cost":20.00,"quantity":5000},
                    {"operation":"sell","unit-cost":5.00,"quantity":5000} ]
                ]
                """;

        mockMvc.perform(post("/api/impostos/lote").contentType("application/json").content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0][1].tax").value(0.00))
                .andExpect(jsonPath("$[1][1].tax").value(10000.00));
    }

    @Test
    void simulacaoCalculadaFicaNoHistorico() throws Exception {
        String body = """
                [ {"operation":"buy","unit-cost":10.00,"quantity":10000},
                  {"operation":"sell","unit-cost":45.00,"quantity":5000} ]
                """;

        mockMvc.perform(post("/api/impostos/simulacao").contentType("application/json").content(body))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/simulacoes"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").exists())
                .andExpect(jsonPath("$[0].impostoTotal").value(35000.00));
    }

    @Test
    void detalheDeSimulacaoInexistenteRetorna404() throws Exception {
        mockMvc.perform(get("/api/simulacoes/999999"))
                .andExpect(status().isNotFound());
    }

    @Test
    void rejeitaOperacaoInvalida() throws Exception {
        String body = """
                [ {"operation":"transferir", "unit-cost":10.00, "quantity":100} ]
                """;

        mockMvc.perform(post("/api/impostos/simulacao").contentType("application/json").content(body))
                .andExpect(status().isBadRequest());
    }

    @Test
    void rejeitaQuantidadeNegativa() throws Exception {
        String body = """
                [ {"operation":"buy", "unit-cost":10.00, "quantity":-5} ]
                """;

        mockMvc.perform(post("/api/impostos/simulacao").contentType("application/json").content(body))
                .andExpect(status().isBadRequest());
    }
}
