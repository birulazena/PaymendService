package com.github.birulazena.PaymentService.dto.response;

import java.math.BigDecimal;

public record UserTotalSumResponseDto(Long userId,
                                      BigDecimal totalSum) {
}
