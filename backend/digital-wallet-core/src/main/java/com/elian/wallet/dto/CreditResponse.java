package com.elian.wallet.dto;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

public record CreditResponse(
        UUID id,
        String estado,
        BigDecimal montoSolicitado,
        BigDecimal montoAprobado,
        BigDecimal tasaInteresAnual,
        Integer plazoMeses,
        OffsetDateTime fechaSolicitud,
        OffsetDateTime fechaAprobacion
) {
}
