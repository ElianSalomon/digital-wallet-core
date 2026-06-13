package com.elian.wallet.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "cat_estados_crediticios")
public class CatEstadoCrediticio {
    @Id
    private Short id;
    private String codigo;
    private String nombre;
}
