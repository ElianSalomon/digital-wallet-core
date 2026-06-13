package com.elian.wallet.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@Getter
@Setter
@Entity
@Table(name = "transacciones")
public class Transaccion {
    @Id
    private UUID id;

    @Column(name = "idempotency_key")
    private String idempotencyKey;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tipo_transaccion_id")
    private CatTipoTransaccion tipoTransaccion;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "estado_transaccion_id")
    private CatEstadoTransaccion estadoTransaccion;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "wallet_origen_id")
    private Wallet walletOrigen;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "wallet_destino_id")
    private Wallet walletDestino;

    private BigDecimal monto;
    @Column(length = 3)
    private String moneda;
    private String descripcion;

    @Column(name = "referencia_externa")
    private String referenciaExterna;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "creada_por")
    private Usuario creadaPor;

    @Column(name = "creada_en")
    private OffsetDateTime creadaEn;
}
