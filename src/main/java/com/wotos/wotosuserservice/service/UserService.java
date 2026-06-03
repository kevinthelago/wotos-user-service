package com.wotos.wotosuserservice.service;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.wotos.wotosuserservice.dao.UserRepo;
import com.wotos.wotosuserservice.model.*;
import com.wotos.wotosuserservice.security.CustomUserDetailsService;
import com.wotos.wotosuserservice.security.Encoder;
import com.wotos.wotosuserservice.util.JwtUtil;
import org.json.simple.parser.JSONParser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.sql.SQLIntegrityConstraintViolationException;
import java.util.Optional;

@Service
public class UserService {

    private final ObjectMapper mapper = new ObjectMapper();
    private final JSONParser jsonParser = new JSONParser();
    private final RestTemplate restTemplate = new RestTemplate();
    private final HttpHeaders httpHeaders = new HttpHeaders();

    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private CustomUserDetailsService customUserDetailsService;

    @Autowired
    private JwtUtil jwtTokenUtil;

    @Autowired
    private Encoder encoder;

    @Autowired
    private UserRepo repo;

    @Autowired
    public UserService() {
        mapper.configure(
                DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false
        );
        httpHeaders.add("Web Token", "");
//        httpHeaders.add("Language", "");
//        httpHeaders.add("Last Modified", "");
    }

    /**
     * Authenticates a user and returns a freshly minted RS256 JWT.
     *
     * @return {@code 200 {"jwt": "..."}} on success; {@code 401} when the
     *         username is unknown or the password is wrong (the two are not
     *         distinguished, to avoid user enumeration).
     */
    public ResponseEntity<UserJwt> login(UserAuthenticationRequest userAuthenticationRequest) {
        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            userAuthenticationRequest.getUsername(),
                            userAuthenticationRequest.getPassword())
            );

            final UserDetails userDetails = customUserDetailsService
                    .loadUserByUsername(userAuthenticationRequest.getUsername());

            final String jwt = jwtTokenUtil.generateToken(userDetails);

            return ResponseEntity.ok(new UserJwt(jwt));
        } catch (UsernameNotFoundException | BadCredentialsException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
    }

    /**
     * Registers a new user. The password is BCrypt-encoded before storage and
     * role/active are set server-side so a client cannot self-assign privileges.
     *
     * <p>Bean-validation (username 3-32, password >= 10 chars) is enforced by
     * {@code @Valid} at the controller; a duplicate username surfaces as a
     * {@code DataIntegrityViolationException} which the global handler maps to a
     * {@code 409} error envelope.
     *
     * @return {@code 201} with the created user's view model.
     */
    public ResponseEntity<LocalUserViewModel> createLocalUser(LocalUser localUser) {
        localUser.setPassword(encoder.encode(localUser.getPassword()));
        localUser.setRoles("user");
        localUser.setActive(true);

        repo.save(localUser);

        LocalUserViewModel localUserViewModel = createViewModel(localUser);
        return new ResponseEntity<>(localUserViewModel, HttpStatus.CREATED);
    }

    private LocalUserViewModel createViewModel(LocalUser localUser) {
        return new LocalUserViewModel(
                localUser.getUsername(),
                localUser.isDark_mode()
        );
    }

}
