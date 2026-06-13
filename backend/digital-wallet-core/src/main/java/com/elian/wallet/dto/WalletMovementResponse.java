package com.elian.wallet.dto;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

public record WalletMovementResponse(
        UUID id,
        UUID transaccionId,
        String naturaleza,
        BigDecimal monto,
        BigDecimal saldoAnterior,
        BigDecimal saldoPosterior,
        OffsetDateTime creadoEn
) {
}
