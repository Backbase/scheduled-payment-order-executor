package com.backbase.accelerators.payment.mapper;

import com.backbase.accelerators.payment.model.PaymentOrderExecutionResponse;
import com.backbase.accelerators.payment.model.ScheduledPaymentOrder;
import com.backbase.payments.scheduled.v1.service.model.PostScheduledPaymentOrderTransactionRequest;
import com.backbase.payments.v2.outbound.model.PaymentOrdersPostRequestBody;
import com.backbase.payments.v2.service.model.GetPaymentOrderResponse;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface PaymentOrderMapper {

    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "id", source = "id")
    @Mapping(target = "paymentMode", constant = "SINGLE")
    @Mapping(target = "paymentType", source = "paymentType")
    @Mapping(target = "entryClass", source = "entryClass")
    @Mapping(target = "batchBooking", source = "batchBooking")
    @Mapping(target = "originator", source = "originator")
    @Mapping(target = "originatorAccount", source = "originatorAccount")
    @Mapping(target = "originatorAccount.identification.schemeName", constant = "BBAN")
    @Mapping(target = "instructionPriority", source = "instructionPriority")
    @Mapping(target = "additions", source = "additions")
    @Mapping(target = "schedule", ignore = true)
    @Mapping(target = "transferTransactionInformation.counterpartyAccount.identification.schemeName", constant = "BBAN")
    @Mapping(target = "requestedExecutionDate", expression = "java(java.time.LocalDate.now())")
    PaymentOrdersPostRequestBody toOutBoundRequest(ScheduledPaymentOrder scheduledPaymentOrder);

    ScheduledPaymentOrder toScheduledPaymentOrder(GetPaymentOrderResponse getPaymentOrderResponse);

    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "scheduledPaymentOrderId", source = "originalScheduledPaymentOrder.id")
    @Mapping(target = "bankReferenceId", source = "paymentOrdersPostResponseBody.bankReferenceId")
    @Mapping(target = "status", source = "paymentOrdersPostResponseBody.bankStatus")
    @Mapping(target = "executionDate", expression = "java(java.time.LocalDate.now())")
    @Mapping(target = "amount", source = "originalScheduledPaymentOrder.transferTransactionInformation.instructedAmount.amount")
    @Mapping(target = "reasonCode", source = "paymentOrdersPostResponseBody.reasonCode")
    @Mapping(target = "reasonText", source = "paymentOrdersPostResponseBody.reasonText")
    PostScheduledPaymentOrderTransactionRequest toPostScheduledPaymentOrderTransactionRequest(PaymentOrderExecutionResponse response);

}
