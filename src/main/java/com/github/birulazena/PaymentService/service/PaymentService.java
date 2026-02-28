package com.github.birulazena.PaymentService.service;

import com.github.birulazena.PaymentService.client.ExternalRandomNumberClient;
import com.github.birulazena.PaymentService.client.dto.RandomNumberDto;
import com.github.birulazena.PaymentService.dto.request.PaymentRequestDto;
import com.github.birulazena.PaymentService.dto.response.GlobalTotalSumResponseDto;
import com.github.birulazena.PaymentService.dto.response.PaymentResponseDto;
import com.github.birulazena.PaymentService.dto.response.UserTotalSumResponseDto;
import com.github.birulazena.PaymentService.entity.Payment;
import com.github.birulazena.PaymentService.entity.enums.Status;
import com.github.birulazena.PaymentService.exception.ExternalNotFoundException;
import com.github.birulazena.PaymentService.filter.PaymentFilter;
import com.github.birulazena.PaymentService.mapper.PaymentMapper;
import com.github.birulazena.PaymentService.repository.PaymentRepository;
import com.github.birulazena.PaymentService.util.NumberUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class PaymentService {

    private final PaymentRepository paymentRepository;

    private final ExternalRandomNumberClient client;

    private final PaymentMapper paymentMapper;

    public PaymentResponseDto createPayment(Long userId, PaymentRequestDto paymentRequestDto) {
        Optional<Payment> successPayment = paymentRepository.findAllByFilter(
                new PaymentFilter(userId, paymentRequestDto.orderId(), Status.SUCCESS),
                PageRequest.of(0, 1)
        ).getContent().stream().findFirst();

        if (successPayment.isPresent()) {
            return paymentMapper.toPaymentResponseDto(successPayment.get());
        }

        Payment payment = paymentMapper.toEntity(paymentRequestDto);
        payment.setUserId(userId);

        RandomNumberDto randomNumberDto = client.getRandomNumber();

        if(randomNumberDto.number() == null)
            throw new ExternalNotFoundException("Random number client not found");

        if(NumberUtils.isEven(randomNumberDto.number())) {
            payment.setStatus(Status.SUCCESS);
        } else {
            payment.setStatus(Status.FAILED);
        }

        Payment savePayment = paymentRepository.save(payment);

        return paymentMapper.toPaymentResponseDto(savePayment);
    }

    public Page<PaymentResponseDto> getAllByFilter(PaymentFilter paymentFilter, Pageable pageable) {
        return paymentRepository.findAllByFilter(paymentFilter, pageable)
                .map(payment -> paymentMapper.toPaymentResponseDto(payment));
    }

    public UserTotalSumResponseDto getUserTotalSum(Long userId, Instant from, Instant to) {
        BigDecimal sum = paymentRepository.getTotalSumForUserInRange(userId, from, to, Status.SUCCESS);
        return new UserTotalSumResponseDto(userId, sum);
    }

    public GlobalTotalSumResponseDto getGlobalTotalSum(Instant from, Instant to) {
        BigDecimal sum = paymentRepository.getTotalSumInRange(from, to, Status.SUCCESS);
        return new GlobalTotalSumResponseDto(sum);
    }



}
