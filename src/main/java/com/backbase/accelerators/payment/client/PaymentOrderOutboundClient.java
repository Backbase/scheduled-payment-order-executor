package com.backbase.accelerators.payment.client;

import com.backbase.accelerators.payment.config.PaymentSchedulerProperties;
import com.backbase.accelerators.payment.exception.RetryablePaymentOrderException;
import com.backbase.payments.v2.outbound.api.PaymentOrderIntegrationOutboundApi;
import com.backbase.payments.v2.outbound.model.PaymentOrdersPostRequestBody;
import com.backbase.payments.v2.outbound.model.PaymentOrdersPostResponseBody;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Component;

import static com.backbase.payments.v2.service.model.Status.REJECTED;
import static java.util.Objects.nonNull;
import static org.apache.commons.lang3.StringUtils.isNotBlank;

@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentOrderOutboundClient {

    private final PaymentOrderIntegrationOutboundApi paymentOrderIntegrationOutboundApi;
    private final PaymentSchedulerProperties paymentSchedulerProperties;

    @Retryable(
            value = RetryablePaymentOrderException.class,
            maxAttemptsExpression = "${payment-scheduler.retry-max-attempts}",
            backoff = @Backoff(delayExpression = "${payment-scheduler.retry-backoff-delay-millis}"))
    public PaymentOrdersPostResponseBody sendToPaymentOrderOutboundService(
            PaymentOrdersPostRequestBody paymentOrdersPostRequestBody) {

        PaymentOrdersPostResponseBody response;
        try {
            log.info("Invoking payment order outbound service with request: {}", paymentOrdersPostRequestBody);
            response = paymentOrderIntegrationOutboundApi.postPaymentOrders(paymentOrdersPostRequestBody);
            validateResponse(response);

            return response;
        } catch (Exception e) {
            log.error("Error occurred invoking payment order outbound service: {}", e.getMessage());
            return handleException(e);
        }
    }

    @Recover
    public PaymentOrdersPostResponseBody recover(
            RetryablePaymentOrderException e,
            PaymentOrdersPostRequestBody paymentOrdersPostRequestBody) {

        /* Per Spring Retry, this fallback method is triggered
           when a Payment Order submission fails after max try attempts exceeded. */
        log.info("Entering retry recovery method with payment order: {}", paymentOrdersPostRequestBody.getId());

        return new PaymentOrdersPostResponseBody()
                .bankStatus(REJECTED.getValue())
                .reasonText("Could not process scheduled payment")
                .errorDescription(e.getMessage());
    }

    private void validateResponse(PaymentOrdersPostResponseBody response) {
        if (isPaymentRejected(response) && isRetryable(response)) {
            throw new RetryablePaymentOrderException(response.getReasonText());
        }
    }

    private PaymentOrdersPostResponseBody handleException(Exception e) {
        if (isRetryable(e)) {
            throw new RetryablePaymentOrderException(e);
        }

        return new PaymentOrdersPostResponseBody()
                .bankStatus(REJECTED.getValue())
                .reasonText("Could not process scheduled payment")
                .errorDescription(e.getMessage());
    }

    private boolean isPaymentRejected(PaymentOrdersPostResponseBody response) {
        return isNotBlank(response.getBankStatus())
                && response.getBankStatus().equalsIgnoreCase(REJECTED.getValue());
    }

    private boolean isRetryable(PaymentOrdersPostResponseBody response) {
         /* A list of retry-able error codes can be defined in the application.yaml. If the core returns one of these
          configured error codes, then the scheduler will attempt to resubmit the payment to the core. */

        return nonNull(paymentSchedulerProperties.getRetryErrorCodes())
                && paymentSchedulerProperties.getRetryErrorCodes().contains(response.getReasonCode());
    }

    private boolean isRetryable(Exception e) {
        /* A list of retry-able exception classes can be defined in the application.yaml. If one of these configured
         exceptions is thrown, then the scheduler will attempt to resubmit the payment to the core. */

        String exceptionClassName = e.getClass().getName();
        log.info("Checking if class {} is a retry-able exception.", exceptionClassName);

        return paymentSchedulerProperties.getRetryExceptionClasses().contains(exceptionClassName);
    }
}
