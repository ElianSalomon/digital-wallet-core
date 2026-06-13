package com.elian.wallet.services;

import com.elian.wallet.dto.CreditApprovalRequest;
import com.elian.wallet.dto.CreditRequest;
import com.elian.wallet.dto.CreditResponse;
import com.elian.wallet.dto.CuotaResponse;

import java.util.List;
import java.util.UUID;

public interface CreditService {
    CreditResponse requestCredit(CreditRequest request);
    List<CreditResponse> findMyCredits();
    CreditResponse approve(UUID creditoId, CreditApprovalRequest request);
    List<CuotaResponse> findPaymentPlan(UUID creditoId);
}
