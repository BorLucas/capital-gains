package com.example.capitalgains.web;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class OrderControllerTest {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    ObjectMapper json;

    private JsonNode postJson(String path, String body) throws Exception {
        String response = mockMvc.perform(post(path).contentType("application/json").content(body))
                .andExpect(status().isAccepted())
                .andReturn().getResponse().getContentAsString();
        return json.readTree(response);
    }

    private JsonNode getOrder(String orderId) throws Exception {
        String response = mockMvc.perform(get("/api/taxes/orders/" + orderId))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return json.readTree(response);
    }

    private void awaitStatus(String orderId, String expectedStatus) {
        await().atMost(Duration.ofSeconds(5)).untilAsserted(() ->
                assertThat(getOrder(orderId).get("status").asText()).isEqualTo(expectedStatus));
    }

    @Test
    void anAcceptedOrderIsAssessedAndLinksToTheStoredSimulation() throws Exception {
        String body = """
                [ {"operation":"buy",  "unit-cost":10.00, "quantity":10000},
                  {"operation":"sell", "unit-cost":20.00, "quantity":5000},
                  {"operation":"sell", "unit-cost":5.00,  "quantity":5000} ]
                """;

        String response = mockMvc.perform(post("/api/taxes/orders").contentType("application/json").content(body))
                .andExpect(status().isAccepted())
                .andExpect(header().string("Location", startsWith("/api/taxes/orders/")))
                .andReturn().getResponse().getContentAsString();
        String orderId = json.readTree(response).get("id").asText();

        awaitStatus(orderId, "COMPLETED");

        JsonNode order = getOrder(orderId);
        assertThat(order.get("simulationId").isNumber()).isTrue();
        assertThat(order.hasNonNull("failureReason")).isFalse();

        mockMvc.perform(get(order.get("simulationUrl").asText()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalTax").value(10000.00))
                .andExpect(jsonPath("$.trades[1].tax").value(10000.00));
    }

    @Test
    void anOrderThatBreaksABusinessRuleEndsUpFailed() throws Exception {
        String orderId = postJson("/api/taxes/orders", """
                [ {"operation":"buy","unit-cost":10.00,"quantity":100},
                  {"operation":"sell","unit-cost":20.00,"quantity":200} ]
                """).get("id").asText();

        awaitStatus(orderId, "FAILED");

        JsonNode order = getOrder(orderId);
        assertThat(order.get("failureReason").asText()).isEqualTo("selling 200 shares, but only 100 are held");
        assertThat(order.hasNonNull("simulationId")).isFalse();
    }

    @Test
    void aBatchIsAcceptedAsSeveralOrdersAndAllGetAssessed() throws Exception {
        JsonNode orders = postJson("/api/taxes/orders/batch", """
                [
                  [ {"operation":"buy","unit-cost":10.00,"quantity":100},
                    {"operation":"sell","unit-cost":15.00,"quantity":50},
                    {"operation":"sell","unit-cost":15.00,"quantity":50} ],
                  [ {"operation":"buy","unit-cost":10.00,"quantity":10000},
                    {"operation":"sell","unit-cost":20.00,"quantity":5000},
                    {"operation":"sell","unit-cost":5.00,"quantity":5000} ]
                ]
                """);

        assertThat(orders.size()).isEqualTo(2);
        for (JsonNode order : orders) {
            assertThat(order.get("id").asText()).isNotBlank();
            awaitStatus(order.get("id").asText(), "COMPLETED");
        }
    }

    @Test
    void malformedTradesAreRejectedAtSubmissionWith400() throws Exception {
        mockMvc.perform(post("/api/taxes/orders").contentType("application/json").content("""
                        [ {"operation":"buy", "unit-cost":10.005, "quantity":100} ]
                        """))
                .andExpect(status().isBadRequest());

        mockMvc.perform(post("/api/taxes/orders").contentType("application/json").content("""
                        [ {"operation":"buy", "unit-cost":10.00, "quantity":-5} ]
                        """))
                .andExpect(status().isBadRequest());

        mockMvc.perform(post("/api/taxes/orders").contentType("application/json").content("[]"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void anUnknownOrderReturns404() throws Exception {
        mockMvc.perform(get("/api/taxes/orders/00000000-0000-0000-0000-000000000000"))
                .andExpect(status().isNotFound());
    }

    @Test
    void aMalformedOrderIdReturns400() throws Exception {
        mockMvc.perform(get("/api/taxes/orders/not-a-uuid"))
                .andExpect(status().isBadRequest());
    }
}
