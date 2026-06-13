package com.elian.wallet.controller;

import com.elian.wallet.exception.GlobalExceptionHandler;
import com.elian.wallet.services.AuthService;
import com.elian.wallet.services.CreditService;
import com.elian.wallet.services.TransactionService;
import com.elian.wallet.services.WalletService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

import static org.mockito.Mockito.mock;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class EndpointValidationTest {
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();

        mockMvc = MockMvcBuilders.standaloneSetup(
                        new AuthController(mock(AuthService.class)),
                        new WalletController(mock(WalletService.class)),
                        new TransactionController(mock(TransactionService.class)),
                        new CreditController(mock(CreditService.class))
                )
                .setControllerAdvice(new GlobalExceptionHandler())
                .setValidator(validator)
                .build();
    }

    @Test
    void registerRejectsInvalidPayload() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .contentType("application/json")
                        .content("""
                                {
                                  "nombre": "",
                                  "apellido": "",
                                  "email": "correo-invalido",
                                  "password": "123",
                                  "telefono": "",
                                  "documentoIdentidad": "",
                                  "fechaNacimiento": "2030-01-01"
                                }
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void loginRejectsInvalidPayload() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType("application/json")
                        .content("""
                                {
                                  "email": "correo-invalido",
                                  "password": ""
                                }
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void walletCreationRejectsInvalidCurrency() throws Exception {
        mockMvc.perform(post("/api/wallets")
                        .contentType("application/json")
                        .content("""
                                {
                                  "alias": "principal",
                                  "moneda": "mxn"
                                }
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void depositRejectsInvalidAmountAndMissingIdempotencyKey() throws Exception {
        mockMvc.perform(post("/api/transactions/deposit")
                        .contentType("application/json")
                        .content("""
                                {
                                  "walletDestinoId": "11111111-1111-1111-1111-111111111111",
                                  "monto": 0,
                                  "idempotencyKey": ""
                                }
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void withdrawRejectsInvalidAmount() throws Exception {
        mockMvc.perform(post("/api/transactions/withdraw")
                        .contentType("application/json")
                        .content("""
                                {
                                  "walletOrigenId": "11111111-1111-1111-1111-111111111111",
                                  "monto": -10,
                                  "idempotencyKey": "withdraw-001"
                                }
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void transferRejectsMissingAmount() throws Exception {
        mockMvc.perform(post("/api/transactions/transfer")
                        .contentType("application/json")
                        .content("""
                                {
                                  "walletOrigenId": "11111111-1111-1111-1111-111111111111",
                                  "walletDestinoId": "22222222-2222-2222-2222-222222222222",
                                  "idempotencyKey": "transfer-001"
                                }
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void creditRequestRejectsInvalidPayload() throws Exception {
        mockMvc.perform(post("/api/credits")
                        .contentType("application/json")
                        .content("""
                                {
                                  "montoSolicitado": 0,
                                  "tasaInteresAnual": -1,
                                  "plazoMeses": 0
                                }
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void creditApprovalRejectsInvalidAmount() throws Exception {
        mockMvc.perform(patch("/api/credits/11111111-1111-1111-1111-111111111111/approve")
                        .contentType("application/json")
                        .content("""
                                {
                                  "montoAprobado": 0
                                }
                                """))
                .andExpect(status().isBadRequest());
    }
}
