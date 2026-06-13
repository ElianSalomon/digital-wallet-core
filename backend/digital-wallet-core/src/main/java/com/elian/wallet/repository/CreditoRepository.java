package com.elian.wallet.repository;

import com.elian.wallet.entity.Credito;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface CreditoRepository extends JpaRepository<Credito, UUID> {
    List<Credito> findByUsuarioIdOrderByFechaSolicitudDesc(UUID usuarioId);
}
