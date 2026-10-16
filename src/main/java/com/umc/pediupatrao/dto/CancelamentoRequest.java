package com.umc.pediupatrao.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CancelamentoRequest(
        @NotBlank @Size(min = 5, max = 500) String justificativa
) {
}
