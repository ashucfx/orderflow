package com.orderflow.inventory.domain;

import com.orderflow.common.exception.InsufficientInventoryException;
import com.orderflow.product.domain.Product;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "inventory")
public class Inventory {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id", nullable = false, unique = true)
    private Product product;

    @Column(name = "available_quantity", nullable = false)
    private int availableQuantity;

    @Column(name = "reserved_quantity", nullable = false)
    private int reservedQuantity;

    @Column(name = "sold_quantity", nullable = false)
    private int soldQuantity;

    @Version
    @Column(name = "version", nullable = false)
    private Long version;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    @PreUpdate
    protected void onUpdate() {
        updatedAt = Instant.now();
    }

    public void adjust(int amount) {
        if (amount < 0 && availableQuantity < Math.abs(amount)) {
            throw new InsufficientInventoryException(
                    "Cannot deduct " + Math.abs(amount) + " items. Available stock is: " + availableQuantity
            );
        }
        this.availableQuantity += amount;
    }

    public void reserve(int quantity) {
        if (quantity <= 0) {
            throw new IllegalArgumentException("Reservation quantity must be greater than zero");
        }
        if (availableQuantity < quantity) {
            throw new InsufficientInventoryException(
                    "Insufficient stock for product. Requested: " + quantity + ", available: " + availableQuantity
            );
        }
        availableQuantity -= quantity;
        reservedQuantity += quantity;
    }

    public void release(int quantity) {
        if (quantity <= 0) {
            throw new IllegalArgumentException("Release quantity must be greater than zero");
        }
        if (reservedQuantity < quantity) {
            throw new IllegalStateException("Cannot release more than currently reserved: " + reservedQuantity);
        }
        reservedQuantity -= quantity;
        availableQuantity += quantity;
    }

    public void confirm(int quantity) {
        if (quantity <= 0) {
            throw new IllegalArgumentException("Confirmation quantity must be greater than zero");
        }
        if (reservedQuantity < quantity) {
            throw new IllegalStateException("Cannot confirm more than currently reserved: " + reservedQuantity);
        }
        reservedQuantity -= quantity;
        soldQuantity += quantity;
    }
}
