package com.github.birulazena.PaymentService.unit;

import com.github.birulazena.PaymentService.client.ExternalRandomNumberClient;
import com.github.birulazena.PaymentService.client.dto.RandomNumberDto;
import com.github.birulazena.PaymentService.dto.request.PaymentRequestDto;
import com.github.birulazena.PaymentService.dto.response.GlobalTotalSumResponseDto;
import com.github.birulazena.PaymentService.dto.response.PaymentResponseDto;
import com.github.birulazena.PaymentService.dto.response.UserTotalSumResponseDto;
import com.github.birulazena.PaymentService.dto.response.event.PaymentEventDto;
import com.github.birulazena.PaymentService.entity.Payment;
import com.github.birulazena.PaymentService.entity.enums.Status;
import com.github.birulazena.PaymentService.exception.ExternalNotFoundException;
import com.github.birulazena.PaymentService.filter.PaymentFilter;
import com.github.birulazena.PaymentService.mapper.PaymentMapper;
import com.github.birulazena.PaymentService.repository.OutboxEventRepository;
import com.github.birulazena.PaymentService.repository.PaymentRepository;
import com.github.birulazena.PaymentService.service.PaymentService;
import com.github.birulazena.PaymentService.util.DateTestFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import tools.jackson.databind.ObjectMapper;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;

@ExtendWith(MockitoExtension.class)
public class PaymentServiceUnit {

    @Mock
    private ObjectMapper objectMapper;

    @Mock
    private OutboxEventRepository outboxEventRepository;

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private ExternalRandomNumberClient client;

    @Mock
    private PaymentMapper paymentMapper;

    @InjectMocks
    private PaymentService paymentService;

    @Test
    void createPaymentSuccessPaymentExistTest() {
        Long userId = 1L;
        PaymentRequestDto paymentRequestDto = DateTestFactory.samePaymentRequestDto();
        Payment payment = DateTestFactory.samePayment();
        PaymentResponseDto paymentResponseDto = DateTestFactory.samePaymentResponseDto();

        Page<Payment> page = new PageImpl<>(List.of(payment));

        Mockito.when(paymentRepository.findAllByFilter(
                new PaymentFilter(userId, paymentRequestDto.orderId(), Status.SUCCESS),
                PageRequest.of(0, 1)
        )).thenReturn(page);

        Mockito.when(paymentMapper.toPaymentResponseDto(payment)).thenReturn(paymentResponseDto);

        PaymentResponseDto result = paymentService.createPayment(userId, paymentRequestDto);

        assertNotNull(result);
        assertEquals(paymentResponseDto,result);

        Mockito.verify(paymentMapper, Mockito.never()).toEntity(any());
        Mockito.verify(client, Mockito.never()).getRandomNumber();
        Mockito.verify(paymentRepository, Mockito.never()).save(any());
    }

    @Test
    void createPaymentClientNotFoundTest() {
        Long userId = 1L;
        PaymentRequestDto paymentRequestDto = DateTestFactory.samePaymentRequestDto();
        Payment payment = DateTestFactory.newPayment();

        Page<Payment> page = new PageImpl<>(List.of());

        Mockito.when(paymentRepository.findAllByFilter(
                new PaymentFilter(userId, paymentRequestDto.orderId(), Status.SUCCESS),
                PageRequest.of(0, 1)
        )).thenReturn(page);
        Mockito.when(paymentMapper.toEntity(paymentRequestDto)).thenReturn(payment);
        Mockito.when(client.getRandomNumber()).thenReturn(new RandomNumberDto(null));

        assertThrows(ExternalNotFoundException.class,
                () -> paymentService.createPayment(userId, paymentRequestDto));
    }

