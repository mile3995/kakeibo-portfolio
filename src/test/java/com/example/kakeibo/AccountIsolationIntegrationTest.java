package com.example.kakeibo;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import com.example.kakeibo.account.AccountRepository;
import com.example.kakeibo.account.AccountService;
import com.example.kakeibo.expense.ExpenseRepository;
import com.example.kakeibo.income.IncomeRepository;

import static org.junit.jupiter.api.Assertions.assertNotNull;

@SpringBootTest
@AutoConfigureMockMvc
class AccountIsolationIntegrationTest {

    private static final String TEST_PASSWORD = "Portfolio_test_2026!";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private AccountService accountService;

    @Autowired
    private AccountRepository accounts;

    @Autowired
    private ExpenseRepository expenses;

    @Autowired
    private IncomeRepository incomes;

    @BeforeEach
    void clearDatabase() {
        expenses.deleteAll();
        incomes.deleteAll();
        accounts.deleteAll();
    }

    @Test
    void registeredUserCanLogInAndReadTheirAccount() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"alice","password":"Portfolio_test_2026!"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("alice"));

        MvcResult loginResult = mockMvc.perform(post("/login")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param("username", "alice")
                        .param("password", TEST_PASSWORD))
                .andExpect(status().is3xxRedirection())
                .andReturn();

        MockHttpSession session = (MockHttpSession) loginResult.getRequest().getSession(false);
        assertNotNull(session, "ログイン後のセッションが作られること");

        mockMvc.perform(get("/api/auth/me").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("alice"));
    }

    @Test
    void expensesAndIncomesAreVisibleOnlyToTheirOwner() throws Exception {
        accountService.register("alice", TEST_PASSWORD);
        accountService.register("bob", TEST_PASSWORD);

        mockMvc.perform(post("/api/expenses")
                        .with(user("alice"))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"date":"2026-10-05","amount":1200,"category":"食費","memo":"昼食"}
                                """))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/incomes")
                        .with(user("alice"))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"date":"2026-10-01","amount":200000,"category":"給与","memo":"10月分"}
                                """))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/expenses").with(user("alice")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].amount").value(1200));

        mockMvc.perform(get("/api/incomes").with(user("alice")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].amount").value(200000));

        mockMvc.perform(get("/api/expenses").with(user("bob")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));

        mockMvc.perform(get("/api/incomes").with(user("bob")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }
}

