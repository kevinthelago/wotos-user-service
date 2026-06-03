- P1-D4: shared maven.yml template at wotos-config/.github/workflow-templates/ does not exist yet (secure-align hasn't landed it); based CI on the peer statistics-service maven.yml pattern + a GHCR build-push job. Tests run on H2 so no mysql service container needed.

- P2-D1: login now returns {jwt} in body (was a viewmodel + 'Web Token' header); JWTs are RS256 with claims sub/iat/exp(30m)/roles; JWKS at GET /.well-known/jwks.json. Filter now reads Authorization: Bearer. Bad creds -> 401 (no user enumeration). RSA key from JWT_PRIVATE_KEY_PEM (PKCS#8); ephemeral key generated if unset (dev/test/single-instance only). Edge must fetch JWKS to validate.

