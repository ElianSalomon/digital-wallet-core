package com.elian.wallet.services;

import com.elian.wallet.dto.AuthResponse;
import com.elian.wallet.dto.LoginRequest;
import com.elian.wallet.dto.RegisterRequest;

public interface AuthService {
    AuthResponse register(RegisterRequest request);
    AuthResponse login(LoginRequest request);
}
