package com.orderflow.inventory.domain;

import com.orderflow.common.exception.InsufficientInventoryException;
import com.orderflow.product.domain.Product;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class InventoryDomainTest {

    private Inventory inventory;

    @BeforeEach
    void setUp() {
        Product product = new Product();
        product.setId(UUID.randomUUID());
        product.setName("Widget");

        inventory = new Inventory();
        inventory.setId(UUID.randomUUID());
        inventory.setProduct(product);
        inventory.setAvailableQuantity(100);
        inventory.setReservedQuantity(20);
        inventory.setSoldQuantity(10);
    }

    @Test
    void adjust_positive_increasesAvailable() {
        inventory.adjust(50);
        assertThat(inventory.getAvailableQuantity()).isEqualTo(150);
    }

    @Test
    void adjust_negative_decreasesAvailable() {
        inventory.adjust(-30);
        assertThat(inventory.getAvailableQuantity()).isEqualTo(70);
    }

    @Test
    void adjust_negativeMoreThanAvailable_throwsInsufficientInventoryException() {
        assertThatThrownBy(() -> inventory.adjust(-150))
                .isInstanceOf(InsufficientInventoryException.class)
                .hasMessageContaining("Cannot deduct 150 items");
    }

    @Test
    void reserve_validQuantity_updatesAvailableAndReserved() {
        inventory.reserve(30);
        assertThat(inventory.getAvailableQuantity()).isEqualTo(70);
        assertThat(inventory.getReservedQuantity()).isEqualTo(50);
    }

    @Test
    void reserve_moreThanAvailable_throwsInsufficientInventoryException() {
        assertThatThrownBy(() -> inventory.reserve(150))
                .isInstanceOf(InsufficientInventoryException.class)
                .hasMessageContaining("Insufficient stock");
    }

    @Test
    void reserve_zeroOrNegative_throwsIllegalArgumentException() {
        assertThatThrownBy(() -> inventory.reserve(0))
                .isInstanceOf(IllegalArgumentException.class);

        assertThatThrownBy(() -> inventory.reserve(-5))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void release_validQuantity_updatesReservedAndAvailable() {
        inventory.release(10);
        assertThat(inventory.getReservedQuantity()).isEqualTo(10);
        assertThat(inventory.getAvailableQuantity()).isEqualTo(110);
    }

    @Test
    void release_moreThanReserved_throwsIllegalStateException() {
        assertThatThrownBy(() -> inventory.release(50))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void confirm_validQuantity_updatesReservedAndSold() {
        inventory.confirm(15);
        assertThat(inventory.getReservedQuantity()).isEqualTo(5);
        assertThat(inventory.getSoldQuantity()).isEqualTo(25);
    }

    @Test
    void confirm_moreThanReserved_throwsIllegalStateException() {
        assertThatThrownBy(() -> inventory.confirm(50))
                .isInstanceOf(IllegalStateException.class);
    }
}
