package com.creator.RetailFlow.settings.service;

import com.creator.RetailFlow.settings.dto.StoreSettingsRequest;
import com.creator.RetailFlow.settings.entity.StoreSettings;
import com.creator.RetailFlow.settings.repository.StoreSettingsRepository;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.util.regex.Pattern;

@Service
public class StoreSettingsService {

    private static final String DEFAULT_SHOP_NAME = "RetailFlow";

    // 15 characters: 2-digit state code, 5-letter PAN, 4 digits, entity
    // code, 'Z', checksum. Format-only check - the GST portal is the
    // authority on whether the number is live.
    private static final Pattern GSTIN_PATTERN = Pattern.compile(
            "^[0-9]{2}[A-Z]{5}[0-9]{4}[A-Z][1-9A-Z]Z[0-9A-Z]$"
    );

    private final StoreSettingsRepository repository;

    public StoreSettingsService(StoreSettingsRepository repository) {
        this.repository = repository;
    }

    /*
     * Returns the single settings row, creating a default one on first
     * use. The lazy fallback also covers databases whose schema was
     * created by Hibernate in tests (where the seed row from the
     * migration does not exist).
     */
    @Transactional
    public StoreSettings get() {
        return repository.findFirstByOrderByIdAsc()
                .orElseGet(this::createDefault);
    }

    @Transactional
    public StoreSettings update(StoreSettingsRequest request) {

        StoreSettings settings = get();

        settings.setShopName(request.getShopName().trim());
        settings.setAddress(blankToNull(request.getAddress()));
        settings.setPhone(blankToNull(request.getPhone()));

        String stateCode = blankToNull(request.getStateCode());

        if (stateCode != null && !stateCode.matches("\\d{2}")) {
            throw new IllegalArgumentException(
                    "State code must be two digits"
            );
        }

        settings.setStateCode(stateCode);
        settings.setGstin(normalizeGstin(request.getGstin()));

        return repository.save(settings);
    }

    private StoreSettings createDefault() {

        StoreSettings settings = new StoreSettings();
        settings.setShopName(DEFAULT_SHOP_NAME);

        return repository.save(settings);
    }

    private String normalizeGstin(String gstin) {

        String normalized = blankToNull(gstin);

        if (normalized == null) {
            return null;
        }

        normalized = normalized.toUpperCase();

        if (!GSTIN_PATTERN.matcher(normalized).matches()) {
            throw new IllegalArgumentException(
                    "GSTIN must be a valid 15-character GST identification number"
            );
        }

        return normalized;
    }

    private String blankToNull(String value) {

        if (value == null) {
            return null;
        }

        String trimmed = value.trim();

        return trimmed.isEmpty() ? null : trimmed;
    }
}
