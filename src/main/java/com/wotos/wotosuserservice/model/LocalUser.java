package com.wotos.wotosuserservice.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Entity
@Table(name = "users")
public class LocalUser {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int user_id;
    @Size(min = 3, max = 32, message = "username must be 3-32 characters")
    @NotBlank(message = "username is required")
    @Column(unique = true)
    private String username;
    // Validated on the raw submitted password at request binding; after the
    // service BCrypt-encodes it the stored hash (~60 chars) still satisfies the
    // bounds, so JPA's pre-persist validation also passes.
    @Size(min = 10, max = 255, message = "password must be at least 10 characters")
    @NotBlank(message = "password is required")
    private String password;
    private boolean active;
    private String roles;
    private boolean dark_mode;

    public LocalUser() {}

    public LocalUser(int user_id, String username, String password, boolean active, String roles, boolean dark_mode) {
        this.user_id = user_id;
        this.username = username;
        this.password = password;
        this.active = active;
        this.roles = roles;
        this.dark_mode = dark_mode;
    }

    public LocalUser(LocalUser localUser) {
    }

    public int getUser_id() {
        return user_id;
    }

    public void setUser_id(int user_id) {
        this.user_id = user_id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public String getRoles() {
        return roles;
    }

    public void setRoles(String roles) {
        this.roles = roles;
    }

    public boolean isDark_mode() {
        return dark_mode;
    }

    public void setDark_mode(boolean dark_mode) {
        this.dark_mode = dark_mode;
    }
}
