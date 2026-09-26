package com.example.capitalgains.web;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class BrokerControllerTest {

    @Autowired
    MockMvc mockMvc;

    /** The context (and its database) is shared across test classes, so every test gets its own user. */
    private static String newUsername() {
        return "u" + UUID.randomUUID().toString().substring(0, 12);
    }

    private static String credentials(String username, String password) {
        return """
                {"username":"%s","password":"%s"}""".formatted(username, password);
    }

    private static String trade(String ticker, String operation, String unitCost, long quantity) {
        return """
                {"ticker":"%s","operation":"%s","unit-cost":%s,"quantity":%d}"""
                .formatted(ticker, operation, unitCost, quantity);
    }

    private MockHttpSession register(String username) throws Exception {
        return (MockHttpSession) mockMvc.perform(post("/api/auth/register")
                        .contentType("application/json").content(credentials(username, "123456")))
                .andExpect(status().isCreated())
                .andReturn().getRequest().getSession(false);
    }

    private void setFee(MockHttpSession session, String fee) throws Exception {
        mockMvc.perform(put("/api/broker/settings").session(session)
                        .contentType("application/json").content("{\"brokerageFee\":" + fee + "}"))
                .andExpect(status().isOk());
    }

    private ResultActions place(MockHttpSession session, String body) throws Exception {
        return mockMvc.perform(post("/api/broker/trades").session(session)
                .contentType("application/json").content(body));
    }

    @Test
    void aUserBuysAndSellsAndSeesProfitFeesAndTax() throws Exception {
        MockHttpSession session = register(newUsername()); // default fee: 5.00 per order

        place(session, trade("petr4", "buy", "10.00", 10))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.ticker").value("PETR4"))
                .andExpect(jsonPath("$.fee").value(5.00))
                .andExpect(jsonPath("$.netValue").value(105.00))
                .andExpect(jsonPath("$.averagePriceAfter").value(10.50));

        // sold 5 at 50.00: (250.00 - 5.00) - 5 x 10.50 = 192.50; the 250.00 sale is exempt
        place(session, trade("PETR4", "sell", "50.00", 5))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.averagePriceBefore").value(10.50))
                .andExpect(jsonPath("$.netValue").value(245.00))
                .andExpect(jsonPath("$.result").value(192.50))
                .andExpect(jsonPath("$.exempt").value(true))
                .andExpect(jsonPath("$.tax").value(0.00))
                .andExpect(jsonPath("$.netResult").value(192.50))
                .andExpect(jsonPath("$.positionAfter").value(5));

        mockMvc.perform(get("/api/broker/account").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.positions[0].ticker").value("PETR4"))
                .andExpect(jsonPath("$.positions[0].quantity").value(5))
                .andExpect(jsonPath("$.positions[0].averagePrice").value(10.50))
                .andExpect(jsonPath("$.realizedResult").value(192.50))
                .andExpect(jsonPath("$.taxPaid").value(0.00))
                .andExpect(jsonPath("$.trades.length()").value(2))
                .andExpect(jsonPath("$.trades[0].operation").value("sell"));
    }

    @Test
    void aSellAboveTheExemptionIsTaxedAndTickersAreIndependent() throws Exception {
        MockHttpSession session = register(newUsername());
        setFee(session, "0"); // isolate the tax rule (official case 2)
        place(session, trade("VALE3", "buy", "10.00", 10000));
        place(session, trade("ITUB4", "buy", "99.00", 1));

        // quote first: nothing is executed
        mockMvc.perform(post("/api/broker/quote").session(session)
                        .contentType("application/json").content(trade("VALE3", "sell", "20.00", 5000)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").doesNotExist())
                .andExpect(jsonPath("$.exempt").value(false))
                .andExpect(jsonPath("$.tax").value(10000.00));

        place(session, trade("VALE3", "sell", "20.00", 5000))
                .andExpect(jsonPath("$.result").value(50000.00))
                .andExpect(jsonPath("$.tax").value(10000.00))
                .andExpect(jsonPath("$.netResult").value(40000.00));

        mockMvc.perform(get("/api/broker/account").session(session))
                .andExpect(jsonPath("$.positions.length()").value(2))
                .andExpect(jsonPath("$.trades.length()").value(3))
                .andExpect(jsonPath("$.taxPaid").value(10000.00));
    }

    @Test
    void sellingMoreThanHeldIsRejectedAndNothingIsStored() throws Exception {
        MockHttpSession session = register(newUsername());
        place(session, trade("WEGE3", "buy", "40.00", 3));

        place(session, trade("WEGE3", "sell", "40.00", 4))
                .andExpect(status().isUnprocessableEntity());
        place(session, trade("BBAS3", "sell", "10.00", 1))
                .andExpect(status().isUnprocessableEntity());

        mockMvc.perform(get("/api/broker/account").session(session))
                .andExpect(jsonPath("$.trades.length()").value(1));
    }

    @Test
    void usersOnlySeeTheirOwnTrades() throws Exception {
        MockHttpSession alice = register(newUsername());
        MockHttpSession bob = register(newUsername());
        place(alice, trade("MGLU3", "buy", "2.00", 100));

        mockMvc.perform(get("/api/broker/account").session(bob))
                .andExpect(jsonPath("$.trades.length()").value(0));
        place(bob, trade("MGLU3", "sell", "2.00", 1))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    void loginWorksWithTheRightPasswordOnly() throws Exception {
        String username = newUsername();
        register(username);

        mockMvc.perform(post("/api/auth/login").contentType("application/json")
                        .content(credentials(username.toUpperCase(), "123456")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value(username));

        mockMvc.perform(post("/api/auth/login").contentType("application/json")
                        .content(credentials(username, "1234567")))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(post("/api/auth/login").contentType("application/json")
                        .content(credentials("nobody-" + username, "123456")))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void signUpRules() throws Exception {
        String username = newUsername();

        mockMvc.perform(post("/api/auth/register").contentType("application/json")
                        .content(credentials(username, "12345")))
                .andExpect(status().isBadRequest());

        register(username);
        mockMvc.perform(post("/api/auth/register").contentType("application/json")
                        .content(credentials(username, "abcdef")))
                .andExpect(status().isConflict());
    }

    @Test
    void theBrokerRequiresASessionAndLogoutEndsIt() throws Exception {
        mockMvc.perform(get("/api/broker/account")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/auth/me")).andExpect(status().isUnauthorized());

        String username = newUsername();
        MockHttpSession session = register(username);
        mockMvc.perform(get("/api/auth/me").session(session))
                .andExpect(jsonPath("$.username").value(username));

        mockMvc.perform(post("/api/auth/logout").session(session)).andExpect(status().isNoContent());
        mockMvc.perform(get("/api/broker/account").session(session)).andExpect(status().isUnauthorized());
    }

    @Test
    void theFeeSettingAppliesToNewOrdersOnly() throws Exception {
        MockHttpSession session = register(newUsername());
        mockMvc.perform(get("/api/broker/settings").session(session))
                .andExpect(jsonPath("$.brokerageFee").value(5.00));

        place(session, trade("ABEV3", "buy", "12.00", 10)).andExpect(jsonPath("$.fee").value(5.00));
        setFee(session, "2.50");
        place(session, trade("ABEV3", "buy", "12.00", 10)).andExpect(jsonPath("$.fee").value(2.50));

        mockMvc.perform(get("/api/broker/account").session(session))
                .andExpect(jsonPath("$.trades[0].fee").value(2.50))
                .andExpect(jsonPath("$.trades[1].fee").value(5.00));

        mockMvc.perform(put("/api/broker/settings").session(session)
                        .contentType("application/json").content("{\"brokerageFee\":-1}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void changingThePasswordNeedsTheCurrentOne() throws Exception {
        String username = newUsername();
        MockHttpSession session = register(username);

        mockMvc.perform(post("/api/auth/password").session(session).contentType("application/json")
                        .content("{\"currentPassword\":\"wrong!\",\"newPassword\":\"abcdef\"}"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(post("/api/auth/password").session(session).contentType("application/json")
                        .content("{\"currentPassword\":\"123456\",\"newPassword\":\"12345\"}"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(post("/api/auth/password").session(session).contentType("application/json")
                        .content("{\"currentPassword\":\"123456\",\"newPassword\":\"abcdef\"}"))
                .andExpect(status().isNoContent());

        mockMvc.perform(post("/api/auth/login").contentType("application/json")
                        .content(credentials(username, "123456")))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(post("/api/auth/login").contentType("application/json")
                        .content(credentials(username, "abcdef")))
                .andExpect(status().isOk());
    }

    @Test
    void meReportsTheSession() throws Exception {
        String username = newUsername();
        MockHttpSession session = register(username);

        mockMvc.perform(get("/api/auth/me").session(session))
                .andExpect(jsonPath("$.username").value(username))
                .andExpect(jsonPath("$.memberSince").isNotEmpty())
                .andExpect(jsonPath("$.loggedInAt").isNotEmpty())
                .andExpect(jsonPath("$.idleTimeoutSeconds").isNumber());
    }
}
