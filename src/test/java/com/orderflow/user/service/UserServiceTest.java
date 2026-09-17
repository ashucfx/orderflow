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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private RoleRepository roleRepository;

    private UserServiceImpl userService;

    private User sampleUser;
    private Role customerRole;
    private Role adminRole;

    @BeforeEach
    void setUp() {
        userService = new UserServiceImpl(userRepository, roleRepository);

        customerRole = new Role();
        customerRole.setId((short) 1);
        customerRole.setName(Role.RoleName.CUSTOMER);

        adminRole = new Role();
        adminRole.setId((short) 2);
        adminRole.setName(Role.RoleName.ADMIN);

        sampleUser = new User();
        sampleUser.setId(UUID.randomUUID());
        sampleUser.setEmail("john@example.com");
        sampleUser.setFirstName("John");
        sampleUser.setLastName("Doe");
        sampleUser.setEnabled(true);
        sampleUser.addRole(customerRole);
    }

    @Test
    void getProfile_whenUserExists_returnsProfile() {
        when(userRepository.findByEmail("john@example.com")).thenReturn(Optional.of(sampleUser));

        UserProfileResponse profile = userService.getProfile("john@example.com");

        assertThat(profile).isNotNull();
        assertThat(profile.getEmail()).isEqualTo("john@example.com");
        assertThat(profile.getFirstName()).isEqualTo("John");
        assertThat(profile.getLastName()).isEqualTo("Doe");
        assertThat(profile.getRoles()).containsExactly("CUSTOMER");
    }

    @Test
    void getProfile_whenUserNotFound_throwsResourceNotFoundException() {
        when(userRepository.findByEmail("notfound@example.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.getProfile("notfound@example.com"))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void updateProfile_whenUserExists_updatesNameAndReturnsProfile() {
        when(userRepository.findByEmail("john@example.com")).thenReturn(Optional.of(sampleUser));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        UpdateProfileRequest request = new UpdateProfileRequest("Johnny", "Smith");
        UserProfileResponse updated = userService.updateProfile("john@example.com", request);

        assertThat(updated.getFirstName()).isEqualTo("Johnny");
        assertThat(updated.getLastName()).isEqualTo("Smith");
        verify(userRepository).save(sampleUser);
    }

    @Test
    void getAllUsers_returnsPagedResponse() {
        Pageable pageable = PageRequest.of(0, 10);
        when(userRepository.findAll(pageable)).thenReturn(new PageImpl<>(List.of(sampleUser), pageable, 1));

        PagedResponse<UserProfileResponse> result = userService.getAllUsers(pageable);

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getTotalElements()).isEqualTo(1);
        assertThat(result.getPage()).isEqualTo(0);
    }

    @Test
    void getUserById_whenUserExists_returnsProfile() {
        UUID id = sampleUser.getId();
        when(userRepository.findById(id)).thenReturn(Optional.of(sampleUser));

        UserProfileResponse profile = userService.getUserById(id);

        assertThat(profile.getId()).isEqualTo(id);
        assertThat(profile.getEmail()).isEqualTo("john@example.com");
    }

    @Test
    void getUserById_whenNotFound_throwsResourceNotFoundException() {
        UUID id = UUID.randomUUID();
        when(userRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.getUserById(id))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void assignRoles_success() {
        UUID id = sampleUser.getId();
        when(userRepository.findById(id)).thenReturn(Optional.of(sampleUser));
        when(roleRepository.findByName(Role.RoleName.ADMIN)).thenReturn(Optional.of(adminRole));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        AssignRolesRequest request = new AssignRolesRequest(Set.of(Role.RoleName.ADMIN));
        UserProfileResponse response = userService.assignRoles(id, request);

        assertThat(response.getRoles()).containsExactly("ADMIN");
        verify(userRepository).save(sampleUser);
    }

    @Test
    void assignRoles_whenRoleNotFound_throwsResourceNotFoundException() {
        UUID id = sampleUser.getId();
        when(userRepository.findById(id)).thenReturn(Optional.of(sampleUser));
        when(roleRepository.findByName(Role.RoleName.ADMIN)).thenReturn(Optional.empty());

        AssignRolesRequest request = new AssignRolesRequest(Set.of(Role.RoleName.ADMIN));

        assertThatThrownBy(() -> userService.assignRoles(id, request))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void setUserStatus_success() {
        UUID id = sampleUser.getId();
        when(userRepository.findById(id)).thenReturn(Optional.of(sampleUser));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        UserProfileResponse response = userService.setUserStatus(id, false);

        assertThat(response.isEnabled()).isFalse();
        verify(userRepository).save(sampleUser);
    }
}
