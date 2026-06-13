package com.elian.wallet.mapper;

import com.elian.wallet.dto.TransactionResponse;
import com.elian.wallet.dto.WalletMovementResponse;
import com.elian.wallet.entity.MovimientoWallet;
import com.elian.wallet.entity.Transaccion;
import org.springframework.stereotype.Component;

@Component
public class TransactionMapper {
    public TransactionResponse toResponse(Transaccion transaccion) {
        return new TransactionResponse(
                transaccion.getId(),
                transaccion.getTipoTransaccion().getCodigo(),
                transaccion.getEstadoTransaccion().getCodigo(),
                transaccion.getWalletOrigen() != null ? transaccion.getWalletOrigen().getId() : null,
                transaccion.getWalletDestino() != null ? transaccion.getWalletDestino().getId() : null,
                transaccion.getMonto(),
                transaccion.getMoneda(),
                transaccion.getIdempotencyKey(),
                transaccion.getCreadaEn()
        );
    }

    public WalletMovementResponse toMovementResponse(MovimientoWallet movimiento) {
        return new WalletMovementResponse(
                movimiento.getId(),
                movimiento.getTransaccion().getId(),
                movimiento.getNaturaleza(),
                movimiento.getMonto(),
                movimiento.getSaldoAnterior(),
                movimiento.getSaldoPosterior(),
                movimiento.getCreadoEn()
        );
    }
}
