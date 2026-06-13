package com.elian.wallet.dto;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

public record TransactionResponse(
        UUID id,
        String tipo,
        String estado,
        UUID walletOrigenId,
        UUID walletDestinoId,
        BigDecimal monto,
        String moneda,
        String idempotencyKey,
        OffsetDateTime creadaEn
) {
}
