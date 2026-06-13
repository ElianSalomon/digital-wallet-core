package com.elian.wallet.services.impl;

import com.elian.wallet.dto.BalanceResponse;
import com.elian.wallet.dto.WalletMovementResponse;
import com.elian.wallet.dto.WalletRequest;
import com.elian.wallet.dto.WalletResponse;
import com.elian.wallet.entity.Wallet;
import com.elian.wallet.exception.NotFoundException;
import com.elian.wallet.mapper.TransactionMapper;
import com.elian.wallet.mapper.WalletMapper;
import com.elian.wallet.repository.MovimientoWalletRepository;
import com.elian.wallet.repository.WalletRepository;
import com.elian.wallet.security.CurrentUserService;
import com.elian.wallet.services.WalletService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class WalletServiceImpl implements WalletService {
    private final WalletRepository walletRepository;
    private final MovimientoWalletRepository movimientoWalletRepository;
    private final CurrentUserService currentUserService;
    private final WalletMapper walletMapper;
    private final TransactionMapper transactionMapper;

    @Override
    @Transactional
    public WalletResponse create(WalletRequest request) {
        Wallet wallet = new Wallet();
        wallet.setId(UUID.randomUUID());
        wallet.setUsuario(currentUserService.get());
        wallet.setAlias(request.alias());
        wallet.setMoneda(request.moneda());
        wallet.setSaldo(BigDecimal.ZERO);
        wallet.setActiva(true);
        wallet.setCreadaEn(OffsetDateTime.now());
        wallet.setActualizadaEn(OffsetDateTime.now());
        return walletMapper.toResponse(walletRepository.save(wallet));
    }

    @Override
    public List<WalletResponse> findMyWallets() {
        return walletRepository.findByUsuarioIdAndActivaTrue(currentUserService.get().getId())
                .stream()
                .map(walletMapper::toResponse)
                .toList();
    }

    @Override
    public BalanceResponse getBalance(UUID walletId) {
        Wallet wallet = findOwnedWallet(walletId);
        return walletMapper.toBalance(wallet);
    }

    @Override
    public List<WalletMovementResponse> getHistory(UUID walletId) {
        findOwnedWallet(walletId);
        return movimientoWalletRepository.findByWalletIdOrderByCreadoEnDesc(walletId)
                .stream()
                .map(transactionMapper::toMovementResponse)
                .toList();
    }

    private Wallet findOwnedWallet(UUID walletId) {
        var user = currentUserService.get();
        Wallet wallet = walletRepository.findByIdAndActivaTrue(walletId)
                .orElseThrow(() -> new NotFoundException("Wallet no encontrada"));
        if (!wallet.getUsuario().getId().equals(user.getId())) {
            throw new NotFoundException("Wallet no encontrada");
        }
        return wallet;
    }
}
