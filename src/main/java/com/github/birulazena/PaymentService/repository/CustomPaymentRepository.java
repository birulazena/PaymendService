package com.github.birulazena.PaymentService.repository;

import com.github.birulazena.PaymentService.entity.Payment;
import com.github.birulazena.PaymentService.filter.PaymentFilter;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface CustomPaymentRepository {

    Page<Payment> findAllByFilter(PaymentFilter filter, Pageable pageable);
}
