package com.github.birulazena.PaymentService.dto.response.error;

import java.util.Map;

public record ValidationErrorResponse(String message,
                                      Map<String, String> errors) {
}
