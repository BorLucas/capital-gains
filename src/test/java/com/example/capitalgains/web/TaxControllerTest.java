package com.example.capitalgains.web;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class TaxControllerTest {

    @Autowired
    MockMvc mockMvc;

    @Test
    void calculatesSimulationAndReturns201WithLocation() throws Exception {
        String body = """
                [
                  {"operation":"buy",  "unit-cost":10.00, "quantity":10000},
                  {"operation":"sell", "unit-cost":20.00, "quantity":5000},
                  {"operation":"sell", "unit-cost":5.00,  "quantity":5000}
                ]
                """;

        mockMvc.perform(post("/api/taxes/simulation").contentType("application/json").content(body))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", org.hamcrest.Matchers.startsWith("/api/simulations/")))
                .andExpect(jsonPath("$[0].tax").value(0.00))
                .andExpect(jsonPath("$[1].tax").value(10000.00))
                .andExpect(jsonPath("$[2].tax").value(0.00));
    }

    @Test
    void calculatesABatchOfSimulations() throws Exception {
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

        mockMvc.perform(post("/api/taxes/batch").contentType("application/json").content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0][1].tax").value(0.00))
                .andExpect(jsonPath("$[1][1].tax").value(10000.00));
    }

    @Test
    void aCalculatedSimulationStaysInTheHistory() throws Exception {
        String body = """
                [ {"operation":"buy","unit-cost":10.00,"quantity":10000},
                  {"operation":"sell","unit-cost":45.00,"quantity":5000} ]
                """;

        MvcResult result = mockMvc.perform(post("/api/taxes/simulation")
                        .contentType("application/json").content(body))
                .andExpect(status().isCreated())
                .andReturn();

        String location = result.getResponse().getHeader("Location");
        assertThat(location).isNotNull();

        mockMvc.perform(get(location))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalTax").value(35000.00))
                .andExpect(jsonPath("$.trades[1].tax").value(35000.00))
                .andExpect(jsonPath("$.trades[1]['unit-cost']").value(45.00));
    }

    @Test
    void aSellLargerThanThePortfolioReturns422() throws Exception {
        String body = """
                [ {"operation":"buy","unit-cost":10.00,"quantity":100},
                  {"operation":"sell","unit-cost":20.00,"quantity":200} ]
                """;

        mockMvc.perform(post("/api/taxes/simulation").contentType("application/json").content(body))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    void detailOfAMissingSimulationReturns404() throws Exception {
        mockMvc.perform(get("/api/simulations/999999"))
                .andExpect(status().isNotFound());
    }

    @Test
    void anUnknownOperationReturns400() throws Exception {
        String body = """
                [ {"operation":"transfer", "unit-cost":10.00, "quantity":100} ]
                """;

        mockMvc.perform(post("/api/taxes/simulation").contentType("application/json").content(body))
                .andExpect(status().isBadRequest());
    }

    @Test
    void aNegativeQuantityReturns400() throws Exception {
        String body = """
                [ {"operation":"buy", "unit-cost":10.00, "quantity":-5} ]
                """;

        mockMvc.perform(post("/api/taxes/simulation").contentType("application/json").content(body))
                .andExpect(status().isBadRequest());
    }

    @Test
    void malformedJsonReturns400() throws Exception {
        mockMvc.perform(post("/api/taxes/simulation").contentType("application/json").content("{ not valid json "))
                .andExpect(status().isBadRequest());
    }

    @Test
    void aUnitCostWithMoreThanTwoDecimalPlacesReturns400() throws Exception {
        String body = """
                [ {"operation":"buy", "unit-cost":10.005, "quantity":100} ]
                """;

        mockMvc.perform(post("/api/taxes/simulation").contentType("application/json").content(body))
                .andExpect(status().isBadRequest());
    }

    @Test
    void aNonNumericIdOnTheDetailRouteReturns400() throws Exception {
        mockMvc.perform(get("/api/simulations/abc"))
                .andExpect(status().isBadRequest());
    }
}
