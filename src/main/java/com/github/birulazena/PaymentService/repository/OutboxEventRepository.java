package com.github.birulazena.PaymentService.repository;

import com.github.birulazena.PaymentService.entity.OutboxEvent;
import com.github.birulazena.PaymentService.entity.enums.OutboxStatus;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface OutboxEventRepository extends MongoRepository<OutboxEvent, String> {

    List<OutboxEvent> findByStatusOrderByCreatedAtAsc(OutboxStatus status);

}
