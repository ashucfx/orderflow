package com.orderflow.user.service;

import com.orderflow.common.api.PagedResponse;
import com.orderflow.common.exception.ResourceNotFoundException;
import com.orderflow.user.domain.Role;
import com.orderflow.user.domain.User;
import com.orderflow.user.dto.AssignRolesRequest;
import com.orderflow.user.dto.UpdateProfileRequest;
import com.orderflow.user.dto.UserProfileResponse;
import com.orderflow.user.repository.RoleRepository;
import com.orderflow.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;

    @Override
    public UserProfileResponse getProfile(String email) {
        User user = findUserByEmail(email);
        return UserProfileResponse.fromEntity(user);
    }

    @Override
    @Transactional
    public UserProfileResponse updateProfile(String email, UpdateProfileRequest request) {
        User user = findUserByEmail(email);
        user.setFirstName(request.getFirstName().trim());
        user.setLastName(request.getLastName().trim());
        User savedUser = userRepository.save(user);
        return UserProfileResponse.fromEntity(savedUser);
    }

    @Override
    public PagedResponse<UserProfileResponse> getAllUsers(Pageable pageable) {
        Page<UserProfileResponse> page = userRepository.findAll(pageable)
                .map(UserProfileResponse::fromEntity);
        return PagedResponse.from(page);
    }

    @Override
    public UserProfileResponse getUserById(UUID id) {
        User user = findUserById(id);
        return UserProfileResponse.fromEntity(user);
    }

    @Override
    @Transactional
    public UserProfileResponse assignRoles(UUID targetUserId, AssignRolesRequest request) {
        User user = findUserById(targetUserId);

        Set<Role> resolvedRoles = new HashSet<>();
        for (Role.RoleName roleName : request.getRoles()) {
            Role role = roleRepository.findByName(roleName)
                    .orElseThrow(() -> new ResourceNotFoundException("Role", roleName));
            resolvedRoles.add(role);
        }

        user.setRoles(resolvedRoles);
        User savedUser = userRepository.save(user);
        return UserProfileResponse.fromEntity(savedUser);
    }

    @Override
    @Transactional
    public UserProfileResponse setUserStatus(UUID targetUserId, boolean enabled) {
        User user = findUserById(targetUserId);
        user.setEnabled(enabled);
        User savedUser = userRepository.save(user);
        return UserProfileResponse.fromEntity(savedUser);
    }

    private User findUserByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + email));
    }

    private User findUserById(UUID id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User", id));
    }
}
