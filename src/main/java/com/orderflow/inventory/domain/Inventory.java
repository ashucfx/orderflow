package com.orderflow.inventory.domain;

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

    public void reserve(int quantity) {
        if (availableQuantity < quantity) {
            throw new IllegalStateException("Insufficient stock for product: " + product.getId());
        }
        availableQuantity -= quantity;
        reservedQuantity += quantity;
    }

    public void release(int quantity) {
        reservedQuantity -= quantity;
        availableQuantity += quantity;
    }

    public void confirm(int quantity) {
        reservedQuantity -= quantity;
        soldQuantity += quantity;
    }
}
