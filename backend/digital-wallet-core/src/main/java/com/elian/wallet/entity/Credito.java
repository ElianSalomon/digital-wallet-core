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
@Table(name = "creditos")
public class Credito {
    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id")
    private Usuario usuario;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "wallet_desembolso_id")
    private Wallet walletDesembolso;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "estado_credito_id")
    private CatEstadoCredito estadoCredito;

    @Column(name = "monto_solicitado")
    private BigDecimal montoSolicitado;

    @Column(name = "monto_aprobado")
    private BigDecimal montoAprobado;

    @Column(name = "tasa_interes_anual")
    private BigDecimal tasaInteresAnual;

    @Column(name = "plazo_meses")
    private Integer plazoMeses;

    @Column(name = "fecha_solicitud")
    private OffsetDateTime fechaSolicitud;

    @Column(name = "fecha_aprobacion")
    private OffsetDateTime fechaAprobacion;
}