    @Test
    void createPaymentSuccessPaymentTest() {
        Long userId = 1L;
        PaymentRequestDto paymentRequestDto = DateTestFactory.samePaymentRequestDto();
        Payment payment = DateTestFactory.newPayment();
        Payment savePayment = DateTestFactory.samePayment();
        PaymentEventDto paymentEventDto = DateTestFactory.paymentEventDto();
        PaymentResponseDto paymentResponseDto = DateTestFactory.samePaymentResponseDto();
        String expectedJsonPayload = "{\"id\":1,\"orderId\":1,\"status\":\"SUCCESS\",\"amount\":100,\"createdAt\":\"2026-02-19T12:12:12Z\"}";

        Page<Payment> page = new PageImpl<>(List.of());

        Mockito.when(paymentRepository.findAllByFilter(
                new PaymentFilter(userId, paymentRequestDto.orderId(), Status.SUCCESS),
                PageRequest.of(0, 1)
        )).thenReturn(page);
        Mockito.when(paymentMapper.toEntity(paymentRequestDto)).thenReturn(payment);
        Mockito.when(client.getRandomNumber()).thenReturn(new RandomNumberDto(2));
        Mockito.when(paymentRepository.save(payment)).thenReturn(savePayment);

        Mockito.when(paymentMapper.toPaymentEventDto(savePayment)).thenReturn(paymentEventDto);
        Mockito.when(objectMapper.writeValueAsString(paymentEventDto)).thenReturn(expectedJsonPayload);
        Mockito.when(paymentMapper.toPaymentResponseDto(savePayment)).thenReturn(paymentResponseDto);

        PaymentResponseDto result = paymentService.createPayment(userId, paymentRequestDto);

        assertEquals(paymentResponseDto, result);
    }

    @Test
    void getAllByFilterTest() {
        PaymentFilter paymentFilter = new PaymentFilter(1L, 1L, null);
        Pageable pageable = PageRequest.of(0, 10);

        Payment payment1 = DateTestFactory.samePayment();
        Payment payment2 = DateTestFactory.samePayment();

        PaymentResponseDto paymentResponseDto1 = DateTestFactory.samePaymentResponseDto();
        PaymentResponseDto paymentResponseDto2 = DateTestFactory.samePaymentResponseDto();

        Page<Payment> page = new PageImpl<>(List.of(payment1, payment2), pageable, 1);

        Mockito.when(paymentRepository.findAllByFilter(paymentFilter, pageable)).thenReturn(page);
        Mockito.when(paymentMapper.toPaymentResponseDto(payment1)).thenReturn(paymentResponseDto1);
        Mockito.when(paymentMapper.toPaymentResponseDto(payment2)).thenReturn(paymentResponseDto2);

        Page<PaymentResponseDto> result = paymentService.getAllByFilter(paymentFilter, pageable);

        assertEquals(paymentResponseDto1, result.getContent().get(0));
        assertEquals(paymentResponseDto1, result.getContent().get(1));
        assertEquals(2, result.getTotalElements());
    }

    @Test
    void getUserTotalSumTest() {
        Long userId = 1L;
        Instant from = Instant.parse("2026-02-18T12:12:12Z");
        Instant to = Instant.parse("2026-02-20T12:12:12Z");
        BigDecimal expectedSum = BigDecimal.valueOf(200);

        Mockito.when(paymentRepository.getTotalSumForUserInRange(userId, from, to, Status.SUCCESS))
                .thenReturn(expectedSum);

        UserTotalSumResponseDto result = paymentService.getUserTotalSum(userId, from, to);

        assertNotNull(result);
        assertEquals(userId, result.userId());
        assertEquals(expectedSum, result.totalSum());
    }

    @Test
    void getGlobalTotalSumTest() {
        Instant from = Instant.parse("2026-02-18T12:12:12Z");
        Instant to = Instant.parse("2026-02-20T12:12:12Z");
        BigDecimal expectedSum = BigDecimal.valueOf(200);

        Mockito.when(paymentRepository.getTotalSumInRange(from, to, Status.SUCCESS))
                .thenReturn(expectedSum);

        GlobalTotalSumResponseDto result = paymentService.getGlobalTotalSum(from, to);

        assertNotNull(result);
        assertEquals(expectedSum, result.totalSum());
    }

}
