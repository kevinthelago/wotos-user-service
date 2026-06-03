package com.wotos.wotosuserservice.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * End-to-end coverage of registration + login through the real security chain:
 * password encoding makes login work (P2-D2/P2-D1), validation and duplicate
 * usernames return the standard error envelope, and login returns {@code {jwt}}.
 */
@SpringBootTest
@AutoConfigureMockMvc
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    private String body(String username, String password) {
        return "{\"username\":\"" + username + "\",\"password\":\"" + password + "\"}";
    }

    @Test
    void registrationReturns201AndDoesNotEchoThePassword() throws Exception {
        mockMvc.perform(post("/users/create")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body("alice", "averysecurepw")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.username").value("alice"))
                .andExpect(jsonPath("$.password").doesNotExist());
    }

    @Test
    void registeredUserCanLogInAndReceiveAJwt() throws Exception {
        mockMvc.perform(post("/users/create")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body("bob", "averysecurepw")))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/users/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body("bob", "averysecurepw")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.jwt").isNotEmpty());
    }

    @Test
    void duplicateUsernameReturns409Envelope() throws Exception {
        mockMvc.perform(post("/users/create")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body("carol", "averysecurepw")))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/users/create")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body("carol", "anotherpw12")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code").value("username_taken"));
    }

    @Test
    void shortPasswordReturns400Envelope() throws Exception {
        mockMvc.perform(post("/users/create")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body("dave", "short")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("validation_error"));
    }

    @Test
    void shortUsernameReturns400Envelope() throws Exception {
        mockMvc.perform(post("/users/create")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body("ab", "averysecurepw")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("validation_error"));
    }

    @Test
    void wrongPasswordReturns401() throws Exception {
        mockMvc.perform(post("/users/create")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body("erin", "averysecurepw")))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/users/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body("erin", "wrongpassword")))
                .andExpect(status().isUnauthorized());
    }
}
