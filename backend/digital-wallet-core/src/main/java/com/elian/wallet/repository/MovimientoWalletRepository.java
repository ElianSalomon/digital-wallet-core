package com.elian.wallet.repository;

import com.elian.wallet.entity.MovimientoWallet;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface MovimientoWalletRepository extends JpaRepository<MovimientoWallet, UUID> {
    List<MovimientoWallet> findByWalletIdOrderByCreadoEnDesc(UUID walletId);
}
