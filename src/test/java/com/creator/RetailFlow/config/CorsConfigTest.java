package com.creator.RetailFlow.config;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/*
 * CORS is configured from app.cors.allowed-origins. A missing method
 * here breaks the browser while curl keeps working, so preflight is
 * asserted for PATCH (the discounts status toggle) explicitly.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestPropertySource(properties = {
        "app.cors.allowed-origins=https://shop.example.com/ , https://admin.example.com"
})
class CorsConfigTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void allowsPatchPreflightForConfiguredOrigin() throws Exception {

        mockMvc.perform(options("/api/discounts/1/status")
                        .header(HttpHeaders.ORIGIN, "https://admin.example.com")
                        .header(
                                HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD,
                                "PATCH"
                        ))
                .andExpect(status().isOk())
                .andExpect(header().string(
                        HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN,
                        "https://admin.example.com"
                ))
                .andExpect(header().string(
                        HttpHeaders.ACCESS_CONTROL_ALLOW_METHODS,
                        containsString("PATCH")
                ));
    }

    @Test
    void ignoresTrailingSlashInConfiguration() throws Exception {

        // Configured as "https://shop.example.com/", but the browser
        // sends the origin without a trailing slash.
        mockMvc.perform(options("/api/products")
                        .header(HttpHeaders.ORIGIN, "https://shop.example.com")
                        .header(
                                HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD,
                                "GET"
                        ))
                .andExpect(status().isOk())
                .andExpect(header().string(
                        HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN,
                        "https://shop.example.com"
                ));
    }

    @Test
    void rejectsUnknownOrigin() throws Exception {

        mockMvc.perform(options("/api/products")
                        .header(HttpHeaders.ORIGIN, "https://evil.example.com")
                        .header(
                                HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD,
                                "GET"
                        ))
                .andExpect(status().isForbidden())
                .andExpect(header().doesNotExist(
                        HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN
                ));
    }
}
