package com.wotos.wotosuserservice.controller;

import com.wotos.wotosuserservice.model.PreferencesViewModel;
import com.wotos.wotosuserservice.service.PreferencesService;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Read/update the authenticated user's preferences. Both endpoints require a
 * valid JWT; the owning user is taken from the authenticated principal, never
 * from the request body.
 */
@RestController
@RequestMapping("/users/me/preferences")
public class PreferencesController {

    private final PreferencesService preferencesService;

    public PreferencesController(PreferencesService preferencesService) {
        this.preferencesService = preferencesService;
    }

    @GetMapping
    public PreferencesViewModel getPreferences(Authentication authentication) {
        return preferencesService.get(authentication.getName());
    }

    @PutMapping
    public PreferencesViewModel updatePreferences(
            Authentication authentication,
            @Valid @RequestBody PreferencesViewModel preferences) {
        return preferencesService.update(authentication.getName(), preferences);
    }
}
