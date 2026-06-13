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
import java.time.LocalDate;
import java.util.UUID;

@Getter
@Setter
@Entity
@Table(name = "cuotas_credito")
public class CuotaCredito {
    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "credito_id")
    private Credito credito;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "estado_cuota_id")
    private CatEstadoCuota estadoCuota;

    @Column(name = "numero_cuota")
    private Integer numeroCuota;

    @Column(name = "fecha_vencimiento")
    private LocalDate fechaVencimiento;

    @Column(name = "monto_capital")
    private BigDecimal montoCapital;

    @Column(name = "monto_interes")
    private BigDecimal montoInteres;

    @Column(name = "monto_total")
    private BigDecimal montoTotal;

    @Column(name = "monto_pagado")
    private BigDecimal montoPagado;
}
