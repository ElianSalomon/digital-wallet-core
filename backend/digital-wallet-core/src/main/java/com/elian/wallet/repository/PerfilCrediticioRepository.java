package com.elian.wallet.repository;

import com.elian.wallet.entity.PerfilCrediticio;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface PerfilCrediticioRepository extends JpaRepository<PerfilCrediticio, UUID> {
    Optional<PerfilCrediticio> findByUsuarioId(UUID usuarioId);
}
