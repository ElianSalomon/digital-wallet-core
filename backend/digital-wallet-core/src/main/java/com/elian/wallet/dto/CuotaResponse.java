package com.elian.wallet.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record CuotaResponse(
        UUID id,
        Integer numeroCuota,
        String estado,
        LocalDate fechaVencimiento,
        BigDecimal montoCapital,
        BigDecimal montoInteres,
        BigDecimal montoTotal,
        BigDecimal montoPagado
) {
}
