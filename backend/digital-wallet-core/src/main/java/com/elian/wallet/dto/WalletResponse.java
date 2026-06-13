package com.elian.wallet.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record WalletResponse(
        UUID id,
        String alias,
        BigDecimal saldo,
        String moneda,
        Boolean activa
) {
}
