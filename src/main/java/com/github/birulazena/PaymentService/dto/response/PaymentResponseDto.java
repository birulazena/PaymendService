package com.github.birulazena.PaymentService.dto.response;

import com.github.birulazena.PaymentService.entity.enums.Status;

import java.math.BigDecimal;
import java.time.Instant;

public record PaymentResponseDto(String id,
                                 Long userId,
                                 Long orderId,
                                 Status status,
                                 Instant timestamp,
                                 BigDecimal paymentAmount) {
}
