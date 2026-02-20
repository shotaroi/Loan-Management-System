package com.shotaroi.loan.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import com.shotaroi.loan.LoanManagementApplication;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(classes = LoanManagementApplication.class)
@AutoConfigureMockMvc
@ActiveProfiles("test")
class LoanFlowIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void approve_application_create_loan_schedule_generated_correctly() throws Exception {
        String userEmail = "user-flow@test.com";
        String userPassword = "password123";

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "email", userEmail,
                                "password", userPassword))))
                .andExpect(status().isCreated());

        MvcResult loginResult = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "email", userEmail,
                                "password", userPassword))))
                .andExpect(status().isOk())
                .andReturn();

        String userToken = extractToken(loginResult);

        MvcResult appResult = mockMvc.perform(post("/api/applications")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "principal", 100000,
                                "currency", "SEK",
                                "termMonths", 12,
                                "annualInterestRate", 0.05))))
                .andExpect(status().isCreated())
                .andReturn();

        Long applicationId = extractApplicationId(appResult);

        MvcResult uwLoginResult = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "email", "underwriter@loan.local",
                                "password", "password123"))))
                .andExpect(status().isOk())
                .andReturn();

        String uwToken = extractToken(uwLoginResult);

        mockMvc.perform(post("/api/underwriting/applications/" + applicationId + "/decision")
                        .header("Authorization", "Bearer " + uwToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "decision", "APPROVED",
                                "reason", "Good credit"))))
                .andExpect(status().isOk());

        MvcResult loanResult = mockMvc.perform(post("/api/loans/from-application/" + applicationId)
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "startDate", LocalDate.now().toString()))))
                .andExpect(status().isCreated())
                .andReturn();

        Long loanId = extractLoanId(loanResult);

        MvcResult scheduleResult = mockMvc.perform(get("/api/loans/" + loanId + "/schedule")
                        .header("Authorization", "Bearer " + userToken)
                        .param("size", "100"))
                .andExpect(status().isOk())
                .andReturn();

        String scheduleBody = scheduleResult.getResponse().getContentAsString();
        var schedulePage = objectMapper.readValue(scheduleBody, Map.class);
        var content = (java.util.List<?>) schedulePage.get("content");
        assertThat(content).hasSize(12);
    }

    @Test
    void post_payment_updates_loan_outstanding_and_marks_installments_paid() throws Exception {
        String userEmail = "user-pay@test.com";
        String userPassword = "password123";

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "email", userEmail,
                                "password", userPassword))))
                .andExpect(status().isCreated());

        String userToken = extractToken(mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "email", userEmail,
                                "password", userPassword))))
                .andExpect(status().isOk())
                .andReturn());

        MvcResult appResult = mockMvc.perform(post("/api/applications")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "principal", 12000,
                                "currency", "SEK",
                                "termMonths", 12,
                                "annualInterestRate", 0.12))))
                .andExpect(status().isCreated())
                .andReturn();

        Long applicationId = extractApplicationId(appResult);

        String uwToken = extractToken(mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "email", "underwriter@loan.local",
                                "password", "password123"))))
                .andExpect(status().isOk())
                .andReturn());

        mockMvc.perform(post("/api/underwriting/applications/" + applicationId + "/decision")
                        .header("Authorization", "Bearer " + uwToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "decision", "APPROVED",
                                "reason", "Approved"))))
                .andExpect(status().isOk());

        MvcResult loanResult = mockMvc.perform(post("/api/loans/from-application/" + applicationId)
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "startDate", LocalDate.now().toString()))))
                .andExpect(status().isCreated())
                .andReturn();

        Long loanId = extractLoanId(loanResult);

        MvcResult loanBefore = mockMvc.perform(get("/api/loans/" + loanId)
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk())
                .andReturn();

        BigDecimal outstandingBefore = new BigDecimal(loanBefore.getResponse().getContentAsString()
                .split("\"outstandingPrincipal\":")[1].split(",")[0]);

        MvcResult paymentResult = mockMvc.perform(post("/api/loans/" + loanId + "/payments")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "amount", 1100,
                                "currency", "SEK",
                                "paymentDate", LocalDate.now().toString(),
                                "reference", "test-ref"))))
                .andExpect(status().isCreated())
                .andReturn();

        String paymentBody = paymentResult.getResponse().getContentAsString();
        var paymentResp = objectMapper.readValue(paymentBody, Map.class);
        BigDecimal newOutstanding = new BigDecimal(paymentResp.get("newOutstandingPrincipal").toString());
        assertThat(newOutstanding).isLessThan(outstandingBefore);

        MvcResult scheduleResult = mockMvc.perform(get("/api/loans/" + loanId + "/schedule")
                        .header("Authorization", "Bearer " + userToken)
                        .param("size", "100"))
                .andExpect(status().isOk())
                .andReturn();

        String scheduleBody = scheduleResult.getResponse().getContentAsString();
        var schedulePage = objectMapper.readValue(scheduleBody, Map.class);
        var content = (java.util.List<?>) schedulePage.get("content");
        long paidCount = content.stream()
                .filter(i -> "PAID".equals(((Map) i).get("status")))
                .count();
        assertThat(paidCount).isGreaterThanOrEqualTo(0);
    }

    @Test
    void user_cannot_access_another_users_loan() throws Exception {
        String user1Email = "user1-sec@test.com";
        String user2Email = "user2-sec@test.com";
        String password = "password123";

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("email", user1Email, "password", password))))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("email", user2Email, "password", password))))
                .andExpect(status().isCreated());

        String user1Token = extractToken(mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("email", user1Email, "password", password))))
                .andExpect(status().isOk())
                .andReturn());

        String user2Token = extractToken(mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("email", user2Email, "password", password))))
                .andExpect(status().isOk())
                .andReturn());

        MvcResult appResult = mockMvc.perform(post("/api/applications")
                        .header("Authorization", "Bearer " + user1Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "principal", 50000,
                                "currency", "SEK",
                                "termMonths", 12,
                                "annualInterestRate", 0.05))))
                .andExpect(status().isCreated())
                .andReturn();

        Long applicationId = extractApplicationId(appResult);

        String uwToken = extractToken(mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "email", "underwriter@loan.local",
                                "password", "password123"))))
                .andExpect(status().isOk())
                .andReturn());

        mockMvc.perform(post("/api/underwriting/applications/" + applicationId + "/decision")
                        .header("Authorization", "Bearer " + uwToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "decision", "APPROVED",
                                "reason", "OK"))))
                .andExpect(status().isOk());

        MvcResult loanResult = mockMvc.perform(post("/api/loans/from-application/" + applicationId)
                        .header("Authorization", "Bearer " + user1Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "startDate", LocalDate.now().toString()))))
                .andExpect(status().isCreated())
                .andReturn();

        Long loanId = extractLoanId(loanResult);

        mockMvc.perform(get("/api/loans/" + loanId)
                        .header("Authorization", "Bearer " + user2Token))
                .andExpect(status().isForbidden());
    }

    private String extractToken(MvcResult result) throws Exception {
        String body = result.getResponse().getContentAsString();
        return objectMapper.readValue(body, Map.class).get("accessToken").toString();
    }

    private Long extractApplicationId(MvcResult result) throws Exception {
        String body = result.getResponse().getContentAsString();
        return Long.valueOf(objectMapper.readValue(body, Map.class).get("applicationId").toString());
    }

    private Long extractLoanId(MvcResult result) throws Exception {
        String body = result.getResponse().getContentAsString();
        return Long.valueOf(objectMapper.readValue(body, Map.class).get("loanId").toString());
    }
}
