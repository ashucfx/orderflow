package com.orderflow.auth.util;

import com.orderflow.common.exception.UnauthorizedException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SecurityUtilsTest {

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void getCurrentUserEmail_withUserDetails_returnsEmail() {
        User principal = new User("alice@example.com", "password", List.of(new SimpleGrantedAuthority("ROLE_CUSTOMER")));
        Authentication auth = new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(auth);
        SecurityContextHolder.setContext(context);

        assertThat(SecurityUtils.getCurrentUserEmail()).isEqualTo("alice@example.com");
    }

    @Test
    void getCurrentUserEmail_withStringPrincipal_returnsEmail() {
        Authentication auth = new UsernamePasswordAuthenticationToken("bob@example.com", null, List.of());
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(auth);
        SecurityContextHolder.setContext(context);

        assertThat(SecurityUtils.getCurrentUserEmail()).isEqualTo("bob@example.com");
    }

    @Test
    void getCurrentUserEmail_whenUnauthenticated_throwsUnauthorizedException() {
        SecurityContextHolder.clearContext();

        assertThatThrownBy(SecurityUtils::getCurrentUserEmail)
                .isInstanceOf(UnauthorizedException.class);
    }

    @Test
    void getCurrentUserEmail_whenAnonymousUser_throwsUnauthorizedException() {
        Authentication auth = new UsernamePasswordAuthenticationToken("anonymousUser", null, List.of());
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(auth);
        SecurityContextHolder.setContext(context);

        assertThatThrownBy(SecurityUtils::getCurrentUserEmail)
                .isInstanceOf(UnauthorizedException.class);
    }
}
