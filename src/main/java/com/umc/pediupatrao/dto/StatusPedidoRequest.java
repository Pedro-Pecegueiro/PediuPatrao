package com.umc.pediupatrao.dto;

import com.umc.pediupatrao.entity.StatusPedido;
import jakarta.validation.constraints.NotNull;

public record StatusPedidoRequest(@NotNull StatusPedido status) {
}
