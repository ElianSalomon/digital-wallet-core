package com.elian.wallet.repository;

import com.elian.wallet.entity.CatEstadoCredito;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CatEstadoCreditoRepository extends JpaRepository<CatEstadoCredito, Short> {
    Optional<CatEstadoCredito> findByCodigo(String codigo);
}
