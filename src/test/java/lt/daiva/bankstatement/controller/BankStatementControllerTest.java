package lt.daiva.bankstatement.controller;

import lt.daiva.bankstatement.dto.BalanceResponse;
import lt.daiva.bankstatement.dto.CurrencyBalance;
import lt.daiva.bankstatement.service.BankStatementService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class BankStatementControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    BankStatementService service;

    @Test
    void shouldReturnBalance_whenRequestIsValid() throws Exception {
        when(service.calculateBalance(eq("LT100001"), any(), any()))
                .thenReturn(new BalanceResponse("LT100001",
                        List.of(new CurrencyBalance("EUR", new BigDecimal("10.00")))));

        mockMvc.perform(get("/api/v1/statements/accounts/LT100001/balance")
                        .param("from", "2025-01-01")
                        .param("to", "2025-01-31"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.balances.length()").value(1))
                .andExpect(jsonPath("$.accountNumber").value("LT100001"))
                .andExpect(jsonPath("$.balances[0].amount").value(10.00))
                .andExpect(jsonPath("$.balances[0].currency").value("EUR"));
    }

    @Test
    void shouldReturn400_whenDateFormatIsInvalid() throws Exception {
        mockMvc.perform(get("/api/v1/statements/accounts/LT1/balance")
                        .param("from", "2025-01-02T09:15:00"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_PARAMETER"));
    }

    @Test
    void shouldReturn400_whenImportingNonCsvFile() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "data.txt",
                "application/octet-stream",
                "not a csv".getBytes()
        );

        mockMvc.perform(multipart("/api/v1/statements/import").file(file))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("BAD_REQUEST"));
    }
}