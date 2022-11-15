package com.backbase.accelerators.payment.client;

import com.backbase.accelerators.payment.config.PaymentSchedulerProperties;
import com.backbase.accelerators.payment.model.PaymentOrderExecutionResponse;
import com.backbase.accelerators.payment.model.ScheduledPaymentOrder;
import com.backbase.payments.scheduled.v1.service.model.ValidateExecutionDateResponse;
import com.backbase.payments.v2.outbound.model.PaymentOrderPutResponseBody;
import com.backbase.payments.v2.outbound.model.PaymentOrdersPostResponseBody;
import com.backbase.payments.v2.service.api.PaymentOrdersApi;
import com.backbase.payments.v2.service.model.Currency;
import com.backbase.payments.v2.service.model.GetPaymentOrderResponse;
import com.backbase.payments.v2.service.model.PaymentOrderPostFilterRequest;
import com.backbase.payments.v2.service.model.PaymentOrderPostFilterResponse;
import com.backbase.payments.v2.service.model.PaymentOrderPutRequest;
import com.backbase.payments.v2.service.model.PaymentOrderPutResponse;
import com.backbase.payments.v2.service.model.SimpleSchedule;
import com.backbase.payments.v2.service.model.SimpleTransaction;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Answers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static com.backbase.payments.v2.service.model.PaymentMode.RECURRING;
import static com.backbase.payments.v2.service.model.SimpleSchedule.EveryEnum._1;
import static com.backbase.payments.v2.service.model.SimpleSchedule.NonWorkingDayExecutionStrategyEnum.NONE;
import static com.backbase.payments.v2.service.model.SimpleSchedule.TransferFrequencyEnum.MONTHLY;
import static com.backbase.payments.v2.service.model.Status.ACCEPTED;
import static com.backbase.payments.v2.service.model.Status.ENTERED;
import static com.backbase.payments.v2.service.model.Status.READY;
import static java.util.Collections.emptyList;
import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class PaymentOrderServiceClientTest {

    @Mock
    private PaymentOrdersApi paymentOrdersApi;

    @Mock
    private ScheduledPaymentOrderServiceClient scheduledPaymentOrderServiceClient;

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private PaymentSchedulerProperties paymentSchedulerProperties;

    @InjectMocks
    private PaymentOrderServiceClient paymentOrderServiceClient;

    @Test
    public void should_return_recurring_payment_orders() {
        when(paymentSchedulerProperties.getQueryFilters().getPaymentTypes()).thenReturn(List.of("ACH_DEBIT", "ACH_CREDIT"));
        when(paymentSchedulerProperties.getQueryFilters().getStatuses()).thenReturn(List.of(READY, ACCEPTED, ENTERED));

        when(paymentOrdersApi.postFilterPaymentOrders(
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                anyInt(),
                anyInt(),
                any(),
                any(),
                any(PaymentOrderPostFilterRequest.class)))
                .thenReturn(paymentOrderPostFilterResponse(1));

        PaymentOrderPostFilterResponse result = paymentOrderServiceClient.getScheduledPaymentOrders(0, 100);
        List<GetPaymentOrderResponse> getPaymentOrderResponse = result.getPaymentOrders();

        assertEquals(1, result.getTotalElements().intValue());
        assertEquals(1, getPaymentOrderResponse.size());

        assertEquals("1", getPaymentOrderResponse.get(0).getId());
        assertEquals(RECURRING, getPaymentOrderResponse.get(0).getPaymentMode());
        assertEquals(LocalDate.parse("2022-11-14"), getPaymentOrderResponse.get(0).getSchedule().getStartDate());
        assertEquals(MONTHLY, getPaymentOrderResponse.get(0).getSchedule().getTransferFrequency());
        assertEquals(NONE, getPaymentOrderResponse.get(0).getSchedule().getNonWorkingDayExecutionStrategy());
        assertEquals(_1, getPaymentOrderResponse.get(0).getSchedule().getEvery());
    }

    @Test
    public void should_update_the_next_execution_date() {
        when(scheduledPaymentOrderServiceClient.validateNextExecutionDate(any()))
                .thenReturn(validateExecutionDateResponse());

        when(paymentOrdersApi.updatePaymentOrder(
                eq("a7d7f3e0-3886-40c0-98ec-c5bcff8d6953"),
                eq("INTERNALID"),
                any(PaymentOrderPutRequest.class)))
                .thenReturn(new PaymentOrderPutResponse().id("a7d7f3e0-3886-40c0-98ec-c5bcff8d6953"));

        paymentOrderServiceClient.updateNextExecutionDate(paymentOrderExecutionResponse());

        verify(scheduledPaymentOrderServiceClient).validateNextExecutionDate(eq(LocalDate.now().plusMonths(1)));
        verify(paymentOrdersApi).updatePaymentOrder(eq("a7d7f3e0-3886-40c0-98ec-c5bcff8d6953"), eq("INTERNALID"),  any(PaymentOrderPutRequest.class));
    }

    private PaymentOrderPostFilterResponse paymentOrderPostFilterResponse(int numberOfElements) {
        if (numberOfElements == 0) {
            return new PaymentOrderPostFilterResponse()
                    .totalElements(new BigDecimal("0"))
                    .paymentOrders(emptyList());
        }

        List<GetPaymentOrderResponse> getPaymentOrderResponseList = new ArrayList<>();
        for (int i = 0; i < numberOfElements; i++) {
            SimpleTransaction simpleTransaction = new SimpleTransaction()
                    .instructedAmount(new Currency().amount("100.00"));

            SimpleSchedule schedule = new SimpleSchedule()
                    .startDate(LocalDate.parse("2022-11-14"))
                    .transferFrequency(MONTHLY)
                    .nonWorkingDayExecutionStrategy(NONE)
                    .on(14)
                    .every(_1);

            GetPaymentOrderResponse getPaymentOrderResponse = new GetPaymentOrderResponse()
                    .id(String.valueOf(i + 1))
                    .paymentMode(RECURRING)
                    .schedule(schedule)
                    .transferTransactionInformation(simpleTransaction);

            getPaymentOrderResponseList.add(getPaymentOrderResponse);
        }

        return new PaymentOrderPostFilterResponse()
                .totalElements(new BigDecimal(numberOfElements))
                .paymentOrders(getPaymentOrderResponseList);
    }

    private ValidateExecutionDateResponse validateExecutionDateResponse() {
        return new ValidateExecutionDateResponse()
                .status(ValidateExecutionDateResponse.StatusEnum.OK)
                .originalExecutionDate(LocalDate.parse("2022-11-14"));
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