package com.backbase.accelerators.payment.client;

import com.backbase.accelerators.payment.config.PaymentSchedulerProperties;
import com.backbase.accelerators.payment.model.ScheduledPaymentOrder;
import com.backbase.limit.v2.service.api.LimitsServiceApi;
import com.backbase.limit.v2.service.model.LimitsCheckPostRequestBody;
import com.backbase.limit.v2.service.model.LimitsCheckPostResponseBody;
import com.backbase.limit.v2.service.model.PaymentState;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Slf4j
@Component
@RequiredArgsConstructor
public class LimitServiceClient {

    private final LimitsServiceApi limitsServiceApi;
    private final PaymentSchedulerProperties paymentSchedulerProperties;

    public ScheduledPaymentOrder checkLimitsConsumption(ScheduledPaymentOrder scheduledPaymentOrder) {

        if (!paymentSchedulerProperties.isLimitChecksEnabled()) {
            log.info("Limits checks disabled");
            return scheduledPaymentOrder;
        }

        LimitsCheckPostRequestBody limitsCheckPostRequestBody = new LimitsCheckPostRequestBody();
        limitsCheckPostRequestBody.setAmount(new BigDecimal(scheduledPaymentOrder.getTransferTransactionInformation().getInstructedAmount().getAmount()));
        limitsCheckPostRequestBody.setCurrency(scheduledPaymentOrder.getTransferTransactionInformation().getInstructedAmount().getCurrencyCode());
        limitsCheckPostRequestBody.setPaymentType(scheduledPaymentOrder.getPaymentType());
        limitsCheckPostRequestBody.setServiceAgreementId(scheduledPaymentOrder.getServiceAgreementId());
        limitsCheckPostRequestBody.setArrangementId(scheduledPaymentOrder.getOriginatorAccount().getArrangementId());
        limitsCheckPostRequestBody.setState(PaymentState.NEW);

        LimitsCheckPostResponseBody limitsCheckPostResponseBody = limitsServiceApi.postLimitsCheck(limitsCheckPostRequestBody);

        return scheduledPaymentOrder;
    }
}
