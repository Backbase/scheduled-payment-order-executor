package com.backbase.accelerators.payment.service.impl;

import com.backbase.accelerators.payment.client.PaymentOrderOutboundClient;
import com.backbase.accelerators.payment.client.PaymentOrderServiceClient;
import com.backbase.accelerators.payment.client.ScheduledPaymentOrderServiceClient;
import com.backbase.accelerators.payment.config.PaymentSchedulerProperties;
import com.backbase.accelerators.payment.event.EventEmitter;
import com.backbase.accelerators.payment.iterator.ScheduledPaymentOrderIterator;
import com.backbase.accelerators.payment.mapper.PaymentOrderMapper;
import com.backbase.accelerators.payment.model.PaymentOrderExecutionResponse;
import com.backbase.accelerators.payment.model.ScheduledPaymentOrder;
import com.backbase.accelerators.payment.service.ScheduledPaymentExecutorService;
import com.backbase.accelerators.payment.util.ScheduledPaymentUtil;
import com.backbase.payments.v2.outbound.model.PaymentOrdersPostRequestBody;
import com.backbase.payments.v2.outbound.model.PaymentOrdersPostResponseBody;
import com.backbase.payments.v2.service.model.GetPaymentOrderResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ScheduledPaymentExecutorServiceImpl implements ScheduledPaymentExecutorService {

    private final PaymentOrderServiceClient paymentOrderServiceClient;
    private final PaymentOrderOutboundClient paymentOrderOutboundClient;
    private final ScheduledPaymentOrderServiceClient scheduledPaymentOrderServiceClient;
    private final PaymentSchedulerProperties paymentSchedulerProperties;
    private final PaymentOrderMapper paymentOrderMapper;
    private final EventEmitter eventEmitter;

    @Override
    @Scheduled(cron = "${payment-scheduler.cron-expression}")
    public void execute() {
        log.info("Entering ScheduledPaymentExecutorServiceImpl.execute()");
        ScheduledPaymentOrderIterator iterator = initializeScheduledPaymentOrderIterator();

        while (iterator.hasNext()) {
            List<GetPaymentOrderResponse> getPaymentOrderResponseList = iterator.next();
            log.info("Iterator retrieved batch of {} scheduled payment orders", getPaymentOrderResponseList.size());
            executePayments(getPaymentOrderResponseList);
        }
    }

    private void executePayments(List<GetPaymentOrderResponse> getPaymentOrderResponseList) {
        List<ScheduledPaymentOrder> scheduledPaymentOrders = getPaymentOrderResponseList.parallelStream()
                .map(paymentOrderMapper::toScheduledPaymentOrder)
                .filter(ScheduledPaymentUtil::isPaymentScheduledForToday)
                .filter(Predicate.not(ScheduledPaymentUtil::isEndDateInThePast))
                .filter(Predicate.not(ScheduledPaymentUtil::isRepeatCountMet))
                .collect(Collectors.toList());

        scheduledPaymentOrders.parallelStream()
                .map(this::executePayment)
                .map(scheduledPaymentOrderServiceClient::createScheduledPaymentOrderTransaction)
                .forEach(paymentOrderServiceClient::updateNextExecutionDate);
    }

    private PaymentOrderExecutionResponse executePayment(ScheduledPaymentOrder scheduledPaymentOrder) {
        PaymentOrdersPostRequestBody request = paymentOrderMapper.toOutBoundRequest(scheduledPaymentOrder);
        PaymentOrdersPostResponseBody response = paymentOrderOutboundClient.sendToPaymentOrderOutboundService(request);
        log.info("Response from payment order outbound service: {}", response);

        PaymentOrderExecutionResponse paymentOrderExecutionResponse = new PaymentOrderExecutionResponse();
        paymentOrderExecutionResponse.setOriginalScheduledPaymentOrder(scheduledPaymentOrder);
        paymentOrderExecutionResponse.setPaymentOrdersPostResponseBody(response);

        return paymentOrderExecutionResponse;
    }

    private ScheduledPaymentOrderIterator initializeScheduledPaymentOrderIterator() {
        return new ScheduledPaymentOrderIterator(
                paymentOrderServiceClient,
                paymentSchedulerProperties);
    }
}
