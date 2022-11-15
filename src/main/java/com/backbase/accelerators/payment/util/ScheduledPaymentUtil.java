package com.backbase.accelerators.payment.util;

import com.backbase.accelerators.payment.model.ScheduledPaymentOrder;
import com.backbase.payments.v2.service.model.SimpleSchedule;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDate;
import java.util.Optional;

import static com.backbase.accelerators.payment.constants.DataModelAdditions.EXECUTION_COUNT;
import static com.backbase.accelerators.payment.constants.DataModelAdditions.ORIGINAL_NEXT_EXECUTION_DATE;
import static com.backbase.payments.v2.service.model.SimpleSchedule.TransferFrequencyEnum.BIWEEKLY;
import static com.backbase.payments.v2.service.model.SimpleSchedule.TransferFrequencyEnum.DAILY;
import static com.backbase.payments.v2.service.model.SimpleSchedule.TransferFrequencyEnum.MONTHLY;
import static com.backbase.payments.v2.service.model.SimpleSchedule.TransferFrequencyEnum.QUARTERLY;
import static com.backbase.payments.v2.service.model.SimpleSchedule.TransferFrequencyEnum.WEEKLY;
import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static java.util.Objects.requireNonNull;

@Slf4j
public class ScheduledPaymentUtil {

    public static LocalDate calculateNextExecutionDate(ScheduledPaymentOrder scheduledPaymentOrder) {
        log.info("Calculating next execution date for scheduled payment: {}", scheduledPaymentOrder.getId());

        SimpleSchedule.TransferFrequencyEnum frequency = requireNonNull(scheduledPaymentOrder.getSchedule()).getTransferFrequency();
        SimpleSchedule.EveryEnum everyEnum = requireNonNull(scheduledPaymentOrder.getSchedule().getEvery());

        LocalDate nextExecutionDate = calculateNextExecutionDate(
                getCurrentExecutionDate(scheduledPaymentOrder),
                frequency,
                everyEnum);

        log.info("Next execution date calculated for payment {}: {}", scheduledPaymentOrder.getId(), nextExecutionDate);
        LocalDate endDate = scheduledPaymentOrder.getSchedule().getEndDate();

        if (nonNull(endDate) && endDate.isBefore(nextExecutionDate)) {
            log.info("End date occurs before next execution date. endDate={}; nextExecutionDate={}",
                    endDate,
                    nextExecutionDate);

            return null;
        }

        return nextExecutionDate;
    }

    private static LocalDate getCurrentExecutionDate(ScheduledPaymentOrder scheduledPaymentOrder) {
        /* The ORIGINAL_NEXT_EXECUTION_DATE represents the date the payment was naturally supposed to be executed
        per the initiator's instruction, but was adjusted due to it falling on a weekend/restricted date.

        If the ORIGINAL_NEXT_EXECUTION_DATE is present in the additions, then we will calculate the new 'next' execute date
        from this date, otherwise, we will calculate it from today's date. */

        return Optional.ofNullable(scheduledPaymentOrder)
                .map(ScheduledPaymentOrder::getAdditions)
                .map(additions -> additions.get(ORIGINAL_NEXT_EXECUTION_DATE.getValue()))
                .map(LocalDate::parse)
                .orElse(LocalDate.now());
    }

    private static LocalDate calculateNextExecutionDate(
            LocalDate currentExecutionDate,
            SimpleSchedule.TransferFrequencyEnum frequency,
            SimpleSchedule.EveryEnum everyEnum) {

        // '1' means execute the payment every single time. '2' means execute payment every other time.
        long every = Long.parseLong(everyEnum.getValue());
        LocalDate nextExecutionDate;

        if (frequency == DAILY) {
            nextExecutionDate = currentExecutionDate.plusDays(every);
        } else if (frequency == WEEKLY) {
            nextExecutionDate = currentExecutionDate.plusWeeks(every);
        } else if (frequency == BIWEEKLY) {
            nextExecutionDate = currentExecutionDate.plusWeeks(2 * every);
        } else if (frequency == MONTHLY) {
            nextExecutionDate = currentExecutionDate.plusMonths(every);
        } else if (frequency == QUARTERLY) {
            nextExecutionDate = currentExecutionDate.plusMonths(3 * every);
        } else {
            nextExecutionDate = currentExecutionDate.plusYears(every);
        }

        return nextExecutionDate;
    }

    public static boolean isEndDateInThePast(ScheduledPaymentOrder scheduledPaymentOrder) {
        LocalDate endDate = Optional.ofNullable(scheduledPaymentOrder.getSchedule())
                .map(SimpleSchedule::getEndDate)
                .orElse(null);

        if (isNull(endDate)) {
            return false;
        }

        boolean isEndDateInThePast = endDate.isBefore(LocalDate.now());
        if (isEndDateInThePast) {
            log.info("End date for scheduled Payment Order {} has already passed. End date: {}",
                    scheduledPaymentOrder.getId(),
                    endDate);
        }

        return isEndDateInThePast;
    }

    public static boolean isPaymentScheduledForToday(ScheduledPaymentOrder scheduledPaymentOrder) {
        LocalDate today = LocalDate.now();
        LocalDate nextExecutionDate = scheduledPaymentOrder.getSchedule().getNextExecutionDate();

        return (nonNull(nextExecutionDate) && nextExecutionDate.equals(today))
                || scheduledPaymentOrder.getSchedule().getStartDate().equals(today);

    }

    public static boolean isRepeatCountMet(ScheduledPaymentOrder scheduledPaymentOrder) {
        Optional<Integer> repeatCount = Optional.ofNullable(scheduledPaymentOrder.getSchedule())
                .map(SimpleSchedule::getRepeat);

        Optional<Integer> executionCount = Optional.ofNullable(scheduledPaymentOrder.getAdditions())
                .map(additions -> additions.get(EXECUTION_COUNT.getValue()))
                .map(Integer::parseInt);

        if (repeatCount.isPresent() && executionCount.isPresent() && repeatCount.get().equals(executionCount.get())) {
            log.info("Scheduled payment order {} has met it's repetition count of: {}",
                    scheduledPaymentOrder.getId(),
                    repeatCount.get());

            return true;
        }

        return false;
    }

    private ScheduledPaymentUtil() {
    }
}
