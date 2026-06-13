package com.elian.wallet.mapper;

import com.elian.wallet.dto.CreditResponse;
import com.elian.wallet.dto.CuotaResponse;
import com.elian.wallet.entity.Credito;
import com.elian.wallet.entity.CuotaCredito;
import org.springframework.stereotype.Component;

@Component
public class CreditMapper {
    public CreditResponse toResponse(Credito credito) {
        return new CreditResponse(
                credito.getId(),
                credito.getEstadoCredito().getCodigo(),
                credito.getMontoSolicitado(),
                credito.getMontoAprobado(),
                credito.getTasaInteresAnual(),
                credito.getPlazoMeses(),
                credito.getFechaSolicitud(),
                credito.getFechaAprobacion()
        );
    }

    public CuotaResponse toCuotaResponse(CuotaCredito cuota) {
        return new CuotaResponse(
                cuota.getId(),
                cuota.getNumeroCuota(),
                cuota.getEstadoCuota().getCodigo(),
                cuota.getFechaVencimiento(),
                cuota.getMontoCapital(),
                cuota.getMontoInteres(),
                cuota.getMontoTotal(),
                cuota.getMontoPagado()
        );
    }
}
