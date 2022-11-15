package com.backbase.accelerators.payment.client;

import com.backbase.accelerators.payment.mapper.PaymentOrderMapper;
import com.backbase.accelerators.payment.model.PaymentOrderExecutionResponse;
import com.backbase.payments.scheduled.v1.service.api.ScheduledPaymentOrderApi;
import com.backbase.payments.scheduled.v1.service.model.PostScheduledPaymentOrderTransactionResponse;
import com.backbase.payments.scheduled.v1.service.model.ValidateExecutionDateResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

@Slf4j
@Component
@RequiredArgsConstructor
public class ScheduledPaymentOrderServiceClient {

    private final ScheduledPaymentOrderApi scheduledPaymentOrderApi;
    private final PaymentOrderMapper paymentOrderMapper;

    public PaymentOrderExecutionResponse createScheduledPaymentOrderTransaction(
            PaymentOrderExecutionResponse paymentOrderExecutionResponse) {

        log.info("Creating scheduled payment order transaction record for scheduled payment order: {}",
                paymentOrderExecutionResponse.getOriginalScheduledPaymentOrder().getId());

        PostScheduledPaymentOrderTransactionResponse response = scheduledPaymentOrderApi.postScheduledPaymentOrderTransaction(
                paymentOrderMapper.toPostScheduledPaymentOrderTransactionRequest(paymentOrderExecutionResponse));

        log.info("Scheduled payment order transaction created. TransactionId: {}", response.getId());
        return paymentOrderExecutionResponse;
    }

    public ValidateExecutionDateResponse validateNextExecutionDate(LocalDate executionDate) {
        ValidateExecutionDateResponse response = scheduledPaymentOrderApi.validateExecutionDate(executionDate);
        log.info("ValidateExecutionDateResponse: {}", response);

        return response;
    }
}
