package com.elian.wallet.repository;

import com.elian.wallet.entity.CatTipoTransaccion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CatTipoTransaccionRepository extends JpaRepository<CatTipoTransaccion, Short> {
    Optional<CatTipoTransaccion> findByCodigo(String codigo);
}
