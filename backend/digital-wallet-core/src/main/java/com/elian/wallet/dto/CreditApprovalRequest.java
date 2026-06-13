package com.elian.wallet.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record CreditApprovalRequest(
        @NotNull @DecimalMin(value = "0.0001") BigDecimal montoAprobado
) {
}
