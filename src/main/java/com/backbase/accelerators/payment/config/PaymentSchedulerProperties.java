package com.backbase.accelerators.payment.config;

import com.backbase.payments.v2.service.model.Status;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.ArrayList;
import java.util.List;

@Data
@Configuration
@ConfigurationProperties("payment-scheduler")
public class PaymentSchedulerProperties {

    private boolean limitChecksEnabled;
    private IteratorProperties iteratorProperties;
    private QueryFilters queryFilters;
    private List<String> retryErrorCodes = new ArrayList<>();
    private List<String> retryExceptionClasses = new ArrayList<>();

    @Data
    public static class IteratorProperties {
        private int pageSize;
    }

    @Data
    public static class QueryFilters {
        private List<Status> statuses;
        private List<String> paymentTypes;
    }

}
