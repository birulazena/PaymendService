package com.github.birulazena.PaymentService.repository;

import com.github.birulazena.PaymentService.entity.Payment;
import com.github.birulazena.PaymentService.entity.enums.Status;
import org.springframework.data.mongodb.repository.Aggregation;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.math.BigDecimal;
import java.time.Instant;

public interface PaymentRepository extends MongoRepository<Payment, String>, CustomPaymentRepository {

    @Aggregation(pipeline = {
            "{ $match: { user_id: ?0, status: ?3, timestamp: { $gte: ?1, $lte: ?2 } } }",
            "{ $group: { _id: null, total: { $sum: \"$payment_amount\" } } }"
    })
    BigDecimal getTotalSumForUserInRange(Long userId, Instant from, Instant to, Status status);

    @Aggregation(pipeline = {
            "{ $match: { status: ?2, timestamp: { $gte: ?0, $lte: ?1 } } }",
            "{ $group: { _id: null, total: { $sum: \"$payment_amount\" } } }"
    })
    BigDecimal getTotalSumInRange(Instant from, Instant to, Status status);
}
