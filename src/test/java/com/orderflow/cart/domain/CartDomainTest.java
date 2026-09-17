package com.orderflow.cart.domain;

import com.orderflow.product.domain.Product;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class CartDomainTest {

    private Cart cart;
    private Product product1;
    private Product product2;

    @BeforeEach
    void setUp() {
        cart = new Cart();
        cart.setId(UUID.randomUUID());

        product1 = new Product();
        product1.setId(UUID.randomUUID());
        product1.setName("Mouse");
        product1.setSku("MOUSE-01");
        product1.setUnitPrice(new BigDecimal("25.00"));

        product2 = new Product();
        product2.setId(UUID.randomUUID());
        product2.setName("Keyboard");
        product2.setSku("KB-01");
        product2.setUnitPrice(new BigDecimal("75.00"));
    }

    @Test
    void addItem_newProduct_addsCartItem() {
        cart.addItem(product1, 2);

        assertThat(cart.getItems()).hasSize(1);
        assertThat(cart.getItems().get(0).getProduct().getName()).isEqualTo("Mouse");
        assertThat(cart.getItems().get(0).getQuantity()).isEqualTo(2);
    }

    @Test
    void addItem_existingProduct_incrementsQuantity() {
        cart.addItem(product1, 2);
        cart.addItem(product1, 3);

        assertThat(cart.getItems()).hasSize(1);
        assertThat(cart.getItems().get(0).getQuantity()).isEqualTo(5);
    }

    @Test
    void updateItemQuantity_success() {
        cart.addItem(product1, 2);
        UUID itemId = UUID.randomUUID();
        cart.getItems().get(0).setId(itemId);

        cart.updateItemQuantity(itemId, 10);

        assertThat(cart.getItems().get(0).getQuantity()).isEqualTo(10);
    }

    @Test
    void removeItem_success() {
        cart.addItem(product1, 2);
        UUID itemId = UUID.randomUUID();
        cart.getItems().get(0).setId(itemId);

        cart.removeItem(itemId);

        assertThat(cart.getItems()).isEmpty();
    }

    @Test
    void clear_removesAllItems() {
        cart.addItem(product1, 2);
        cart.addItem(product2, 1);

        cart.clear();

        assertThat(cart.getItems()).isEmpty();
    }

    @Test
    void calculateSubtotal_andTotalItems_calculatesCorrectValues() {
        cart.addItem(product1, 2); // 2 * 25.00 = 50.00
        cart.addItem(product2, 1); // 1 * 75.00 = 75.00

        assertThat(cart.calculateTotalItems()).isEqualTo(3);
        assertThat(cart.calculateSubtotal()).isEqualByComparingTo(new BigDecimal("125.00"));
    }
}
