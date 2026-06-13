package com.elian.wallet.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record WalletRequest(
        @NotBlank @Size(max = 80) String alias,
        @NotBlank @Pattern(regexp = "^[A-Z]{3}$") String moneda
) {
}
