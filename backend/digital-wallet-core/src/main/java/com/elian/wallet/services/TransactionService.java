package com.elian.wallet.services;

import com.elian.wallet.dto.TransactionRequest;
import com.elian.wallet.dto.TransactionResponse;

public interface TransactionService {
    TransactionResponse deposit(TransactionRequest request);
    TransactionResponse withdraw(TransactionRequest request);
    TransactionResponse transfer(TransactionRequest request);
}
