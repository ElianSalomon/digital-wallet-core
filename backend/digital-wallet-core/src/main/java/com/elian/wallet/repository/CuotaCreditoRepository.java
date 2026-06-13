package com.elian.wallet.repository;

import com.elian.wallet.entity.CuotaCredito;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface CuotaCreditoRepository extends JpaRepository<CuotaCredito, UUID> {
    boolean existsByCreditoId(UUID creditoId);
    List<CuotaCredito> findByCreditoIdOrderByNumeroCuota(UUID creditoId);
}
