package com.backbase.accelerators.payment.client;

import com.backbase.accelerators.payment.config.PaymentSchedulerProperties;
import com.backbase.accelerators.payment.model.PaymentOrderExecutionResponse;
import com.backbase.accelerators.payment.model.ScheduledPaymentOrder;
import com.backbase.accelerators.payment.util.ScheduledPaymentUtil;
import com.backbase.payments.scheduled.v1.service.model.ValidateExecutionDateResponse;
import com.backbase.payments.v2.outbound.model.PaymentOrdersPostResponseBody;
import com.backbase.payments.v2.service.api.PaymentOrdersApi;
import com.backbase.payments.v2.service.model.Audit;
import com.backbase.payments.v2.service.model.PaymentOrderPostFilterRequest;
import com.backbase.payments.v2.service.model.PaymentOrderPostFilterResponse;
import com.backbase.payments.v2.service.model.PaymentOrderPutRequest;
import com.backbase.payments.v2.service.model.PaymentOrderPutResponse;
import com.backbase.payments.v2.service.model.SimpleSchedule;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.Optional;

import static com.backbase.accelerators.payment.constants.DataModelAdditions.EXECUTION_COUNT;
import static com.backbase.accelerators.payment.constants.DataModelAdditions.ORIGINAL_NEXT_EXECUTION_DATE;
import static com.backbase.payments.scheduled.v1.service.model.ValidateExecutionDateResponse.StatusEnum.OK;
import static com.backbase.payments.scheduled.v1.service.model.ValidateExecutionDateResponse.StatusEnum.RESTRICTED_DATE_DETECTED;
import static com.backbase.payments.v2.service.model.PaymentMode.RECURRING;
import static com.backbase.payments.v2.service.model.SimpleSchedule.NonWorkingDayExecutionStrategyEnum.BEFORE;
import static com.backbase.payments.v2.service.model.SimpleSchedule.NonWorkingDayExecutionStrategyEnum.NONE;
import static com.backbase.payments.v2.service.model.Status.READY;
import static java.util.Objects.nonNull;

