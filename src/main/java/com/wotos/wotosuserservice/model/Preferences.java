package com.wotos.wotosuserservice.model;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;

import java.util.ArrayList;
import java.util.List;

/**
 * Per-user UI/state preferences, keyed by username (one row per user). Persisted
 * separately from {@link LocalUser} so the auth record stays focused on identity.
 */
@Entity
@Table(name = "user_preferences")
public class Preferences {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String username;

    private boolean darkTheme;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "user_saved_nicknames",
            joinColumns = @JoinColumn(name = "preferences_id"))
    @Column(name = "nickname")
    private List<String> savedNicknames = new ArrayList<>();

    /** Last vehicle viewed in the garage; null until the user opens one. */
    private Long lastGarageVehicleId;

    public Preferences() {}

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
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
