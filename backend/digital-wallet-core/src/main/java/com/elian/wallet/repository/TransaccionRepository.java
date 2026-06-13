package com.elian.wallet.repository;

import com.elian.wallet.entity.Transaccion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface TransaccionRepository extends JpaRepository<Transaccion, UUID> {
    Optional<Transaccion> findByIdempotencyKey(String idempotencyKey);
}
