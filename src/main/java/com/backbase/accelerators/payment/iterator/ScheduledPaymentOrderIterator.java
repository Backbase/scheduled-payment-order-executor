package com.backbase.accelerators.payment.iterator;

import com.backbase.accelerators.payment.client.PaymentOrderServiceClient;
import com.backbase.accelerators.payment.config.PaymentSchedulerProperties;
import com.backbase.payments.v2.service.model.GetPaymentOrderResponse;
import com.backbase.payments.v2.service.model.PaymentOrderPostFilterResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.math.BigDecimal;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;

import static java.math.BigDecimal.ZERO;
import static java.util.Collections.emptyList;

@Slf4j
@RequiredArgsConstructor
public class ScheduledPaymentOrderIterator implements Iterator<List<GetPaymentOrderResponse>> {

    private final PaymentOrderServiceClient paymentOrderServiceClient;
    private final PaymentSchedulerProperties paymentSchedulerProperties;

    private boolean hasMoreElements = true;
    private BigDecimal totalElements;
    private int currentPosition = 0;

    @Override
    public boolean hasNext() {
        return hasMoreElements;
    }

    @Override
    public List<GetPaymentOrderResponse> next() {
        if (!hasNext()) {
            throw new NoSuchElementException("No more scheduled payment orders available");
        }

        PaymentOrderPostFilterResponse response = paymentOrderServiceClient.getScheduledPaymentOrders(
                currentPosition,
                paymentSchedulerProperties.getIteratorProperties().getPageSize());

        if (response.getTotalElements().equals(ZERO) || response.getPaymentOrders().isEmpty()) {
            log.info("No more scheduled payment orders available");
            hasMoreElements = false;
            return emptyList();
        }

        totalElements = response.getTotalElements();
        ++currentPosition;

        return response.getPaymentOrders();
    }
}
