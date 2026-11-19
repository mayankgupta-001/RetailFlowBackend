package com.creator.RetailFlow.settings.repository;

import com.creator.RetailFlow.settings.entity.StoreSettings;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface StoreSettingsRepository
        extends JpaRepository<StoreSettings, Long> {

    Optional<StoreSettings> findFirstByOrderByIdAsc();
}
