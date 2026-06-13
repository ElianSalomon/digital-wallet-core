package com.elian.wallet.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@Getter
@Setter
@Entity
@Table(name = "perfiles_crediticios")
public class PerfilCrediticio {
    @Id
    private UUID id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id")
    private Usuario usuario;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "estado_crediticio_id")
    private CatEstadoCrediticio estadoCrediticio;

    @Column(name = "score_crediticio")
    private Integer scoreCrediticio;

    @Column(name = "ingreso_mensual")
    private BigDecimal ingresoMensual;

    @Column(name = "limite_credito")
    private BigDecimal limiteCredito;

    @Column(name = "deuda_actual")
    private BigDecimal deudaActual;

    @Column(name = "evaluado_en")
    private OffsetDateTime evaluadoEn;
}
