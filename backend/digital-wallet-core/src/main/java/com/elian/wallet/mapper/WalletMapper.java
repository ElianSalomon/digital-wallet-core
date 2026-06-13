package com.elian.wallet.mapper;

import com.elian.wallet.dto.BalanceResponse;
import com.elian.wallet.dto.WalletResponse;
import com.elian.wallet.entity.Wallet;
import org.springframework.stereotype.Component;

@Component
public class WalletMapper {
    public WalletResponse toResponse(Wallet wallet) {
        return new WalletResponse(wallet.getId(), wallet.getAlias(), wallet.getSaldo(), wallet.getMoneda(), wallet.getActiva());
    }

    public BalanceResponse toBalance(Wallet wallet) {
        return new BalanceResponse(wallet.getId(), wallet.getAlias(), wallet.getSaldo(), wallet.getMoneda());
    }
}
