package com.wotos.wotosuserservice.util;

import org.junit.jupiter.api.Test;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Round-trip coverage for the jjwt 0.12 migration: a token produced by
 * {@link JwtUtil#generateToken} must verify and parse back with the same signing key.
 */
class JwtUtilTest {

    private final JwtUtil jwtUtil = new JwtUtil();

    private UserDetails user(String username) {
        return new User(username, "password", Collections.emptyList());
    }

    @Test
    void generatedTokenRoundTripsToTheSameUsername() {
        String token = jwtUtil.generateToken(user("tanker"));

        assertNotNull(token);
        assertEquals("tanker", jwtUtil.extractUsername(token));
    }

    @Test
    void validateTokenAcceptsAFreshTokenForTheSameUser() {
        UserDetails userDetails = user("tanker");
        String token = jwtUtil.generateToken(userDetails);

        assertTrue(jwtUtil.validateToken(token, userDetails));
    }

    @Test
    void validateTokenRejectsADifferentUser() {
        String token = jwtUtil.generateToken(user("tanker"));

        assertFalse(jwtUtil.validateToken(token, user("intruder")));
    }

    @Test
    void generatedTokenExpiresInTheFuture() {
        String token = jwtUtil.generateToken(user("tanker"));

        assertTrue(jwtUtil.extractExpiration(token).getTime() > System.currentTimeMillis());
    }

}
