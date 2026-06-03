package com.wotos.wotosuserservice.util;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.JWSSigner;
import com.nimbusds.jose.JWSVerifier;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jose.crypto.RSASSAVerifier;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.KeyUse;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.interfaces.RSAPrivateCrtKey;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.RSAPublicKeySpec;
import java.text.ParseException;
import java.util.Base64;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * Issues and verifies RS256-signed JWTs and publishes the verification key as a
 * JWK set.
 *
 * <p>The 2048-bit RSA signing key is loaded from {@code jwt.private-key-pem}
 * (env {@code JWT_PRIVATE_KEY_PEM}; supplied as a Spring Cloud Config
 * {@code {cipher}} value in production). The PEM must be an unencrypted
 * <strong>PKCS#8</strong> private key ({@code -----BEGIN PRIVATE KEY-----}). When
 * no key is configured an ephemeral key pair is generated at startup so local
 * dev, tests, and a single-instance {@code docker compose up} work out of the
 * box — consumers fetch the matching public key from {@code /.well-known/jwks.json}
 * at runtime, so the ephemeral key is self-consistent. Configure a stable key in
 * any multi-instance deployment.
 *
 * <p>Token claims: {@code sub} (username), {@code iat}, {@code exp} (30 min
 * default), and {@code roles} (a JSON array of the user's authorities). The
 * {@code kid} header is the RFC 7638 thumbprint of the public key and matches
 * the {@code kid} of the published JWK.
 */
@Service
public class JwtUtil {

    private static final Logger log = LoggerFactory.getLogger(JwtUtil.class);

    /** Bit length for an ephemeral key when none is configured. */
    private static final int EPHEMERAL_KEY_BITS = 2048;

    private final RSAPrivateKey privateKey;
    private final RSAKey publicJwk;
    private final String keyId;
    private final long expirationMillis;

    public JwtUtil(
            @Value("${jwt.private-key-pem:}") String privateKeyPem,
            @Value("${jwt.expiration-millis:1800000}") long expirationMillis) {
        RSAPrivateCrtKey loadedPrivate = loadOrGenerateKey(privateKeyPem);
        this.privateKey = loadedPrivate;
        RSAPublicKey publicKey = derivePublicKey(loadedPrivate);
        try {
            this.publicJwk = new RSAKey.Builder(publicKey)
                    .keyUse(KeyUse.SIGNATURE)
                    .algorithm(JWSAlgorithm.RS256)
                    .keyIDFromThumbprint()
                    .build();
        } catch (JOSEException e) {
            throw new IllegalStateException("Failed to derive JWK from RSA public key", e);
        }
        this.keyId = publicJwk.getKeyID();
        this.expirationMillis = expirationMillis;
    }

    /** Generates an RS256 token for the given user, embedding their roles. */
    public String generateToken(UserDetails userDetails) {
        long now = System.currentTimeMillis();
        List<String> roles = userDetails.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .toList();
        JWTClaimsSet claims = new JWTClaimsSet.Builder()
                .subject(userDetails.getUsername())
                .issueTime(new Date(now))
                .expirationTime(new Date(now + expirationMillis))
                .claim("roles", roles)
                .build();
        SignedJWT signedJwt = new SignedJWT(
                new JWSHeader.Builder(JWSAlgorithm.RS256).keyID(keyId).build(),
                claims);
        try {
            JWSSigner signer = new RSASSASigner(privateKey);
            signedJwt.sign(signer);
        } catch (JOSEException e) {
            throw new IllegalStateException("Failed to sign JWT", e);
        }
        return signedJwt.serialize();
    }

    /** Returns the subject of a token whose RS256 signature verifies; throws otherwise. */
    public String extractUsername(String token) {
        return verify(token).getSubject();
    }

    /** Returns the expiry of a token whose RS256 signature verifies; throws otherwise. */
    public Date extractExpiration(String token) {
        return verify(token).getExpirationTime();
    }

    public Boolean validateToken(String token, UserDetails userDetails) {
        try {
            JWTClaimsSet claims = verify(token);
            return userDetails.getUsername().equals(claims.getSubject()) && !isExpired(claims);
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    /**
     * The public verification key as an RFC 7517 JWK set (public parameters only),
     * served verbatim at {@code /.well-known/jwks.json}.
     */
    public Map<String, Object> jwkSet() {
        return new JWKSet(publicJwk).toJSONObject();
    }

    private JWTClaimsSet verify(String token) {
        try {
            SignedJWT signedJwt = SignedJWT.parse(token);
            JWSVerifier verifier = new RSASSAVerifier(publicJwk.toRSAPublicKey());
            if (!signedJwt.verify(verifier)) {
                throw new IllegalArgumentException("JWT signature does not verify");
            }
            return signedJwt.getJWTClaimsSet();
        } catch (ParseException | JOSEException e) {
            throw new IllegalArgumentException("Malformed or unverifiable JWT", e);
        }
    }

    private boolean isExpired(JWTClaimsSet claims) {
        Date expiration = claims.getExpirationTime();
        return expiration == null || expiration.before(new Date());
    }

    private RSAPrivateCrtKey loadOrGenerateKey(String privateKeyPem) {
        if (privateKeyPem == null || privateKeyPem.isBlank()) {
            log.warn("jwt.private-key-pem is not set — generating an ephemeral RSA key pair. "
                    + "Tokens will not verify across restarts or instances. Configure "
                    + "JWT_PRIVATE_KEY_PEM (PKCS#8) for any non-local deployment.");
            return generateEphemeralKey();
        }
        return parsePkcs8PrivateKey(privateKeyPem);
    }

    private RSAPrivateCrtKey generateEphemeralKey() {
        try {
            KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
            generator.initialize(EPHEMERAL_KEY_BITS);
            KeyPair pair = generator.generateKeyPair();
            return (RSAPrivateCrtKey) pair.getPrivate();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("RSA key generation unavailable", e);
        }
    }

    private RSAPrivateCrtKey parsePkcs8PrivateKey(String pem) {
        String base64 = pem
                .replaceAll("-----BEGIN (.*)-----", "")
                .replaceAll("-----END (.*)-----", "")
                .replaceAll("\\s", "");
        byte[] der;
        try {
            der = Base64.getDecoder().decode(base64);
        } catch (IllegalArgumentException e) {
            throw new IllegalStateException(
                    "jwt.private-key-pem is not valid base64; expected an unencrypted PKCS#8 PEM", e);
        }
        try {
            KeyFactory keyFactory = KeyFactory.getInstance("RSA");
            return (RSAPrivateCrtKey) keyFactory.generatePrivate(new PKCS8EncodedKeySpec(der));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("RSA key factory unavailable", e);
        } catch (InvalidKeySpecException | ClassCastException e) {
            throw new IllegalStateException(
                    "jwt.private-key-pem must be an unencrypted PKCS#8 RSA private key "
                            + "(-----BEGIN PRIVATE KEY-----)", e);
        }
    }

    private RSAPublicKey derivePublicKey(RSAPrivateCrtKey privateCrtKey) {
        try {
            KeyFactory keyFactory = KeyFactory.getInstance("RSA");
            RSAPublicKeySpec publicSpec = new RSAPublicKeySpec(
                    privateCrtKey.getModulus(), privateCrtKey.getPublicExponent());
            return (RSAPublicKey) keyFactory.generatePublic(publicSpec);
        } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
            throw new IllegalStateException("Failed to derive RSA public key from private key", e);
        }
    }

}
