package com.github.birulazena.PaymentService.repository;

import com.github.birulazena.PaymentService.entity.Payment;
import com.github.birulazena.PaymentService.filter.PaymentFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.support.PageableExecutionUtils;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;

@Repository
@RequiredArgsConstructor
public class CustomPaymentRepositoryImpl implements CustomPaymentRepository{

    private final MongoTemplate mongoTemplate;

    @Override
    public Page<Payment> findAllByFilter(PaymentFilter filter, Pageable pageable) {
        Query query = new Query().with(pageable);

        if(filter != null) {
            List<Criteria> orCriteriaList = new ArrayList<>();

            if(filter.userId() != null) {
                orCriteriaList.add(Criteria.where("userId").is(filter.userId()));
            }
            if(filter.orderId() != null) {
                orCriteriaList.add(Criteria.where("orderId").is(filter.orderId()));
            }
            if(filter.status() != null) {
                orCriteriaList.add(Criteria.where("status").is(filter.status()));
            }

            if (!orCriteriaList.isEmpty()) {
                Criteria criteria = new Criteria();
                criteria.andOperator(orCriteriaList.toArray(Criteria[]::new));
                query.addCriteria(criteria);
            }
        }

        long count = mongoTemplate.count(Query.of(query).limit(-1).skip(-1), Payment.class);
        List<Payment> payments = mongoTemplate.find(query, Payment.class);

        return PageableExecutionUtils.getPage(payments, pageable, () -> count);
    }
}
