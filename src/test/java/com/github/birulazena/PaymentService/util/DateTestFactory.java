package com.github.birulazena.PaymentService.util;

import com.github.birulazena.PaymentService.dto.request.PaymentRequestDto;
import com.github.birulazena.PaymentService.dto.response.PaymentResponseDto;
import com.github.birulazena.PaymentService.dto.response.event.PaymentEventDto;
import com.github.birulazena.PaymentService.entity.Payment;
import com.github.birulazena.PaymentService.entity.enums.Status;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public class DateTestFactory {

    public static Payment samePayment() {
        return new Payment("1234567890",
                1L,
                1L,
                Status.SUCCESS,
                Instant.parse("2026-02-19T12:12:12Z"),
                BigDecimal.valueOf(100));
    }

    public static Payment newPayment() {
        return new Payment(null,
                1L,
                1L,
                Status.SUCCESS,
                Instant.parse("2026-02-19T12:12:12Z"),
                BigDecimal.valueOf(100));
    }

    public static PaymentRequestDto samePaymentRequestDto() {
        return new PaymentRequestDto(1L,
                BigDecimal.valueOf(100));
    }

    public static PaymentResponseDto samePaymentResponseDto() {
        return new PaymentResponseDto("1234567890",
                1L,
                1L,
                Status.SUCCESS,
                Instant.parse("2026-02-19T12:12:12Z"),
                BigDecimal.valueOf(100));
    }

    public static PaymentEventDto paymentEventDto() {
        return new PaymentEventDto(1L,
                1L,
                Status.SUCCESS,
                BigDecimal.valueOf(100),
                Instant.parse("2026-02-19T12:12:12Z"));
    }

}
