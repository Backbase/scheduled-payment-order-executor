package com.backbase.accelerators.payment.client;

import com.backbase.accelerators.payment.mapper.PaymentOrderMapper;
import com.backbase.accelerators.payment.model.PaymentOrderExecutionResponse;
import com.backbase.accelerators.payment.model.ScheduledPaymentOrder;
import com.backbase.payments.scheduled.v1.service.api.ScheduledPaymentOrderApi;
import com.backbase.payments.scheduled.v1.service.model.PostScheduledPaymentOrderTransactionResponse;
import com.backbase.payments.v2.outbound.model.PaymentOrderPutResponseBody;
import com.backbase.payments.v2.outbound.model.PaymentOrdersPostResponseBody;
import com.backbase.payments.v2.service.model.SimpleSchedule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnitRunner;

import static com.backbase.payments.v2.service.model.SimpleSchedule.EveryEnum._1;
import static com.backbase.payments.v2.service.model.SimpleSchedule.NonWorkingDayExecutionStrategyEnum.NONE;
import static com.backbase.payments.v2.service.model.SimpleSchedule.TransferFrequencyEnum.MONTHLY;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class ScheduledPaymentOrderServiceClientTest {

    @Mock
    private ScheduledPaymentOrderApi scheduledPaymentOrderApi;

    @Spy
    private PaymentOrderMapper paymentOrderMapper;

    @InjectMocks
    private ScheduledPaymentOrderServiceClient scheduledPaymentOrderServiceClient;

    @Test
    public void should_create_scheduled_payment_order_transaction() {
        when(scheduledPaymentOrderApi.postScheduledPaymentOrderTransaction(any()))
                .thenReturn(new PostScheduledPaymentOrderTransactionResponse().id("1"));

        PaymentOrderExecutionResponse result = scheduledPaymentOrderServiceClient.createScheduledPaymentOrderTransaction(
                paymentOrderExecutionResponse());

        verify(paymentOrderMapper).toPostScheduledPaymentOrderTransactionRequest(any());
        verify(scheduledPaymentOrderApi).postScheduledPaymentOrderTransaction(any());
    }

    private PaymentOrderExecutionResponse paymentOrderExecutionResponse() {
        PaymentOrdersPostResponseBody paymentOrdersPostResponseBody = new PaymentOrderPutResponseBody()
                .bankStatus("PROCESSED");

        SimpleSchedule schedule = new SimpleSchedule()
                .transferFrequency(MONTHLY)
                .every(_1)
                .nonWorkingDayExecutionStrategy(NONE);

        ScheduledPaymentOrder scheduledPaymentOrder = new ScheduledPaymentOrder();
        scheduledPaymentOrder.setId("a7d7f3e0-3886-40c0-98ec-c5bcff8d6953");
        scheduledPaymentOrder.setSchedule(schedule);

        PaymentOrderExecutionResponse paymentOrderExecutionResponse = new PaymentOrderExecutionResponse();
        paymentOrderExecutionResponse.setOriginalScheduledPaymentOrder(scheduledPaymentOrder);
        paymentOrderExecutionResponse.setPaymentOrdersPostResponseBody(paymentOrdersPostResponseBody);

        return paymentOrderExecutionResponse;
    }

}