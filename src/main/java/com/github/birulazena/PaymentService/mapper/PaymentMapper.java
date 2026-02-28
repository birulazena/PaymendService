package com.github.birulazena.PaymentService.mapper;

import com.github.birulazena.PaymentService.dto.request.PaymentRequestDto;
import com.github.birulazena.PaymentService.dto.response.PaymentResponseDto;
import com.github.birulazena.PaymentService.entity.Payment;
import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING,
        unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface PaymentMapper {

    Payment toEntity(PaymentRequestDto paymentRequestDto);

    PaymentResponseDto toPaymentResponseDto(Payment payment);

}
