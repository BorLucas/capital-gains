package com.example.capitalgains.web;

import com.example.capitalgains.broker.BrokerConfig;
import com.example.capitalgains.broker.BrokerConfigRepository;
import com.example.capitalgains.broker.Role;
import com.example.capitalgains.broker.UserAccount;
import com.example.capitalgains.broker.UserAccountRepository;
import com.example.capitalgains.domain.FeeSchedule;
import com.example.capitalgains.domain.TaxRules;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class AdminControllerTest {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    ObjectMapper json;

    @Autowired
    UserAccountRepository users;

    @Autowired
    BrokerConfigRepository config;

    @AfterEach
    void restoreDefaultRules() {
        BrokerConfig current = config.findById(BrokerConfig.ID).orElseThrow();
        current.apply(FeeSchedule.fixed("5.00"), TaxRules.CHALLENGE);
        config.save(current);
    }

    private static String newName(String prefix) {
        return prefix + UUID.randomUUID().toString().substring(0, 8);
    }

    private MockHttpSession login(String username, String password) throws Exception {
        return (MockHttpSession) mockMvc.perform(post("/api/auth/login").contentType("application/json")
                        .content("{\"username\":\"%s\",\"password\":\"%s\"}".formatted(username, password)))
                .andExpect(status().isOk())
                .andReturn().getRequest().getSession(false);
    }

    private MockHttpSession user() throws Exception {
        String username = newName("u");
        return (MockHttpSession) mockMvc.perform(post("/api/auth/register").contentType("application/json")
                        .content("{\"username\":\"%s\",\"password\":\"123456\"}".formatted(username)))
                .andExpect(status().isCreated())
                .andReturn().getRequest().getSession(false);
    }

    /** An admin for the test: signs up, then is promoted straight in the database. */
    private MockHttpSession admin() throws Exception {
        String username = newName("a");
        mockMvc.perform(post("/api/auth/register").contentType("application/json")
                .content("{\"username\":\"%s\",\"password\":\"123456\"}".formatted(username)));
        UserAccount account = users.findByUsername(username).orElseThrow();
        account.changeRole(Role.ADMIN);
        users.save(account);
        return login(username, "123456");
    }

    @Test
    void theBootstrapCreatedAFirstAdmin() {
        assertThat(users.existsByRole(Role.ADMIN)).isTrue();
    }

    @Test
    void aPlainUserCannotReachTheAdminConsole() throws Exception {
        MockHttpSession session = user();

        mockMvc.perform(get("/api/admin/users").session(session)).andExpect(status().isForbidden());
        mockMvc.perform(get("/api/admin/rules").session(session)).andExpect(status().isForbidden());
        mockMvc.perform(put("/api/admin/rules").session(session).contentType("application/json")
                        .content("{\"feeType\":\"FIXED\",\"feeValue\":0,\"taxRatePercent\":0,\"exemptionLimit\":0}"))
                .andExpect(status().isForbidden());
        mockMvc.perform(post("/api/admin/tickers").session(session).contentType("application/json")
                        .content("{\"symbol\":\"HACK3\",\"name\":\"x\",\"referencePrice\":1}"))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/admin/users")).andExpect(status().isUnauthorized());
    }

    @Test
    void anAdminCreatesUsersAndChangesRoles() throws Exception {
        MockHttpSession admin = admin();
        String username = newName("n");

        String created = mockMvc.perform(post("/api/admin/users").session(admin).contentType("application/json")
                        .content("{\"username\":\"%s\",\"password\":\"123456\",\"role\":\"USER\"}".formatted(username)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.role").value("USER"))
                .andReturn().getResponse().getContentAsString();
        long id = json.readTree(created).get("id").asLong();

        mockMvc.perform(post("/api/admin/users").session(admin).contentType("application/json")
                        .content("{\"username\":\"%s\",\"password\":\"123456\",\"role\":\"USER\"}".formatted(username)))
                .andExpect(status().isConflict());

        // the new user logs in and is not an admin yet
        MockHttpSession promoted = login(username, "123456");
        mockMvc.perform(get("/api/admin/users").session(promoted)).andExpect(status().isForbidden());

        mockMvc.perform(put("/api/admin/users/" + id + "/role").session(admin).contentType("application/json")
                        .content("{\"role\":\"ADMIN\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("ADMIN"));
        // the role is read from the database on every call: same session, access granted now
        mockMvc.perform(get("/api/admin/users").session(promoted)).andExpect(status().isOk());
        mockMvc.perform(get("/api/auth/me").session(promoted)).andExpect(jsonPath("$.role").value("ADMIN"));
    }

    @Test
    void anAdminCannotChangeTheirOwnRole() throws Exception {
        MockHttpSession admin = admin();
        long me = json.readTree(mockMvc.perform(get("/api/auth/me").session(admin))
                .andReturn().getResponse().getContentAsString()).get("id").asLong();

        mockMvc.perform(put("/api/admin/users/" + me + "/role").session(admin).contentType("application/json")
                        .content("{\"role\":\"USER\"}"))
                .andExpect(status().isConflict());
        mockMvc.perform(put("/api/admin/users/999999/role").session(admin).contentType("application/json")
                        .content("{\"role\":\"USER\"}"))
                .andExpect(status().isNotFound());
    }

    @Test
    void anAdminListsEditsAndDelistsTickers() throws Exception {
        MockHttpSession admin = admin();
        MockHttpSession user = user();
        String symbol = ("X" + UUID.randomUUID().toString().replace("-", "")).substring(0, 7).toUpperCase();

        mockMvc.perform(post("/api/admin/tickers").session(admin).contentType("application/json")
                        .content("{\"symbol\":\"%s\",\"name\":\"New Co\",\"referencePrice\":15.50}".formatted(symbol.toLowerCase())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.symbol").value(symbol))
                .andExpect(jsonPath("$.active").value(true));
        mockMvc.perform(post("/api/admin/tickers").session(admin).contentType("application/json")
                        .content("{\"symbol\":\"%s\",\"name\":\"Again\",\"referencePrice\":1}".formatted(symbol)))
                .andExpect(status().isConflict());

        String userList = mockMvc.perform(get("/api/broker/tickers").session(user))
                .andReturn().getResponse().getContentAsString();
        assertThat(userList).contains(symbol);

        mockMvc.perform(put("/api/admin/tickers/" + symbol).session(admin).contentType("application/json")
                        .content("{\"name\":\"New Co SA\",\"referencePrice\":16.00,\"active\":false}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("New Co SA"))
                .andExpect(jsonPath("$.active").value(false));

        assertThat(mockMvc.perform(get("/api/broker/tickers").session(user))
                .andReturn().getResponse().getContentAsString()).doesNotContain(symbol);
        assertThat(mockMvc.perform(get("/api/admin/tickers").session(admin))
                .andReturn().getResponse().getContentAsString()).contains(symbol);
    }

    @Test
    void anAdminSetsFeesAndTaxRules() throws Exception {
        MockHttpSession admin = admin();
        MockHttpSession user = user();

        mockMvc.perform(put("/api/admin/rules").session(admin).contentType("application/json")
                        .content("{\"feeType\":\"PERCENT\",\"feeValue\":0.5,\"taxRatePercent\":15,\"exemptionLimit\":35000}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.feeType").value("PERCENT"))
                .andExpect(jsonPath("$.taxRatePercent").value(15));

        mockMvc.perform(get("/api/broker/rules").session(user))
                .andExpect(jsonPath("$.feeType").value("PERCENT"))
                .andExpect(jsonPath("$.feeValue").value(0.5))
                .andExpect(jsonPath("$.exemptionLimit").value(35000.00));

        // out of range: the domain rejects it
        mockMvc.perform(put("/api/admin/rules").session(admin).contentType("application/json")
                        .content("{\"feeType\":\"FIXED\",\"feeValue\":1,\"taxRatePercent\":101,\"exemptionLimit\":0}"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(put("/api/admin/rules").session(admin).contentType("application/json")
                        .content("{\"feeType\":\"PERCENT\",\"feeValue\":150,\"taxRatePercent\":20,\"exemptionLimit\":0}"))
                .andExpect(status().isBadRequest());
    }
}
