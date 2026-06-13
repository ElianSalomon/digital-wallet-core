package com.elian.wallet.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record RegisterRequest(
        @NotBlank @Size(max = 100) String nombre,
        @NotBlank @Size(max = 100) String apellido,
        @NotBlank @Email @Size(max = 255) String email,
        @NotBlank @Size(min = 8, max = 80) String password,
        @NotBlank @Size(max = 30) String telefono,
        @NotBlank @Size(max = 50) String documentoIdentidad,
        @NotNull @Past LocalDate fechaNacimiento
) {
}
