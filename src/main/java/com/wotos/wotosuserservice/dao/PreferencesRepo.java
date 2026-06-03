package com.wotos.wotosuserservice.dao;

import com.wotos.wotosuserservice.model.Preferences;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PreferencesRepo extends JpaRepository<Preferences, Long> {

    Optional<Preferences> findByUsername(String username);

}
