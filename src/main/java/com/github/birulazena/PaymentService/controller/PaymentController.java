package com.github.birulazena.PaymentService.controller;

import com.github.birulazena.PaymentService.dto.request.PaymentRequestDto;
import com.github.birulazena.PaymentService.dto.response.GlobalTotalSumResponseDto;
import com.github.birulazena.PaymentService.dto.response.PaymentResponseDto;
import com.github.birulazena.PaymentService.dto.response.UserTotalSumResponseDto;
import com.github.birulazena.PaymentService.filter.PaymentFilter;
import com.github.birulazena.PaymentService.service.PaymentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.CurrentSecurityContext;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;

@RestController
@RequestMapping("api/v1/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;

    @PostMapping
    public ResponseEntity<PaymentResponseDto> createPayment(@Valid @RequestBody
                                                            PaymentRequestDto paymentRequestDto,
                                                            @CurrentSecurityContext(expression = "authentication.details['userId']")
                                                            Long userId) {
        PaymentResponseDto paymentResponseDto = paymentService.createPayment(userId, paymentRequestDto);
        return ResponseEntity.status(HttpStatus.CREATED).body(paymentResponseDto);
    }

    @PreAuthorize("#paymentFilter.userId() == authentication.details['userId'] or hasRole('ADMIN')")
    @GetMapping
    public ResponseEntity<Page<PaymentResponseDto>> getAllByFilter(PaymentFilter paymentFilter, Pageable pageable) {
        Page<PaymentResponseDto> page = paymentService.getAllByFilter(paymentFilter, pageable);
        return ResponseEntity.ok(page);
    }

    @PreAuthorize("#userId == authentication.details['userId'] or hasRole('ADMIN')")
    @GetMapping("/stats/users/{userId}")
    public ResponseEntity<UserTotalSumResponseDto> getUserTotalSum(@PathVariable Long userId,
                                                                   @RequestParam Instant from,
                                                                   @RequestParam Instant to) {
        UserTotalSumResponseDto userTotalSumResponseDto = paymentService.getUserTotalSum(userId, from, to);
        return ResponseEntity.ok(userTotalSumResponseDto);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/stats/total")
    public ResponseEntity<GlobalTotalSumResponseDto> getGlobalTotalSum(@RequestParam Instant from,
                                                                       @RequestParam Instant to) {
        GlobalTotalSumResponseDto globalTotalSumResponseDto = paymentService.getGlobalTotalSum(from, to);
        return ResponseEntity.ok(globalTotalSumResponseDto);
    }



}
