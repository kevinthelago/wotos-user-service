package com.wotos.wotosuserservice.service;

import com.wotos.wotosuserservice.dao.PreferencesRepo;
import com.wotos.wotosuserservice.model.Preferences;
import com.wotos.wotosuserservice.model.PreferencesViewModel;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;

@Service
public class PreferencesService {

    private final PreferencesRepo repo;

    public PreferencesService(PreferencesRepo repo) {
        this.repo = repo;
    }

    /** Current preferences for the user, or sensible defaults if none are stored yet. */
    @Transactional(readOnly = true)
    public PreferencesViewModel get(String username) {
        return repo.findByUsername(username)
                .map(PreferencesService::toViewModel)
                .orElseGet(PreferencesViewModel::defaults);
    }

    /** Upserts the user's preferences and returns the persisted result. */
    @Transactional
    public PreferencesViewModel update(String username, PreferencesViewModel request) {
        Preferences preferences = repo.findByUsername(username)
                .orElseGet(() -> {
                    Preferences fresh = new Preferences();
                    fresh.setUsername(username);
                    return fresh;
                });
        preferences.setDarkTheme(request.isDarkTheme());
        preferences.setSavedNicknames(request.getSavedNicknames() == null
                ? new ArrayList<>() : new ArrayList<>(request.getSavedNicknames()));
        preferences.setLastGarageVehicleId(request.getLastGarageVehicleId());
        return toViewModel(repo.save(preferences));
    }

    private static PreferencesViewModel toViewModel(Preferences preferences) {
        return new PreferencesViewModel(
                preferences.isDarkTheme(),
                new ArrayList<>(preferences.getSavedNicknames()),
                preferences.getLastGarageVehicleId());
    }
}
