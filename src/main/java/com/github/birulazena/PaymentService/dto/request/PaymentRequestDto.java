package com.github.birulazena.PaymentService.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record PaymentRequestDto(@NotNull(message = "Order ID cannot be null")
                                Long orderId,
                                @NotNull(message = "Payment amount cannot be null")
                                @DecimalMin(value = "0.01", message = "Payment amount must be greater that zero")
                                BigDecimal paymentAmount) {
}
