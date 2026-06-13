package com.elian.wallet.repository;

import com.elian.wallet.entity.CatEstadoTransaccion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CatEstadoTransaccionRepository extends JpaRepository<CatEstadoTransaccion, Short> {
    Optional<CatEstadoTransaccion> findByCodigo(String codigo);
}
