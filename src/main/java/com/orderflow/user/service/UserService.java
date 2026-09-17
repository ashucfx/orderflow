package com.orderflow.user.service;

import com.orderflow.common.api.PagedResponse;
import com.orderflow.user.dto.AssignRolesRequest;
import com.orderflow.user.dto.UpdateProfileRequest;
import com.orderflow.user.dto.UserProfileResponse;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface UserService {

    UserProfileResponse getProfile(String email);

    UserProfileResponse updateProfile(String email, UpdateProfileRequest request);

    PagedResponse<UserProfileResponse> getAllUsers(Pageable pageable);

    UserProfileResponse getUserById(UUID id);

    UserProfileResponse assignRoles(UUID targetUserId, AssignRolesRequest request);

    UserProfileResponse setUserStatus(UUID targetUserId, boolean enabled);
}
