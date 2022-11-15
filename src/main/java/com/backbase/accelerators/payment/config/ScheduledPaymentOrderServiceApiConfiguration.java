package com.backbase.accelerators.payment.config;

import com.backbase.payments.scheduled.v1.service.ApiClient;
import com.backbase.payments.scheduled.v1.service.api.ScheduledPaymentOrderApi;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

import javax.validation.constraints.Pattern;

import static com.backbase.buildingblocks.communication.http.HttpCommunicationConfiguration.INTERCEPTORS_ENABLED_HEADER;
import static com.backbase.buildingblocks.communication.http.HttpCommunicationConfiguration.INTER_SERVICE_REST_TEMPLATE_BEAN_NAME;

@Configuration
public class ScheduledPaymentOrderServiceApiConfiguration {

    private static final String SCHEDULED_PAYMENT_ORDER_SERVICE_ID = "scheduled-payment-order-service";

    @Value("${backbase.communication.http.default-scheme:http}")
    @Pattern(regexp = "https?")
    private String scheme;

    @Bean
    public ScheduledPaymentOrderApi scheduledPaymentOrderApi(
            @Qualifier(INTER_SERVICE_REST_TEMPLATE_BEAN_NAME) RestTemplate restTemplate) {

        ApiClient apiClient = new ApiClient(restTemplate);
        apiClient.setBasePath(scheme + "://" + SCHEDULED_PAYMENT_ORDER_SERVICE_ID);
        apiClient.addDefaultHeader(INTERCEPTORS_ENABLED_HEADER, Boolean.TRUE.toString());

        return new ScheduledPaymentOrderApi(apiClient);
    }
}
