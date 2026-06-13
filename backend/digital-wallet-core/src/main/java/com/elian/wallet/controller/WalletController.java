package com.elian.wallet.controller;

import com.elian.wallet.dto.BalanceResponse;
import com.elian.wallet.dto.WalletMovementResponse;
import com.elian.wallet.dto.WalletRequest;
import com.elian.wallet.dto.WalletResponse;
import com.elian.wallet.services.WalletService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/wallets")
@RequiredArgsConstructor
public class WalletController {
    private final WalletService walletService;

    @PostMapping
    public ResponseEntity<WalletResponse> create(@Valid @RequestBody WalletRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(walletService.create(request));
    }

    @GetMapping
    public ResponseEntity<List<WalletResponse>> findMyWallets() {
        return ResponseEntity.ok(walletService.findMyWallets());
    }

    @GetMapping("/{walletId}/balance")
    public ResponseEntity<BalanceResponse> getBalance(@PathVariable UUID walletId) {
        return ResponseEntity.ok(walletService.getBalance(walletId));
    }

    @GetMapping("/{walletId}/history")
    public ResponseEntity<List<WalletMovementResponse>> getHistory(@PathVariable UUID walletId) {
        return ResponseEntity.ok(walletService.getHistory(walletId));
    }
}
