package com.elian.wallet.controller;

import com.elian.wallet.dto.CreditApprovalRequest;
import com.elian.wallet.dto.CreditRequest;
import com.elian.wallet.dto.CreditResponse;
import com.elian.wallet.dto.CuotaResponse;
import com.elian.wallet.services.CreditService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/credits")
@RequiredArgsConstructor
public class CreditController {
    private final CreditService creditService;

    @PostMapping
    public ResponseEntity<CreditResponse> requestCredit(@Valid @RequestBody CreditRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(creditService.requestCredit(request));
    }

    @GetMapping
    public ResponseEntity<List<CreditResponse>> findMyCredits() {
        return ResponseEntity.ok(creditService.findMyCredits());
    }

    @PatchMapping("/{creditoId}/approve")
    public ResponseEntity<CreditResponse> approve(@PathVariable UUID creditoId, @Valid @RequestBody CreditApprovalRequest request) {
        return ResponseEntity.ok(creditService.approve(creditoId, request));
    }

    @GetMapping("/{creditoId}/payment-plan")
    public ResponseEntity<List<CuotaResponse>> findPaymentPlan(@PathVariable UUID creditoId) {
        return ResponseEntity.ok(creditService.findPaymentPlan(creditoId));
    }
}