@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentOrderServiceClient {

    private final PaymentOrdersApi paymentOrdersApi;
    private final ScheduledPaymentOrderServiceClient scheduledPaymentOrderServiceClient;
    private final PaymentSchedulerProperties paymentSchedulerProperties;

    public PaymentOrderPostFilterResponse getScheduledPaymentOrders(int from, int size) {
        log.info("Fetching scheduled payments starting at index {} and with page size {}", from, size);

        PaymentOrderPostFilterRequest request = new PaymentOrderPostFilterRequest()
                .paymentTypes(paymentSchedulerProperties.getQueryFilters().getPaymentTypes())
                .statuses(paymentSchedulerProperties.getQueryFilters().getStatuses());

        PaymentOrderPostFilterResponse response = paymentOrdersApi.postFilterPaymentOrders(
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                RECURRING.getValue(),
                null,
                null,
                null,
                null,
                from,
                size,
                null,
                null,
                request);

        log.debug("Scheduled payment orders retrieved: {}", response);
        return response;
    }

    public void updateNextExecutionDate(PaymentOrderExecutionResponse paymentOrderExecutionResponse) {
        ScheduledPaymentOrder scheduledPaymentOrder = paymentOrderExecutionResponse.getOriginalScheduledPaymentOrder();
        PaymentOrdersPostResponseBody paymentOrdersPostResponseBody = paymentOrderExecutionResponse.getPaymentOrdersPostResponseBody();
        log.info("Updating next execution date for scheduled payment: {}", scheduledPaymentOrder.getId());

        PaymentOrderPutRequest request = new PaymentOrderPutRequest()
                .status(READY)
                .bankStatus("READY")
                .audit(createAudit())
                .putAdditionsItem(EXECUTION_COUNT.getValue(), String.valueOf(incrementExecutionCount(scheduledPaymentOrder)));

        if (nonNull(paymentOrdersPostResponseBody.getNextExecutionDate())) {
            // If the outbound service already calculated the next execution date for us, then use that.
            request.setNextExecutionDate(paymentOrdersPostResponseBody.getNextExecutionDate());
        } else {
            // If the nextExecutionDate is not returned from the outbound service, then calculate it here.

            /* The nextExecutionDate is calculated purely off of the schedule defined on the payment order object
             * and does not account for weekends or restricted dates. That check will happen in the next step. */
            LocalDate nextExecutionDate = ScheduledPaymentUtil.calculateNextExecutionDate(scheduledPaymentOrder);

            if (nonNull(nextExecutionDate)) {
                /* Submit the nextExecutionDate for additional validation. If it falls on a weekend/restricted date
                 * the API will return alternative dates 'before' and 'after' the nextExecutionDate. Based on the
                 * NonWorkingDayExecutionStrategy defined on the payment order, we will have the choice to either adjust the
                 * execution date to be before or after (or to keep it as is, if the strategy is set to NONE). */
                LocalDate suggestedNextExecutionDate = validateExecutionDate(
                        nextExecutionDate,
                        scheduledPaymentOrder.getSchedule().getNonWorkingDayExecutionStrategy());

                if (nextExecutionDate.equals(suggestedNextExecutionDate)) {
                    // No adjustment of the execution date required.
                    request.setNextExecutionDate(nextExecutionDate);
                    request.putAdditionsItem(ORIGINAL_NEXT_EXECUTION_DATE.getValue(), null);
                } else {
                    request.setNextExecutionDate(suggestedNextExecutionDate);

                    // Keep track of what the original next execution date would have been, had it not fallen on a restricted date
                    request.putAdditionsItem(ORIGINAL_NEXT_EXECUTION_DATE.getValue(), nextExecutionDate.toString());
                }
            }
        }

        log.info("PaymentOrderPutRequest: {}", request);
        PaymentOrderPutResponse response = paymentOrdersApi.updatePaymentOrder(
                scheduledPaymentOrder.getId(),
                "INTERNALID",
                request);

        log.info("Successfully updated nextExecutionDate for scheduled payment order {}", response.getId());
    }

    private LocalDate validateExecutionDate(
            LocalDate nextExecutionDate,
            SimpleSchedule.NonWorkingDayExecutionStrategyEnum weekendExecutionStrategy) {

        log.info("Validating calculated execution date to see if it falls on a non-working day: {}", nextExecutionDate);
        ValidateExecutionDateResponse response = scheduledPaymentOrderServiceClient.validateNextExecutionDate(nextExecutionDate);
        log.info("Non-working day execution strategy for payment order: {}", weekendExecutionStrategy);

        if (!isNonWorkingDateExecutionStrategyDefined(weekendExecutionStrategy) || isValidExecutionDate(response)) {
            /* No non-working day execution strategy defined for the payment, or validation API returned OK status.
            just return the original execution date. */
            return response.getOriginalExecutionDate();
        }

        return getNextAvailableExecutionDate(response, weekendExecutionStrategy);
    }

    private LocalDate getNextAvailableExecutionDate(
            ValidateExecutionDateResponse response,
            SimpleSchedule.NonWorkingDayExecutionStrategyEnum weekendExecutionStrategy) {

        if (response.getStatus() == RESTRICTED_DATE_DETECTED && weekendExecutionStrategy == BEFORE) {
            if (nonNull(response.getNextAvailableExecutionDateBefore())) {
                // Move execution date to next immediate date BEFORE the originally calculated execution date.
                return response.getNextAvailableExecutionDateBefore();
            }
        }

        // Move execution date to next immediate date AFTER the originally calculated execution date.
        return response.getNextAvailableExecutionDateAfter();
    }

    private boolean isValidExecutionDate(ValidateExecutionDateResponse response) {
        return response.getStatus() == OK;
    }

    private boolean isNonWorkingDateExecutionStrategyDefined(SimpleSchedule.NonWorkingDayExecutionStrategyEnum weekendExecutionStrategy) {
        return nonNull(weekendExecutionStrategy) && weekendExecutionStrategy != NONE;
    }

    private int incrementExecutionCount(ScheduledPaymentOrder scheduledPaymentOrder) {
        /* Some scheduled payment orders may not have an endDate set, but instead utilize the repeat property.
         * We need to keep tally of each execution, so that we know when to no longer process payments. */
        return Optional.ofNullable(scheduledPaymentOrder.getAdditions())
                .map(additions -> additions.get(EXECUTION_COUNT.getValue()))
                .map(Integer::parseInt)
                .map(executionCount -> executionCount + 1)
                .orElse(1);
    }

    private Audit createAudit() {
        return new Audit()
                .timestamp(OffsetDateTime.now())
                .user("SYSTEM-SCHEDULER");
    }
}
