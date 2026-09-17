package com.orderflow.user.dto;

import com.orderflow.user.domain.Role;
import com.orderflow.user.domain.User;
import lombok.Builder;
import lombok.Getter;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Getter
@Builder
public class UserProfileResponse {

    private final UUID id;
    private final String email;
    private final String firstName;
    private final String lastName;
    private final boolean enabled;
    private final Set<String> roles;
    private final Instant createdAt;
    private final Instant updatedAt;

    public static UserProfileResponse fromEntity(User user) {
        Set<String> roleNames = user.getRoles().stream()
                .map(Role::getName)
                .map(Enum::name)
                .collect(Collectors.toSet());

        return UserProfileResponse.builder()
                .id(user.getId())
                .email(user.getEmail())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .enabled(user.isEnabled())
                .roles(roleNames)
                .createdAt(user.getCreatedAt())
                .updatedAt(user.getUpdatedAt())
                .build();
    }
}
