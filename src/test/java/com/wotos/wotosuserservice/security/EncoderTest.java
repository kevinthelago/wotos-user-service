package com.wotos.wotosuserservice.security;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Confirms the BCrypt cost factor meets the audited floor of 12 (P2-D2).
 */
class EncoderTest {

    private final Encoder encoder = new Encoder();

    @Test
    void encodesWithBcryptCostFactorAtLeast12() {
        assertTrue(Encoder.BCRYPT_STRENGTH >= 12);

        String hash = encoder.encode("password123");
        // BCrypt hashes embed the cost factor: $2a$<cost>$...
        assertTrue(hash.startsWith("$2a$12$"), "expected a cost-12 BCrypt hash, got: " + hash);
        assertTrue(encoder.matches("password123", hash));
    }
}
