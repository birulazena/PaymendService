package com.github.birulazena.PaymentService.dto.response.event;

import com.github.birulazena.PaymentService.entity.enums.Status;

import java.math.BigDecimal;
import java.time.Instant;

public record PaymentEventDto(Long orderId,
                              Long userId,
                              Status status,
                              BigDecimal paymentAmount,
                              Instant timestamp) {
}
