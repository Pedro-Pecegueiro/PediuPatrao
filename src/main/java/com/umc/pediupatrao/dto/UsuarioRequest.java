package com.umc.pediupatrao.dto;

import com.umc.pediupatrao.entity.PerfilUsuario;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record UsuarioRequest(
        @NotBlank @Pattern(regexp = "^[a-zA-Z0-9._-]{3,40}$") String username,
        @NotBlank @Size(min = 8, max = 72) String password,
        @NotNull PerfilUsuario role,
        Boolean ativo
) {
}
