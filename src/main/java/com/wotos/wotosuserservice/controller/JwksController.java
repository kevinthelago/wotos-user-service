package com.wotos.wotosuserservice.controller;

import com.wotos.wotosuserservice.util.JwtUtil;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Publishes the RS256 verification key as a JWK set so downstream services
 * (e.g. the edge gateway) can validate tokens this service issues without
 * sharing a secret. Unauthenticated by design — it exposes only public key
 * material.
 */
@RestController
public class JwksController {

    private final JwtUtil jwtUtil;

    public JwksController(JwtUtil jwtUtil) {
        this.jwtUtil = jwtUtil;
    }

    @GetMapping(value = "/.well-known/jwks.json", produces = MediaType.APPLICATION_JSON_VALUE)
    public Map<String, Object> jwks() {
        return jwtUtil.jwkSet();
    }

}
