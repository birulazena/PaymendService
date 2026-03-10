package com.github.birulazena.PaymentService.entity;

import com.github.birulazena.PaymentService.entity.enums.EventType;
import com.github.birulazena.PaymentService.entity.enums.OutboxStatus;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.time.Instant;

@Document(collection = "outbox_events")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class OutboxEvent {

    @Id
    private String id;

    @Field("event_type")
    private EventType eventType;

    @Field("aggregate_id")
    private String aggregateId;

    private String payload;

    private OutboxStatus status = OutboxStatus.PENDING;

    @CreatedDate
    @Field("created_at")
    private Instant createdAt;
}
