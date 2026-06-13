package com.elian.wallet.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.UUID;

public record TransactionRequest(
        UUID walletOrigenId,
        UUID walletDestinoId,
        @NotNull @DecimalMin(value = "0.0001") BigDecimal monto,
        @NotBlank @Size(max = 120) String idempotencyKey,
        @Size(max = 255) String descripcion
) {
}
