package com.backbase.accelerators.payment.config;

import com.backbase.payments.v2.outbound.ApiClient;
import com.backbase.payments.v2.outbound.api.PaymentOrderIntegrationOutboundApi;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

import javax.validation.constraints.Pattern;

import static com.backbase.buildingblocks.communication.http.HttpCommunicationConfiguration.INTERCEPTORS_ENABLED_HEADER;
import static com.backbase.buildingblocks.communication.http.HttpCommunicationConfiguration.INTER_SERVICE_REST_TEMPLATE_BEAN_NAME;

@Configuration
public class PaymentOrderOutboundServiceApiConfiguration {

    private static final String PAYMENT_ORDER_INTEGRATION_SERVICE_ID = "payment-order-integration-service";

    @Value("${backbase.communication.http.default-scheme:http}")
    @Pattern(regexp = "https?")
    private String scheme;

    @Bean
    public PaymentOrderIntegrationOutboundApi paymentOrderIntegrationOutboundApi(
            @Qualifier(INTER_SERVICE_REST_TEMPLATE_BEAN_NAME) RestTemplate restTemplate) {

        ApiClient apiClient = new ApiClient(restTemplate);
        apiClient.setBasePath(scheme + "://" + PAYMENT_ORDER_INTEGRATION_SERVICE_ID);
        apiClient.addDefaultHeader(INTERCEPTORS_ENABLED_HEADER, Boolean.TRUE.toString());

        return new PaymentOrderIntegrationOutboundApi(apiClient);
    }
}
