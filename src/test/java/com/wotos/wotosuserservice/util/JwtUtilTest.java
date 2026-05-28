package com.wotos.wotosuserservice.util;

import org.junit.jupiter.api.Test;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Round-trip coverage for the jjwt 0.12 migration and the issue #6 secret
 * externalization: a token produced by {@link JwtUtil#generateToken} must
 * verify and parse back with the same injected signing key, and the
 * constructor must reject any secret below the 256-bit HS256 floor.
 */
class JwtUtilTest {

    private static final String TEST_SECRET =
            "test-secret-key-must-be-at-least-32-bytes-long-for-hs256";
    private static final long TEST_EXPIRATION_MILLIS = 600_000L;

    private final JwtUtil jwtUtil = new JwtUtil(TEST_SECRET, TEST_EXPIRATION_MILLIS);

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

    @Test
    void constructorRejectsSecretShorterThanTheHs256Floor() {
        assertThrows(IllegalStateException.class,
                () -> new JwtUtil("too-short", TEST_EXPIRATION_MILLIS));
    }

    @Test
    void constructorRejectsMissingSecret() {
        assertThrows(IllegalStateException.class,
                () -> new JwtUtil("", TEST_EXPIRATION_MILLIS));
    }

}
