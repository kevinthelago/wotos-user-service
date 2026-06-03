package com.wotos.wotosuserservice.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Covers the JWT-protected preferences endpoints (P2-D5): unauthenticated access
 * is rejected, a fresh user reads defaults, and an update round-trips.
 */
@SpringBootTest
@AutoConfigureMockMvc
class PreferencesControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private String credentials(String username, String password) {
        return "{\"username\":\"" + username + "\",\"password\":\"" + password + "\"}";
    }

    /** Registers a user and returns a bearer JWT for them. */
    private String registerAndLogin(String username) throws Exception {
        mockMvc.perform(post("/users/create")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(credentials(username, "averysecurepw")))
                .andExpect(status().isCreated());
        String response = mockMvc.perform(post("/users/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(credentials(username, "averysecurepw")))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(response).get("jwt").asText();
    }

    @Test
    void preferencesRequireAuthentication() throws Exception {
        mockMvc.perform(get("/users/me/preferences"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void freshUserGetsDefaultPreferences() throws Exception {
        String jwt = registerAndLogin("frank");

        mockMvc.perform(get("/users/me/preferences").header("Authorization", "Bearer " + jwt))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.darkTheme").value(false))
                .andExpect(jsonPath("$.savedNicknames").isEmpty())
                .andExpect(jsonPath("$.lastGarageVehicleId").doesNotExist());
    }

    @Test
    void updateRoundTripsThroughGet() throws Exception {
        String jwt = registerAndLogin("grace");

        mockMvc.perform(put("/users/me/preferences")
                        .header("Authorization", "Bearer " + jwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"darkTheme\":true,\"savedNicknames\":[\"Tankzilla\",\"BushWookie\"],"
                                + "\"lastGarageVehicleId\":42}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.darkTheme").value(true))
                .andExpect(jsonPath("$.savedNicknames.length()").value(2))
                .andExpect(jsonPath("$.lastGarageVehicleId").value(42));

        mockMvc.perform(get("/users/me/preferences").header("Authorization", "Bearer " + jwt))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.darkTheme").value(true))
                .andExpect(jsonPath("$.savedNicknames[0]").value("Tankzilla"))
                .andExpect(jsonPath("$.lastGarageVehicleId").value(42));
    }

    @Test
    void moreThan25NicknamesIsRejected() throws Exception {
        String jwt = registerAndLogin("heidi");
        String nicknames = IntStream.rangeClosed(1, 26)
                .mapToObj(i -> "\"nick" + i + "\"")
                .collect(Collectors.joining(","));

        mockMvc.perform(put("/users/me/preferences")
                        .header("Authorization", "Bearer " + jwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"darkTheme\":false,\"savedNicknames\":[" + nicknames + "]}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("validation_error"));
    }
}
