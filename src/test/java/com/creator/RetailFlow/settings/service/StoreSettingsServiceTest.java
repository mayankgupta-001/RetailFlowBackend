package com.creator.RetailFlow.settings.service;

import com.creator.RetailFlow.settings.dto.StoreSettingsRequest;
import com.creator.RetailFlow.settings.entity.StoreSettings;
import com.creator.RetailFlow.settings.repository.StoreSettingsRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StoreSettingsServiceTest {

    @Mock
    private StoreSettingsRepository repository;

    @InjectMocks
    private StoreSettingsService service;

    @Test
    void getCreatesDefaultRowWhenTableIsEmpty() {

        when(repository.findFirstByOrderByIdAsc())
                .thenReturn(Optional.empty());

        when(repository.save(any(StoreSettings.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        StoreSettings settings = service.get();

        assertThat(settings.getShopName()).isEqualTo("RetailFlow");
        assertThat(settings.getGstin()).isNull();

        verify(repository).save(any(StoreSettings.class));
    }

    @Test
    void updateTrimsFieldsAndNormalizesGstin() {

        StoreSettings existing = new StoreSettings();
        existing.setShopName("RetailFlow");

        when(repository.findFirstByOrderByIdAsc())
                .thenReturn(Optional.of(existing));

        when(repository.save(any(StoreSettings.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        StoreSettingsRequest request = new StoreSettingsRequest();
        request.setShopName("  My Shop  ");
        request.setAddress("  12 Main Street  ");
        request.setPhone("   ");
        request.setStateCode("19");
        request.setGstin("  19aabcu9603r1zm ");

        StoreSettings updated = service.update(request);

        assertThat(updated.getShopName()).isEqualTo("My Shop");
        assertThat(updated.getAddress()).isEqualTo("12 Main Street");
        assertThat(updated.getPhone()).isNull();
        assertThat(updated.getStateCode()).isEqualTo("19");
        assertThat(updated.getGstin()).isEqualTo("19AABCU9603R1ZM");
    }

    @Test
    void blankGstinMeansUnregistered() {

        StoreSettings existing = new StoreSettings();
        existing.setShopName("RetailFlow");
        existing.setGstin("19AABCU9603R1ZM");

        when(repository.findFirstByOrderByIdAsc())
                .thenReturn(Optional.of(existing));

        when(repository.save(any(StoreSettings.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        StoreSettingsRequest request = new StoreSettingsRequest();
        request.setShopName("RetailFlow");
        request.setGstin("   ");

        assertThat(service.update(request).getGstin()).isNull();
    }

    @Test
    void rejectsInvalidGstin() {

        StoreSettings existing = new StoreSettings();
        existing.setShopName("RetailFlow");

        when(repository.findFirstByOrderByIdAsc())
                .thenReturn(Optional.of(existing));

        StoreSettingsRequest request = new StoreSettingsRequest();
        request.setShopName("RetailFlow");
        request.setGstin("NOT-A-GSTIN");

        assertThatThrownBy(() -> service.update(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage(
                        "GSTIN must be a valid 15-character GST identification number"
                );
    }

    @Test
    void rejectsInvalidStateCode() {

        StoreSettings existing = new StoreSettings();
        existing.setShopName("RetailFlow");

        when(repository.findFirstByOrderByIdAsc())
                .thenReturn(Optional.of(existing));

        StoreSettingsRequest request = new StoreSettingsRequest();
        request.setShopName("RetailFlow");
        request.setStateCode("WB");

        assertThatThrownBy(() -> service.update(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("State code must be two digits");
    }
}
