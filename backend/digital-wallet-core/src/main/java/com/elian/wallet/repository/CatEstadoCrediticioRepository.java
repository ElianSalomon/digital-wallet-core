package com.elian.wallet.repository;

import com.elian.wallet.entity.CatEstadoCrediticio;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CatEstadoCrediticioRepository extends JpaRepository<CatEstadoCrediticio, Short> {
    Optional<CatEstadoCrediticio> findByCodigo(String codigo);
}
