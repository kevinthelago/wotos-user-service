package com.wotos.wotosuserservice.model;

import jakarta.validation.constraints.Size;

import java.util.ArrayList;
import java.util.List;

/**
 * Request/response shape for {@code /users/me/preferences}. Excludes the internal
 * id and the owning username (which is derived from the JWT, never the body).
 */
public class PreferencesViewModel {

    private boolean darkTheme;

    @Size(max = 25, message = "at most 25 saved nicknames are allowed")
    private List<String> savedNicknames = new ArrayList<>();

    private Long lastGarageVehicleId;

    public PreferencesViewModel() {}

    public PreferencesViewModel(boolean darkTheme, List<String> savedNicknames, Long lastGarageVehicleId) {
        this.darkTheme = darkTheme;
        this.savedNicknames = savedNicknames == null ? new ArrayList<>() : savedNicknames;
        this.lastGarageVehicleId = lastGarageVehicleId;
    }

    /** Defaults returned for a user who has never saved preferences. */
    public static PreferencesViewModel defaults() {
        return new PreferencesViewModel(false, new ArrayList<>(), null);
    }

    public boolean isDarkTheme() {
        return darkTheme;
    }

    public void setDarkTheme(boolean darkTheme) {
        this.darkTheme = darkTheme;
    }

    public List<String> getSavedNicknames() {
        return savedNicknames;
    }

    public void setSavedNicknames(List<String> savedNicknames) {
        this.savedNicknames = savedNicknames;
    }

    public Long getLastGarageVehicleId() {
        return lastGarageVehicleId;
    }

    public void setLastGarageVehicleId(Long lastGarageVehicleId) {
        this.lastGarageVehicleId = lastGarageVehicleId;
    }
}
