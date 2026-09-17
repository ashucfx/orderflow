package com.orderflow.payment.service;

import com.orderflow.common.exception.BadRequestException;
import com.orderflow.common.exception.InvalidStateTransitionException;
import com.orderflow.common.exception.ResourceNotFoundException;
import com.orderflow.inventory.service.InventoryService;
import com.orderflow.order.domain.Order;
import com.orderflow.order.domain.OrderItem;
import com.orderflow.order.domain.OrderStatus;
import com.orderflow.order.repository.OrderRepository;
import com.orderflow.payment.domain.Payment;
import com.orderflow.payment.domain.PaymentStatus;
import com.orderflow.payment.dto.PaymentResponse;
import com.orderflow.payment.dto.ProcessPaymentRequest;
import com.orderflow.payment.repository.PaymentRepository;
import com.orderflow.product.domain.Product;
import com.orderflow.user.domain.Role;
import com.orderflow.user.domain.User;
import com.orderflow.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private InventoryService inventoryService;

    @Mock
    private com.orderflow.order.kafka.OrderEventProducer orderEventProducer;

    @InjectMocks
    private PaymentServiceImpl paymentService;

    private User sampleUser;
    private Order sampleOrder;
    private Product sampleProduct;

    @BeforeEach
    void setUp() {
        sampleUser = new User();
        sampleUser.setId(UUID.randomUUID());
        sampleUser.setEmail("buyer@example.com");
        Role customerRole = new Role();
        customerRole.setName(Role.RoleName.CUSTOMER);
        sampleUser.addRole(customerRole);

        sampleProduct = new Product();
        sampleProduct.setId(UUID.randomUUID());
        sampleProduct.setName("Wireless Mouse");
        sampleProduct.setSku("MOU-001");
        sampleProduct.setUnitPrice(new BigDecimal("50.00"));

        sampleOrder = new Order();
        sampleOrder.setId(UUID.randomUUID());
        sampleOrder.setUser(sampleUser);
        sampleOrder.setStatus(OrderStatus.PENDING);
        sampleOrder.setTotalAmount(new BigDecimal("100.00"));

        OrderItem item = new OrderItem();
        item.setProduct(sampleProduct);
        item.setQuantity(2);
        item.setUnitPrice(new BigDecimal("50.00"));
        item.setSubtotal(new BigDecimal("100.00"));
        sampleOrder.addItem(item);
    }

    @Test
    void processPayment_success_confirmsInventoryAndOrder() {
        ProcessPaymentRequest request = ProcessPaymentRequest.builder()
                .orderId(sampleOrder.getId())
                .amount(new BigDecimal("100.00"))
                .paymentMethod("CREDIT_CARD")
                .simulateFailure(false)
                .build();

        when(userRepository.findByEmail("buyer@example.com")).thenReturn(Optional.of(sampleUser));
        when(orderRepository.findById(sampleOrder.getId())).thenReturn(Optional.of(sampleOrder));
        when(paymentRepository.findByOrderId(sampleOrder.getId())).thenReturn(Optional.empty());
        when(paymentRepository.save(any(Payment.class))).thenAnswer(inv -> {
            Payment p = inv.getArgument(0);
            p.setId(UUID.randomUUID());
            return p;
        });
        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));

        PaymentResponse response = paymentService.processPayment("buyer@example.com", request);

        assertThat(response.getStatus()).isEqualTo(PaymentStatus.SUCCESS);
        assertThat(response.getAmount()).isEqualTo(new BigDecimal("100.00"));
        assertThat(sampleOrder.getStatus()).isEqualTo(OrderStatus.CONFIRMED);

        verify(inventoryService).confirmStock(sampleProduct.getId(), 2);
        verify(paymentRepository).save(any(Payment.class));
        verify(orderRepository).save(sampleOrder);
    }

    @Test
    void processPayment_simulatedFailure_cancelsOrderAndReleasesInventory() {
        ProcessPaymentRequest request = ProcessPaymentRequest.builder()
                .orderId(sampleOrder.getId())
                .amount(new BigDecimal("100.00"))
                .paymentMethod("UPI")
                .simulateFailure(true)
                .build();

        when(userRepository.findByEmail("buyer@example.com")).thenReturn(Optional.of(sampleUser));
        when(orderRepository.findById(sampleOrder.getId())).thenReturn(Optional.of(sampleOrder));
        when(paymentRepository.findByOrderId(sampleOrder.getId())).thenReturn(Optional.empty());
        when(paymentRepository.save(any(Payment.class))).thenAnswer(inv -> {
            Payment p = inv.getArgument(0);
            p.setId(UUID.randomUUID());
            return p;
        });
        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));

        PaymentResponse response = paymentService.processPayment("buyer@example.com", request);

        assertThat(response.getStatus()).isEqualTo(PaymentStatus.FAILED);
        assertThat(sampleOrder.getStatus()).isEqualTo(OrderStatus.CANCELLED);

        verify(inventoryService).releaseStock(sampleProduct.getId(), 2);
        verify(paymentRepository).save(any(Payment.class));
        verify(orderRepository).save(sampleOrder);
    }

    @Test
    void processPayment_idempotentSuccessReplay() {
        ProcessPaymentRequest request = ProcessPaymentRequest.builder()
                .orderId(sampleOrder.getId())
                .amount(new BigDecimal("100.00"))
                .build();

        Payment existingPayment = new Payment();
        existingPayment.setId(UUID.randomUUID());
        existingPayment.setOrder(sampleOrder);
        existingPayment.setAmount(new BigDecimal("100.00"));
        existingPayment.markSuccess();

        when(userRepository.findByEmail("buyer@example.com")).thenReturn(Optional.of(sampleUser));
        when(orderRepository.findById(sampleOrder.getId())).thenReturn(Optional.of(sampleOrder));
        when(paymentRepository.findByOrderId(sampleOrder.getId())).thenReturn(Optional.of(existingPayment));

        PaymentResponse response = paymentService.processPayment("buyer@example.com", request);

        assertThat(response.getStatus()).isEqualTo(PaymentStatus.SUCCESS);
        verify(paymentRepository, never()).save(existingPayment);
        verify(orderRepository, never()).save(any(Order.class));
        verify(inventoryService, never()).confirmStock(any(), anyInt());
    }

    @Test
    void processPayment_unauthorizedUser_throwsAccessDeniedException() {
        User differentUser = new User();
        differentUser.setId(UUID.randomUUID());
        differentUser.setEmail("other@example.com");
        Role customerRole = new Role();
        customerRole.setName(Role.RoleName.CUSTOMER);
        differentUser.addRole(customerRole);

        ProcessPaymentRequest request = ProcessPaymentRequest.builder()
                .orderId(sampleOrder.getId())
                .amount(new BigDecimal("100.00"))
                .build();

        when(userRepository.findByEmail("other@example.com")).thenReturn(Optional.of(differentUser));
        when(orderRepository.findById(sampleOrder.getId())).thenReturn(Optional.of(sampleOrder));

        assertThatThrownBy(() -> paymentService.processPayment("other@example.com", request))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("not authorized");
    }

    @Test
    void processPayment_adminUser_canProcessForCustomerOrder() {
        User adminUser = new User();
        adminUser.setId(UUID.randomUUID());
        adminUser.setEmail("admin@example.com");
        Role adminRole = new Role();
        adminRole.setName(Role.RoleName.ADMIN);
        adminUser.addRole(adminRole);

        ProcessPaymentRequest request = ProcessPaymentRequest.builder()
                .orderId(sampleOrder.getId())
                .amount(new BigDecimal("100.00"))
                .build();

        when(userRepository.findByEmail("admin@example.com")).thenReturn(Optional.of(adminUser));
        when(orderRepository.findById(sampleOrder.getId())).thenReturn(Optional.of(sampleOrder));
        when(paymentRepository.findByOrderId(sampleOrder.getId())).thenReturn(Optional.empty());
        when(paymentRepository.save(any(Payment.class))).thenAnswer(inv -> inv.getArgument(0));
        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));

        PaymentResponse response = paymentService.processPayment("admin@example.com", request);

        assertThat(response.getStatus()).isEqualTo(PaymentStatus.SUCCESS);
        assertThat(sampleOrder.getStatus()).isEqualTo(OrderStatus.CONFIRMED);
    }

    @Test
    void processPayment_nonPendingOrder_throwsInvalidStateTransitionException() {
        sampleOrder.setStatus(OrderStatus.CONFIRMED);

        ProcessPaymentRequest request = ProcessPaymentRequest.builder()
                .orderId(sampleOrder.getId())
                .amount(new BigDecimal("100.00"))
                .build();

        when(userRepository.findByEmail("buyer@example.com")).thenReturn(Optional.of(sampleUser));
        when(orderRepository.findById(sampleOrder.getId())).thenReturn(Optional.of(sampleOrder));

        assertThatThrownBy(() -> paymentService.processPayment("buyer@example.com", request))
                .isInstanceOf(InvalidStateTransitionException.class)
                .hasMessageContaining("CONFIRMED");
    }

    @Test
    void processPayment_amountMismatch_throwsBusinessException() {
        ProcessPaymentRequest request = ProcessPaymentRequest.builder()
                .orderId(sampleOrder.getId())
                .amount(new BigDecimal("75.00"))
                .build();

        when(userRepository.findByEmail("buyer@example.com")).thenReturn(Optional.of(sampleUser));
        when(orderRepository.findById(sampleOrder.getId())).thenReturn(Optional.of(sampleOrder));

        assertThatThrownBy(() -> paymentService.processPayment("buyer@example.com", request))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("does not match order total");
    }

    @Test
    void getPaymentByOrderId_success() {
        Payment payment = new Payment();
        payment.setId(UUID.randomUUID());
        payment.setOrder(sampleOrder);
        payment.setAmount(new BigDecimal("100.00"));
        payment.markSuccess();

        when(userRepository.findByEmail("buyer@example.com")).thenReturn(Optional.of(sampleUser));
        when(orderRepository.findById(sampleOrder.getId())).thenReturn(Optional.of(sampleOrder));
        when(paymentRepository.findByOrderId(sampleOrder.getId())).thenReturn(Optional.of(payment));

        PaymentResponse response = paymentService.getPaymentByOrderId("buyer@example.com", sampleOrder.getId());

        assertThat(response.getStatus()).isEqualTo(PaymentStatus.SUCCESS);
        assertThat(response.getOrderId()).isEqualTo(sampleOrder.getId());
    }

    @Test
    void getPaymentById_success() {
        UUID paymentId = UUID.randomUUID();
        Payment payment = new Payment();
        payment.setId(paymentId);
        payment.setOrder(sampleOrder);
        payment.setAmount(new BigDecimal("100.00"));
        payment.markSuccess();

        when(userRepository.findByEmail("buyer@example.com")).thenReturn(Optional.of(sampleUser));
        when(paymentRepository.findById(paymentId)).thenReturn(Optional.of(payment));

        PaymentResponse response = paymentService.getPaymentById("buyer@example.com", paymentId);

        assertThat(response.getId()).isEqualTo(paymentId);
        assertThat(response.getStatus()).isEqualTo(PaymentStatus.SUCCESS);
    }
}
