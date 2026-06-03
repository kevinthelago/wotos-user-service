package com.wotos.wotosuserservice.controller;

import com.wotos.wotosuserservice.model.LocalUser;
import com.wotos.wotosuserservice.model.LocalUserViewModel;
import com.wotos.wotosuserservice.model.UserAuthenticationRequest;
import com.wotos.wotosuserservice.model.UserJwt;
import com.wotos.wotosuserservice.security.CustomUserDetailsService;
import com.wotos.wotosuserservice.service.UserService;
import com.wotos.wotosuserservice.util.JwtUtil;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/users")
public class UserController {

    @Autowired
    UserService userService;

    @PostMapping("/login")
    public ResponseEntity<UserJwt> createAuthenticationToken(@RequestBody UserAuthenticationRequest userAuthenticationRequest) {
        return userService.login(userAuthenticationRequest);
    }

    @PostMapping("/create")
    public ResponseEntity<LocalUserViewModel> createLocalUser(@Valid @RequestBody LocalUser localUser) {
        return userService.createLocalUser(localUser);
    }

    @GetMapping("/hello")
    public ResponseEntity<String> hw() {
        return ResponseEntity.ok("hello world");
    }

}
