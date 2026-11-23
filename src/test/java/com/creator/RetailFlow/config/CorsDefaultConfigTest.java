package com.creator.RetailFlow.config;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/*
 * src/test/resources/application.properties shadows the main properties
 * file, so app.cors.allowed-origins is undefined here. This proves the
 * inline default in SecurityConfig keeps the Vite dev server working
 * instead of failing context startup.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class CorsDefaultConfigTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void allowsLocalViteOriginByDefault() throws Exception {

        mockMvc.perform(options("/api/sales")
                        .header(HttpHeaders.ORIGIN, "http://localhost:5173")
                        .header(
                                HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD,
                                "GET"
                        ))
                .andExpect(status().isOk())
                .andExpect(header().string(
                        HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN,
                        "http://localhost:5173"
                ));
    }
}
