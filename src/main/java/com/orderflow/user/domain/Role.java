package com.orderflow.user.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "roles")
public class Role {

    @Id
    @Column(name = "id")
    private Short id;

    @Column(name = "name", nullable = false, unique = true, length = 30)
    @Enumerated(EnumType.STRING)
    private RoleName name;

    public enum RoleName {
        CUSTOMER, ADMIN, INVENTORY_MANAGER
    }
}
