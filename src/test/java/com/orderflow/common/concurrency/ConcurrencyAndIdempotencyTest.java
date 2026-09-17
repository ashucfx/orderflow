package com.orderflow.common.concurrency;

import com.orderflow.common.idempotency.domain.IdempotencyRecord;
import com.orderflow.common.idempotency.service.IdempotencyService;
import com.orderflow.inventory.domain.Inventory;
import com.orderflow.order.domain.Order;
import com.orderflow.order.domain.OrderStatus;
import com.orderflow.order.dto.CheckoutRequest;
import com.orderflow.order.dto.OrderResponse;
import com.orderflow.order.repository.OrderRepository;
import com.orderflow.order.service.OrderService;
import com.orderflow.user.domain.Role;
import com.orderflow.user.domain.User;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ConcurrencyAndIdempotencyTest {

    @Mock
    private OrderService orderService;

    @Mock
    private IdempotencyService idempotencyService;

    @Test
    void concurrentCheckout_withSameIdempotencyKey_returnsConsistentResults() throws InterruptedException {
        int threadCount = 10;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch readyLatch = new CountDownLatch(threadCount);
        CountDownLatch startLatch = new CountDownLatch(1);

        UUID orderId = UUID.randomUUID();
        OrderResponse expectedResponse = OrderResponse.builder()
                .id(orderId)
                .status(OrderStatus.PENDING)
                .totalAmount(new BigDecimal("99.99"))
                .idempotencyKey("SHARED-IDEMPOTENCY-KEY")
                .build();

        AtomicInteger serviceInvocationCount = new AtomicInteger(0);

        when(orderService.checkout(any(String.class), any(CheckoutRequest.class)))
                .thenAnswer(inv -> {
                    serviceInvocationCount.incrementAndGet();
                    return expectedResponse;
                });

        List<OrderResponse> results = Collections.synchronizedList(new ArrayList<>());
        List<Future<?>> futures = new ArrayList<>();

        for (int i = 0; i < threadCount; i++) {
            futures.add(executor.submit(() -> {
                readyLatch.countDown();
                try {
                    startLatch.await();
                    OrderResponse response = orderService.checkout("user@example.com",
                            new CheckoutRequest("SHARED-IDEMPOTENCY-KEY"));
                    results.add(response);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }));
        }

        readyLatch.await(5, TimeUnit.SECONDS);
        startLatch.countDown(); // release all threads simultaneously

        for (Future<?> f : futures) {
            try {
                f.get(5, TimeUnit.SECONDS);
            } catch (ExecutionException | TimeoutException e) {
                throw new RuntimeException(e);
            }
        }
        executor.shutdown();

        assertThat(results).hasSize(threadCount);
        for (OrderResponse res : results) {
            assertThat(res.getId()).isEqualTo(orderId);
            assertThat(res.getIdempotencyKey()).isEqualTo("SHARED-IDEMPOTENCY-KEY");
        }
    }

    @Test
    void concurrentInventoryReservation_preservesInvariants() throws InterruptedException {
        int threadCount = 20;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch readyLatch = new CountDownLatch(threadCount);
        CountDownLatch startLatch = new CountDownLatch(1);

        Inventory inventory = new Inventory();
        inventory.setAvailableQuantity(100);
        inventory.setReservedQuantity(0);

        AtomicInteger successfulReservations = new AtomicInteger(0);
        AtomicInteger failedReservations = new AtomicInteger(0);

        List<Future<?>> futures = new ArrayList<>();

        for (int i = 0; i < threadCount; i++) {
            futures.add(executor.submit(() -> {
                readyLatch.countDown();
                try {
                    startLatch.await();
                    synchronized (inventory) {
                        if (inventory.getAvailableQuantity() >= 10) {
                            inventory.reserve(10);
                            successfulReservations.incrementAndGet();
                        } else {
                            failedReservations.incrementAndGet();
                        }
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }));
        }

        readyLatch.await(5, TimeUnit.SECONDS);
        startLatch.countDown();

        for (Future<?> f : futures) {
            try {
                f.get(5, TimeUnit.SECONDS);
            } catch (ExecutionException | TimeoutException e) {
                throw new RuntimeException(e);
            }
        }
        executor.shutdown();

        assertThat(successfulReservations.get()).isEqualTo(10);
        assertThat(failedReservations.get()).isEqualTo(10);
        assertThat(inventory.getAvailableQuantity()).isEqualTo(0);
        assertThat(inventory.getReservedQuantity()).isEqualTo(100);
    }
}
