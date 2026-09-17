package com.orderflow.user.controller;

import com.orderflow.auth.config.SecurityConfig;
import com.orderflow.auth.filter.JwtAuthenticationFilter;
import com.orderflow.auth.service.JwtService;
import com.orderflow.common.api.PagedResponse;
import com.orderflow.user.domain.Role;
import com.orderflow.user.dto.AssignRolesRequest;
import com.orderflow.user.dto.UpdateProfileRequest;
import com.orderflow.user.dto.UserProfileResponse;
import com.orderflow.user.service.UserService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(UserController.class)
@Import({SecurityConfig.class, JwtAuthenticationFilter.class})
@ActiveProfiles("test")
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private UserService userService;

    @MockBean
    private JwtService jwtService;

    @MockBean
    private UserDetailsService userDetailsService;

    @Test
    void getMe_whenUnauthenticated_returns401() throws Exception {
        mockMvc.perform(get("/api/v1/users/me"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(username = "alice@example.com", roles = {"CUSTOMER"})
    void getMe_whenAuthenticatedCustomer_returnsProfile() throws Exception {
        UUID userId = UUID.randomUUID();
        UserProfileResponse profile = UserProfileResponse.builder()
                .id(userId)
                .email("alice@example.com")
                .firstName("Alice")
                .lastName("Smith")
                .enabled(true)
                .roles(Set.of("CUSTOMER"))
                .createdAt(Instant.now())
                .build();

        when(userService.getProfile("alice@example.com")).thenReturn(profile);

        mockMvc.perform(get("/api/v1/users/me"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.email").value("alice@example.com"))
                .andExpect(jsonPath("$.data.firstName").value("Alice"));
    }

    @Test
    @WithMockUser(username = "alice@example.com", roles = {"CUSTOMER"})
    void updateMe_whenValidRequest_returnsUpdatedProfile() throws Exception {
        UpdateProfileRequest request = new UpdateProfileRequest("Alice", "Johnson");
        UserProfileResponse updated = UserProfileResponse.builder()
                .id(UUID.randomUUID())
                .email("alice@example.com")
                .firstName("Alice")
                .lastName("Johnson")
                .enabled(true)
                .roles(Set.of("CUSTOMER"))
                .createdAt(Instant.now())
                .build();

        when(userService.updateProfile(eq("alice@example.com"), any(UpdateProfileRequest.class)))
                .thenReturn(updated);

        mockMvc.perform(put("/api/v1/users/me")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.lastName").value("Johnson"));
    }

    @Test
    @WithMockUser(username = "customer@example.com", roles = {"CUSTOMER"})
    void getAllUsers_whenCustomerRole_returns403Forbidden() throws Exception {
        mockMvc.perform(get("/api/v1/users"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "admin@example.com", roles = {"ADMIN"})
    void getAllUsers_whenAdminRole_returns200Ok() throws Exception {
        UserProfileResponse user = UserProfileResponse.builder()
                .id(UUID.randomUUID())
                .email("customer@example.com")
                .firstName("Customer")
                .lastName("User")
                .enabled(true)
                .roles(Set.of("CUSTOMER"))
                .createdAt(Instant.now())
                .build();

        PagedResponse<UserProfileResponse> pagedResponse = PagedResponse.from(
                new PageImpl<>(List.of(user), PageRequest.of(0, 20), 1)
        );

        when(userService.getAllUsers(any())).thenReturn(pagedResponse);

        mockMvc.perform(get("/api/v1/users"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content[0].email").value("customer@example.com"));
    }

    @Test
    @WithMockUser(username = "customer@example.com", roles = {"CUSTOMER"})
    void assignRoles_whenCustomerRole_returns403Forbidden() throws Exception {
        AssignRolesRequest request = new AssignRolesRequest(Set.of(Role.RoleName.ADMIN));

        mockMvc.perform(put("/api/v1/users/{id}/roles", UUID.randomUUID())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "admin@example.com", roles = {"ADMIN"})
    void assignRoles_whenAdminRole_returns200Ok() throws Exception {
        UUID targetId = UUID.randomUUID();
        AssignRolesRequest request = new AssignRolesRequest(Set.of(Role.RoleName.ADMIN, Role.RoleName.INVENTORY_MANAGER));
        UserProfileResponse updated = UserProfileResponse.builder()
                .id(targetId)
                .email("promoted@example.com")
                .firstName("Promoted")
                .lastName("User")
                .enabled(true)
                .roles(Set.of("ADMIN", "INVENTORY_MANAGER"))
                .createdAt(Instant.now())
                .build();

        when(userService.assignRoles(eq(targetId), any(AssignRolesRequest.class))).thenReturn(updated);

        mockMvc.perform(put("/api/v1/users/{id}/roles", targetId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.roles").isArray());
    }

    @Test
    @WithMockUser(username = "customer@example.com", roles = {"CUSTOMER"})
    void setUserStatus_whenCustomerRole_returns403Forbidden() throws Exception {
        mockMvc.perform(patch("/api/v1/users/{id}/status", UUID.randomUUID())
                        .param("enabled", "false"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "admin@example.com", roles = {"ADMIN"})
    void setUserStatus_whenAdminRole_returns200Ok() throws Exception {
        UUID targetId = UUID.randomUUID();
        UserProfileResponse updated = UserProfileResponse.builder()
                .id(targetId)
                .email("target@example.com")
                .firstName("Target")
                .lastName("User")
                .enabled(false)
                .roles(Set.of("CUSTOMER"))
                .createdAt(Instant.now())
                .build();

        when(userService.setUserStatus(targetId, false)).thenReturn(updated);

        mockMvc.perform(patch("/api/v1/users/{id}/status", targetId)
                        .param("enabled", "false"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.enabled").value(false));
    }
}
