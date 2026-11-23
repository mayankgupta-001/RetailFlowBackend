package com.creator.RetailFlow.config;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/*
 * Vercel mints a new origin per deployment, so wildcard patterns are
 * needed alongside the exact production origin. The negative cases
 * matter more than the positive ones here: an over-broad pattern is an
 * open door for any site that can register a similar hostname.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestPropertySource(properties = {
        "app.cors.allowed-origins=https://retailflow-frontend-drab.vercel.app",
        "app.cors.allowed-origin-patterns=https://retailflow-frontend-*.vercel.app"
})
class CorsPatternConfigTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void allowsExactProductionOrigin() throws Exception {

        assertPreflightAllowed(
                "https://retailflow-frontend-drab.vercel.app"
        );
    }

    @Test
    void allowsPreviewDeploymentOrigin() throws Exception {

        assertPreflightAllowed(
                "https://retailflow-frontend-9f8e7d-mayank.vercel.app"
        );

        assertPreflightAllowed(
                "https://retailflow-frontend-git-main-mayank.vercel.app"
        );
    }

    @Test
    void rejectsOriginWithoutTheProjectPrefix() throws Exception {

        // Matches "*.vercel.app" but not the project's own namespace.
        assertPreflightRejected("https://someone-else.vercel.app");
    }

    @Test
    void rejectsLookalikeDomain() throws Exception {

        // A pattern that is not anchored would let this through.
        assertPreflightRejected(
                "https://retailflow-frontend-drab.vercel.app.evil.com"
        );
    }

    private void assertPreflightAllowed(String origin) throws Exception {

        mockMvc.perform(options("/api/sales")
                        .header(HttpHeaders.ORIGIN, origin)
                        .header(
                                HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD,
                                "GET"
                        ))
                .andExpect(status().isOk())
                .andExpect(header().string(
                        HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN,
                        origin
                ));
    }

    private void assertPreflightRejected(String origin) throws Exception {

        mockMvc.perform(options("/api/sales")
                        .header(HttpHeaders.ORIGIN, origin)
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
