- P1-D4: shared maven.yml template at wotos-config/.github/workflow-templates/ does not exist yet (secure-align hasn't landed it); based CI on the peer statistics-service maven.yml pattern + a GHCR build-push job. Tests run on H2 so no mysql service container needed.

- P2-D1: login now returns {jwt} in body (was a viewmodel + 'Web Token' header); JWTs are RS256 with claims sub/iat/exp(30m)/roles; JWKS at GET /.well-known/jwks.json. Filter now reads Authorization: Bearer. Bad creds -> 401 (no user enumeration). RSA key from JWT_PRIVATE_KEY_PEM (PKCS#8); ephemeral key generated if unset (dev/test/single-instance only). Edge must fetch JWKS to validate.

- P2-D2: registration now BCrypt-encodes passwords (was storing raw -> login was broken); cost factor 12. roles/active set server-side ('user'/true) to block privilege self-assignment. Validation: username 3-32, password >=10; errors use {error:{code,message}} envelope (400 validation_error, 409 username_taken).

