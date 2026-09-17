package com.orderflow.payment.domain;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class PaymentDomainTest {

    @Test
    void newPayment_hasPendingStatusAndCreatedAt() {
        Payment payment = new Payment();
        payment.setAmount(new BigDecimal("99.99"));

        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.PENDING);
        assertThat(payment.getCreatedAt()).isNotNull();
        assertThat(payment.getProcessedAt()).isNull();
        assertThat(payment.getAmount()).isEqualTo(new BigDecimal("99.99"));
    }

    @Test
    void markSuccess_setsStatusToSuccessAndProcessedAt() {
        Payment payment = new Payment();
        payment.markSuccess();

        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.SUCCESS);
        assertThat(payment.getProcessedAt()).isNotNull();
        assertThat(payment.getProcessedAt()).isBeforeOrEqualTo(Instant.now());
    }

    @Test
    void markFailed_setsStatusToFailedAndProcessedAt() {
        Payment payment = new Payment();
        payment.markFailed();

        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.FAILED);
        assertThat(payment.getProcessedAt()).isNotNull();
        assertThat(payment.getProcessedAt()).isBeforeOrEqualTo(Instant.now());
    }

    @Test
    void markRefunded_setsStatusToRefundedAndProcessedAt() {
        Payment payment = new Payment();
        payment.markSuccess();
        payment.markRefunded();

        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.REFUNDED);
        assertThat(payment.getProcessedAt()).isNotNull();
    }
}
