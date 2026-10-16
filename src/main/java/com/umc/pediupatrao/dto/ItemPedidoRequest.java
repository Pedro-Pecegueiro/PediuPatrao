package com.umc.pediupatrao.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

public record ItemPedidoRequest(
        @NotBlank String produtoId,
        @Min(1) int quantidade
) {
}
