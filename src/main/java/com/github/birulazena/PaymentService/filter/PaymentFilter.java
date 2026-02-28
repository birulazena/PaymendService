package com.github.birulazena.PaymentService.filter;

import com.github.birulazena.PaymentService.entity.enums.Status;

public record PaymentFilter(Long userId,
                            Long orderId,
                            Status status) {
}
