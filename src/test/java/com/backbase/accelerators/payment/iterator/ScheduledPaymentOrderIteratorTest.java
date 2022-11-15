package com.backbase.accelerators.payment.iterator;

import com.backbase.accelerators.payment.client.PaymentOrderServiceClient;
import com.backbase.accelerators.payment.config.PaymentSchedulerProperties;
import com.backbase.payments.v2.service.model.GetPaymentOrderResponse;
import com.backbase.payments.v2.service.model.PaymentOrderPostFilterResponse;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Answers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import static java.util.Collections.emptyList;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class ScheduledPaymentOrderIteratorTest {

    @Mock
    private PaymentOrderServiceClient paymentOrderServiceClient;

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private PaymentSchedulerProperties paymentSchedulerProperties;

    @InjectMocks
    private ScheduledPaymentOrderIterator scheduledPaymentOrderIterator;

    @Test
    public void should_return_scheduled_payment_orders() {
        when(paymentOrderServiceClient.getScheduledPaymentOrders(anyInt(), anyInt()))
                .thenReturn(paymentOrderPostFilterResponse(10))
                .thenReturn(paymentOrderPostFilterResponse(0));

        when(paymentSchedulerProperties.getIteratorProperties().getPageSize()).thenReturn(5);

        List<GetPaymentOrderResponse> getPaymentOrderResponseList = new ArrayList<>();
        while (scheduledPaymentOrderIterator.hasNext()) {
            getPaymentOrderResponseList.addAll(scheduledPaymentOrderIterator.next());
        }

        System.out.println(getPaymentOrderResponseList);
    }

    private PaymentOrderPostFilterResponse paymentOrderPostFilterResponse(int numberOfElements) {
        if (numberOfElements == 0) {
            return new PaymentOrderPostFilterResponse()
                    .totalElements(new BigDecimal("0"))
                    .paymentOrders(emptyList());
        }

        List<GetPaymentOrderResponse> getPaymentOrderResponseList = new ArrayList<>();
        for (int i = 0; i < numberOfElements; i++) {
            getPaymentOrderResponseList.add(new GetPaymentOrderResponse().id(String.valueOf(i + 1)));
        }

        return new PaymentOrderPostFilterResponse()
                .totalElements(new BigDecimal(numberOfElements))
                .paymentOrders(getPaymentOrderResponseList);
    }

}