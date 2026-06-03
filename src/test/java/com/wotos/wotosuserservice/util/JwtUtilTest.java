package com.wotos.wotosuserservice.util;

import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jwt.SignedJWT;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;

import java.security.KeyPairGenerator;
import java.security.interfaces.RSAPrivateKey;
import java.util.Base64;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Covers the RS256 migration (issue P2-D1): tokens are signed with RS256, carry
 * the agreed claims, round-trip against the in-memory key, and the published JWK
 * set exposes only the matching public key.
 */
class JwtUtilTest {

    private static final long TEST_EXPIRATION_MILLIS = 600_000L;

    /** No PEM configured -> JwtUtil generates an ephemeral key pair for the test. */
    private final JwtUtil jwtUtil = new JwtUtil("", TEST_EXPIRATION_MILLIS);

    private UserDetails user(String username) {
        return new User(username, "password", Collections.emptyList());
    }

    private UserDetails userWithRoles(String username, String... roles) {
        return new User(username, "password",
                List.of(roles).stream().map(SimpleGrantedAuthority::new).toList());
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
    void tokenIsSignedWithRs256AndCarriesRoles() throws Exception {
        String token = jwtUtil.generateToken(userWithRoles("tanker", "user", "admin"));

        SignedJWT parsed = SignedJWT.parse(token);
        assertEquals(JWSAlgorithm.RS256, parsed.getHeader().getAlgorithm());
        assertNotNull(parsed.getHeader().getKeyID());
        // Spring's User sorts authorities, so compare order-independently.
        assertEquals(java.util.Set.of("user", "admin"),
                java.util.Set.copyOf(parsed.getJWTClaimsSet().getStringListClaim("roles")));
    }

    @Test
    void validateTokenRejectsAGarbageToken() {
        assertFalse(jwtUtil.validateToken("not-a-jwt", user("tanker")));
    }

    @Test
    @SuppressWarnings("unchecked")
    void jwkSetExposesOnlyThePublicKeyMatchingTheTokenKid() throws Exception {
        String token = jwtUtil.generateToken(user("tanker"));
        String tokenKid = SignedJWT.parse(token).getHeader().getKeyID();

        Map<String, Object> jwks = jwtUtil.jwkSet();
        List<Map<String, Object>> keys = (List<Map<String, Object>>) jwks.get("keys");

        assertEquals(1, keys.size());
        Map<String, Object> key = keys.get(0);
        assertEquals("RSA", key.get("kty"));
        assertEquals("sig", key.get("use"));
        assertEquals(tokenKid, key.get("kid"));
        // Public JWK must never leak the private exponent or CRT factors.
        assertFalse(key.containsKey("d"));
        assertFalse(key.containsKey("p"));
        assertFalse(key.containsKey("q"));
    }

    @Test
    void aConfiguredPkcs8PemKeyIsUsedForSigning() throws Exception {
        KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(2048);
        RSAPrivateKey privateKey = (RSAPrivateKey) generator.generateKeyPair().getPrivate();
        String pem = "-----BEGIN PRIVATE KEY-----\n"
                + Base64.getMimeEncoder().encodeToString(privateKey.getEncoded())
                + "\n-----END PRIVATE KEY-----";

        JwtUtil configured = new JwtUtil(pem, TEST_EXPIRATION_MILLIS);

        String token = configured.generateToken(user("tanker"));
        assertEquals("tanker", configured.extractUsername(token));
        // A token from the ephemeral instance must NOT verify under the configured key.
        assertFalse(configured.validateToken(jwtUtil.generateToken(user("tanker")), user("tanker")));
    }

}
