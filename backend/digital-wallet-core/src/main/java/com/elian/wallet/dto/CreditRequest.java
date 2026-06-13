package com.elian.wallet.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.UUID;

public record CreditRequest(
        UUID walletDesembolsoId,
        @NotNull @DecimalMin(value = "0.0001") BigDecimal montoSolicitado,
        @NotNull @DecimalMin(value = "0.0000") BigDecimal tasaInteresAnual,
        @NotNull @Min(1) @Max(120) Integer plazoMeses
) {
}
