package com.elian.wallet.dto;

import java.util.UUID;

public record AuthResponse(
        String token,
        String tokenType,
        UUID usuarioId,
        String email,
        String rol
) {
}
