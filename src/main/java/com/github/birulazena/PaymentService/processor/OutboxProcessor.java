package com.github.birulazena.PaymentService.processor;

import com.github.birulazena.PaymentService.entity.OutboxEvent;
import com.github.birulazena.PaymentService.entity.enums.OutboxStatus;
import com.github.birulazena.PaymentService.repository.OutboxEventRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class OutboxProcessor {

    private final OutboxEventRepository outboxEventRepository;

    private final KafkaTemplate<String, String> kafkaTemplate;

    @Value("${app.kafka.topics.payment-events.name}")
    private String paymentEventsTopic;

    @Scheduled(fixedDelay = 1000)
    public void processorOutbox() {
        List<OutboxEvent> events = outboxEventRepository.findByStatusOrderByCreatedAtAsc(OutboxStatus.PENDING);

        if(events.isEmpty())
            return;

        for(OutboxEvent event : events) {
            kafkaTemplate.send(paymentEventsTopic, event.getAggregateId(), event.getPayload())
                    .whenComplete((result, ex) -> {
                        if(ex == null) {
                            event.setStatus(OutboxStatus.SENT);
                            outboxEventRepository.save(event);
                        }
                    });
        }
    }

}
