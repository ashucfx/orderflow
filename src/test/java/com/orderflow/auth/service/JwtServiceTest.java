package com.orderflow.auth.service;

import com.orderflow.auth.config.JwtProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class JwtServiceTest {

    private JwtService jwtService;

    @BeforeEach
    void setUp() {
        JwtProperties props = new JwtProperties();
        // 256-bit base64-encoded secret for testing
        props.setSecret("dGVzdC1zZWNyZXQtdGhhdC1pcy1sb25nLWVub3VnaC1mb3ItaG1hYy1zaGEyNTY=");
        props.setAccessTokenExpiryMs(900_000L);
        props.setRefreshTokenExpiryDays(7);
        jwtService = new JwtService(props);
    }

    @Test
    void generatedTokenIsValidAndContainsEmail() {
        String email = "user@example.com";
        String token = jwtService.generateAccessToken(email);

        assertThat(jwtService.isValid(token)).isTrue();
        assertThat(jwtService.extractEmail(token)).isEqualTo(email);
    }

    @Test
    void tamperedTokenIsRejected() {
        String token = jwtService.generateAccessToken("user@example.com");
        String tampered = token + "x";

        assertThat(jwtService.isValid(tampered)).isFalse();
    }

    @Test
    void arbitraryStringIsRejected() {
        assertThat(jwtService.isValid("not.a.token")).isFalse();
    }
}
