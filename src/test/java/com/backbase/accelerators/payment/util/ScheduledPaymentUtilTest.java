package com.backbase.accelerators.payment.util;

import com.backbase.accelerators.payment.model.ScheduledPaymentOrder;
import com.backbase.payments.v2.service.model.SimpleSchedule;
import org.junit.Test;

import java.time.LocalDate;

import static com.backbase.payments.v2.service.model.SimpleSchedule.EveryEnum._1;
import static com.backbase.payments.v2.service.model.SimpleSchedule.EveryEnum._2;
import static com.backbase.payments.v2.service.model.SimpleSchedule.TransferFrequencyEnum.BIWEEKLY;
import static com.backbase.payments.v2.service.model.SimpleSchedule.TransferFrequencyEnum.DAILY;
import static com.backbase.payments.v2.service.model.SimpleSchedule.TransferFrequencyEnum.MONTHLY;
import static com.backbase.payments.v2.service.model.SimpleSchedule.TransferFrequencyEnum.QUARTERLY;
import static com.backbase.payments.v2.service.model.SimpleSchedule.TransferFrequencyEnum.WEEKLY;
import static com.backbase.payments.v2.service.model.SimpleSchedule.TransferFrequencyEnum.YEARLY;
import static java.time.LocalDate.now;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class ScheduledPaymentUtilTest {

    @Test
    public void should_calculate_next_execution_date() {
        LocalDate result = null;

        // Daily
        result = ScheduledPaymentUtil.calculateNextExecutionDate(getScheduledPaymentOrder(DAILY, _1));
        assertEquals(now().plusDays(1), result);

        result = ScheduledPaymentUtil.calculateNextExecutionDate(getScheduledPaymentOrder(DAILY, _2));
        assertEquals(now().plusDays(2), result);

        // Weekly
        result = ScheduledPaymentUtil.calculateNextExecutionDate(getScheduledPaymentOrder(WEEKLY, _1));
        assertEquals(now().plusWeeks(1), result);

        result = ScheduledPaymentUtil.calculateNextExecutionDate(getScheduledPaymentOrder(WEEKLY, _2));
        assertEquals(now().plusWeeks(2), result);

        // Bi-Weekly
        result = ScheduledPaymentUtil.calculateNextExecutionDate(getScheduledPaymentOrder(BIWEEKLY, _1));
        assertEquals(now().plusWeeks(2), result);

        result = ScheduledPaymentUtil.calculateNextExecutionDate(getScheduledPaymentOrder(BIWEEKLY, _2));
        assertEquals(now().plusWeeks(4), result);

        // Monthly
        result = ScheduledPaymentUtil.calculateNextExecutionDate(getScheduledPaymentOrder(MONTHLY, _1));
        assertEquals(now().plusMonths(1), result);

        result = ScheduledPaymentUtil.calculateNextExecutionDate(getScheduledPaymentOrder(MONTHLY, _2));
        assertEquals(now().plusMonths(2), result);

        // Quarterly
        result = ScheduledPaymentUtil.calculateNextExecutionDate(getScheduledPaymentOrder(QUARTERLY, _1));
        assertEquals(now().plusMonths(3), result);

        result = ScheduledPaymentUtil.calculateNextExecutionDate(getScheduledPaymentOrder(QUARTERLY, _2));
        assertEquals(now().plusMonths(6), result);

        // Yearly
        result = ScheduledPaymentUtil.calculateNextExecutionDate(getScheduledPaymentOrder(YEARLY, _1));
        assertEquals(now().plusYears(1), result);

        result = ScheduledPaymentUtil.calculateNextExecutionDate(getScheduledPaymentOrder(YEARLY, _2));
        assertEquals(now().plusYears(2), result);
    }

    @Test
    public void should_return_null_when_calculated_execution_date_exceeds_end_date() {
        ScheduledPaymentOrder scheduledPaymentOrder = getScheduledPaymentOrder(DAILY, _1);
        scheduledPaymentOrder.getSchedule().setEndDate(now());

        LocalDate result = ScheduledPaymentUtil.calculateNextExecutionDate(scheduledPaymentOrder);
        assertNull(result);
    }

    @Test
    public void should_return_true_when_end_date_is_in_the_past() {
        LocalDate endDate = LocalDate.now().minusDays(1);

        SimpleSchedule schedule = new SimpleSchedule()
                .endDate(endDate);

        ScheduledPaymentOrder scheduledPaymentOrder = new ScheduledPaymentOrder();
        scheduledPaymentOrder.setId("TEST");
        scheduledPaymentOrder.setSchedule(schedule);

        assertTrue(ScheduledPaymentUtil.isEndDateInThePast(scheduledPaymentOrder));
    }

    @Test
    public void should_return_false_when_end_date_is_in_the_future() {
        LocalDate endDate = LocalDate.now().plusDays(1);

        SimpleSchedule schedule = new SimpleSchedule()
                .endDate(endDate);

        ScheduledPaymentOrder scheduledPaymentOrder = new ScheduledPaymentOrder();
        scheduledPaymentOrder.setId("TEST");
        scheduledPaymentOrder.setSchedule(schedule);

        assertFalse(ScheduledPaymentUtil.isEndDateInThePast(scheduledPaymentOrder));
    }

    @Test
    public void should_return_true_when_payment_order_is_scheduled_for_today() {
        LocalDate endDate = LocalDate.now();

        SimpleSchedule schedule = new SimpleSchedule()
                .startDate(endDate);

        ScheduledPaymentOrder scheduledPaymentOrder = new ScheduledPaymentOrder();
        scheduledPaymentOrder.setId("TEST");
        scheduledPaymentOrder.setSchedule(schedule);

        assertTrue(ScheduledPaymentUtil.isPaymentScheduledForToday(scheduledPaymentOrder));
    }

    private ScheduledPaymentOrder getScheduledPaymentOrder(
            SimpleSchedule.TransferFrequencyEnum frequency,
            SimpleSchedule.EveryEnum every) {

        SimpleSchedule schedule = new SimpleSchedule()
                .every(every)
                .transferFrequency(frequency);

        ScheduledPaymentOrder scheduledPaymentOrder = new ScheduledPaymentOrder();
        scheduledPaymentOrder.setId("TEST");
        scheduledPaymentOrder.setSchedule(schedule);

        return scheduledPaymentOrder;
    }

}