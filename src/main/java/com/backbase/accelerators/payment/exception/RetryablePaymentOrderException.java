package com.backbase.accelerators.payment.exception;

/**
 * Exception to be thrown to trigger Spring Retry for eligible payment submission failures.
 */
public class RetryablePaymentOrderException extends RuntimeException {

    public RetryablePaymentOrderException(Throwable cause) {
        super(cause);
    }

    public RetryablePaymentOrderException(String message) {
        super(message);
    }
}
