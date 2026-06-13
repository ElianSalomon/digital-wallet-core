package com.elian.wallet.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record BalanceResponse(
        UUID walletId,
        String alias,
        BigDecimal saldo,
        String moneda
) {
}
