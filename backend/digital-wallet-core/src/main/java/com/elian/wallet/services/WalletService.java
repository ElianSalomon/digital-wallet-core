package com.elian.wallet.services;

import com.elian.wallet.dto.BalanceResponse;
import com.elian.wallet.dto.WalletMovementResponse;
import com.elian.wallet.dto.WalletRequest;
import com.elian.wallet.dto.WalletResponse;

import java.util.List;
import java.util.UUID;

public interface WalletService {
    WalletResponse create(WalletRequest request);
    List<WalletResponse> findMyWallets();
    BalanceResponse getBalance(UUID walletId);
    List<WalletMovementResponse> getHistory(UUID walletId);
}
