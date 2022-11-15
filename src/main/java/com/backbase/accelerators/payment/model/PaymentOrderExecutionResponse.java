package com.backbase.accelerators.payment.model;

import lombok.Data;
import com.backbase.payments.v2.outbound.model.PaymentOrdersPostResponseBody;

@Data
public class PaymentOrderExecutionResponse {

    private ScheduledPaymentOrder originalScheduledPaymentOrder;
    private PaymentOrdersPostResponseBody paymentOrdersPostResponseBody;

}
