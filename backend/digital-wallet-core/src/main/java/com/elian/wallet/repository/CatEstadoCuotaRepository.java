package com.elian.wallet.repository;

import com.elian.wallet.entity.CatEstadoCuota;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CatEstadoCuotaRepository extends JpaRepository<CatEstadoCuota, Short> {
    Optional<CatEstadoCuota> findByCodigo(String codigo);
}
