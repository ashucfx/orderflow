package com.orderflow.user.controller;

import com.orderflow.auth.util.SecurityUtils;
import com.orderflow.common.api.ApiResponse;
import com.orderflow.common.api.PagedResponse;
import com.orderflow.user.dto.AssignRolesRequest;
import com.orderflow.user.dto.UpdateProfileRequest;
import com.orderflow.user.dto.UserProfileResponse;
import com.orderflow.user.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
@Tag(name = "Users", description = "User profile and administration endpoints")
@SecurityRequirement(name = "BearerAuth")
public class UserController {

    private final UserService userService;

    @GetMapping("/me")
    @Operation(summary = "Get the authenticated user's profile")
    public ResponseEntity<ApiResponse<UserProfileResponse>> getCurrentUser() {
        String email = SecurityUtils.getCurrentUserEmail();
        UserProfileResponse profile = userService.getProfile(email);
        return ResponseEntity.ok(ApiResponse.ok(profile));
    }

    @PutMapping("/me")
    @Operation(summary = "Update the authenticated user's profile")
    public ResponseEntity<ApiResponse<UserProfileResponse>> updateCurrentUser(
            @Valid @RequestBody UpdateProfileRequest request) {
        String email = SecurityUtils.getCurrentUserEmail();
        UserProfileResponse updated = userService.updateProfile(email, request);
        return ResponseEntity.ok(ApiResponse.ok("Profile updated successfully", updated));
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Get all users with pagination (ADMIN only)")
    public ResponseEntity<ApiResponse<PagedResponse<UserProfileResponse>>> getAllUsers(
            @PageableDefault(size = 20) Pageable pageable) {
        PagedResponse<UserProfileResponse> users = userService.getAllUsers(pageable);
        return ResponseEntity.ok(ApiResponse.ok(users));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Get user details by ID (ADMIN only)")
    public ResponseEntity<ApiResponse<UserProfileResponse>> getUserById(@PathVariable UUID id) {
        UserProfileResponse user = userService.getUserById(id);
        return ResponseEntity.ok(ApiResponse.ok(user));
    }

    @PutMapping("/{id}/roles")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Assign roles to a user (ADMIN only)")
    public ResponseEntity<ApiResponse<UserProfileResponse>> assignRoles(
            @PathVariable UUID id,
            @Valid @RequestBody AssignRolesRequest request) {
        UserProfileResponse user = userService.assignRoles(id, request);
        return ResponseEntity.ok(ApiResponse.ok("Roles assigned successfully", user));
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Enable or disable a user account (ADMIN only)")
    public ResponseEntity<ApiResponse<UserProfileResponse>> setUserStatus(
            @PathVariable UUID id,
            @RequestParam boolean enabled) {
        UserProfileResponse user = userService.setUserStatus(id, enabled);
        String message = enabled ? "User account enabled" : "User account disabled";
        return ResponseEntity.ok(ApiResponse.ok(message, user));
    }
}
