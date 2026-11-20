package com.creator.RetailFlow.settings.repository;

import com.creator.RetailFlow.settings.entity.StoreSettings;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import static org.assertj.core.api.Assertions.assertThat;

/*
 * Guards the store_settings mapping against schema regressions, and
 * proves the single-row accessor used by the service works.
 */
@DataJpaTest
@ActiveProfiles("test")
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:store_settings_test;MODE=PostgreSQL;DB_CLOSE_DELAY=-1"
})
class StoreSettingsRepositoryTest {

    @Autowired
    private StoreSettingsRepository repository;

    @Autowired
    private EntityManager entityManager;

    @Test
    void persistsAndReadsStoreSettings() {

        StoreSettings settings = new StoreSettings();
        settings.setShopName("My Shop");
        settings.setAddress("12 Main Street\nKolkata");
        settings.setPhone("9876543210");
        settings.setStateCode("19");
        settings.setGstin("19AABCU9603R1ZM");

        repository.saveAndFlush(settings);
        entityManager.clear();

        StoreSettings reloaded = repository.findFirstByOrderByIdAsc()
                .orElseThrow();

        assertThat(reloaded.getShopName()).isEqualTo("My Shop");
        assertThat(reloaded.getAddress()).isEqualTo("12 Main Street\nKolkata");
        assertThat(reloaded.getPhone()).isEqualTo("9876543210");
        assertThat(reloaded.getStateCode()).isEqualTo("19");
        assertThat(reloaded.getGstin()).isEqualTo("19AABCU9603R1ZM");
        assertThat(reloaded.getUpdatedAt()).isNotNull();
    }

    @Test
    void returnsEmptyWhenNoRowExists() {
        assertThat(repository.findFirstByOrderByIdAsc()).isEmpty();
    }
}
