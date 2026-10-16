package com.umc.pediupatrao.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record DescontoRequest(
        @NotNull @DecimalMin("0.01") @DecimalMax("20.00") BigDecimal percentual
) {
}
