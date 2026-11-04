package com.creator.RetailFlow.sale.controller;

import com.creator.RetailFlow.auth.service.JwtService;
import com.creator.RetailFlow.sale.dto.DiscountRequest;
import com.creator.RetailFlow.sale.dto.DiscountResponse;
import com.creator.RetailFlow.sale.entity.DiscountType;
import com.creator.RetailFlow.sale.service.DiscountService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(DiscountController.class)
@Import(DiscountControllerTest.MethodSecurityConfig.class)
class DiscountControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private DiscountService discountService;

    @MockitoBean
    private JwtService jwtService;

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    void adminCanCreateDiscount() throws Exception {

        DiscountResponse response = new DiscountResponse(
                1L,
                "Weekend Sale",
                DiscountType.PERCENTAGE,
                new BigDecimal("20.00"),
                LocalDateTime.of(2026, 10, 30, 0, 0),
                LocalDateTime.of(2026, 10, 30, 23, 59, 59),
                true,
                LocalDateTime.now(),
                LocalDateTime.now()
        );

        when(discountService.create(any(DiscountRequest.class)))
                .thenReturn(response);

        mockMvc.perform(post("/api/discounts")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Weekend Sale",
                                  "type": "PERCENTAGE",
                                  "value": 20.00,
                                  "startAt": "2026-10-30T00:00:00",
                                  "endAt": "2026-10-30T23:59:59"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.name").value("Weekend Sale"))
                .andExpect(jsonPath("$.type").value("PERCENTAGE"))
                .andExpect(jsonPath("$.value").value(20.00))
                .andExpect(jsonPath("$.active").value(true));

        verify(discountService)
                .create(any(DiscountRequest.class));
    }

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    void adminCanGetAllDiscounts() throws Exception {

        DiscountResponse response = new DiscountResponse(
                1L,
                "Weekend Sale",
                DiscountType.PERCENTAGE,
                new BigDecimal("20.00"),
                LocalDateTime.of(2026, 10, 30, 0, 0),
                LocalDateTime.of(2026, 10, 30, 23, 59, 59),
                true,
                LocalDateTime.now(),
                LocalDateTime.now()
        );

        when(discountService.getAll())
                .thenReturn(List.of(response));

        mockMvc.perform(get("/api/discounts"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].name").value("Weekend Sale"))
                .andExpect(jsonPath("$[0].type").value("PERCENTAGE"));

        verify(discountService)
                .getAll();
    }

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    void adminCanGetDiscountById() throws Exception {

        DiscountResponse response = new DiscountResponse(
                1L,
                "Weekend Sale",
                DiscountType.PERCENTAGE,
                new BigDecimal("20.00"),
                LocalDateTime.of(2026, 10, 30, 0, 0),
                LocalDateTime.of(2026, 10, 30, 23, 59, 59),
                true,
                LocalDateTime.now(),
                LocalDateTime.now()
        );

        when(discountService.getById(1L))
                .thenReturn(response);

        mockMvc.perform(get("/api/discounts/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.name").value("Weekend Sale"));

        verify(discountService)
                .getById(1L);
    }

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    void adminCanUpdateDiscount() throws Exception {

        DiscountResponse response = new DiscountResponse(
                1L,
                "Updated Sale",
                DiscountType.FIXED,
                new BigDecimal("500.00"),
                LocalDateTime.of(2026, 10, 30, 0, 0),
                LocalDateTime.of(2026, 10, 30, 23, 59, 59),
                true,
                LocalDateTime.now(),
                LocalDateTime.now()
        );

        when(discountService.update(eq(1L), any(DiscountRequest.class)))
                .thenReturn(response);

        mockMvc.perform(put("/api/discounts/1")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Updated Sale",
                                  "type": "FIXED",
                                  "value": 500.00,
                                  "startAt": "2026-10-30T00:00:00",
                                  "endAt": "2026-10-30T23:59:59"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.name").value("Updated Sale"))
                .andExpect(jsonPath("$.type").value("FIXED"))
                .andExpect(jsonPath("$.value").value(500.00));

        verify(discountService)
                .update(eq(1L), any(DiscountRequest.class));
    }

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    void adminCanDisableDiscount() throws Exception {

        DiscountResponse response = new DiscountResponse(
                1L,
                "Weekend Sale",
                DiscountType.PERCENTAGE,
                new BigDecimal("20.00"),
                LocalDateTime.of(2026, 10, 30, 0, 0),
                LocalDateTime.of(2026, 10, 30, 23, 59, 59),
                false,
                LocalDateTime.now(),
                LocalDateTime.now()
        );

        when(discountService.updateStatus(1L, false))
                .thenReturn(response);

        mockMvc.perform(patch("/api/discounts/1/status")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "active": false
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.active").value(false));

        verify(discountService)
                .updateStatus(1L, false);
    }

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    void adminCanDeleteDiscount() throws Exception {

        doNothing()
                .when(discountService)
                .delete(1L);

        mockMvc.perform(delete("/api/discounts/1")
                        .with(csrf()))
                .andExpect(status().isNoContent());

        verify(discountService)
                .delete(1L);
    }

    @Test
    @WithMockUser(username = "cashier", roles = "CASHIER")
    void cashierCannotCreateDiscount() throws Exception {

        mockMvc.perform(post("/api/discounts")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Weekend Sale",
                                  "type": "PERCENTAGE",
                                  "value": 20.00,
                                  "startAt": "2026-10-30T00:00:00",
                                  "endAt": "2026-10-30T23:59:59"
                                }
                                """))
                .andExpect(status().isForbidden());

        verifyNoInteractions(discountService);
    }

    @Test
    @WithMockUser(username = "cashier", roles = "CASHIER")
    void cashierCannotUpdateDiscount() throws Exception {

        mockMvc.perform(put("/api/discounts/1")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Updated Sale",
                                  "type": "FIXED",
                                  "value": 500.00,
                                  "startAt": "2026-10-30T00:00:00",
                                  "endAt": "2026-10-30T23:59:59"
                                }
                                """))
                .andExpect(status().isForbidden());

        verifyNoInteractions(discountService);
    }

    @Test
    @WithMockUser(username = "cashier", roles = "CASHIER")
    void cashierCannotDeleteDiscount() throws Exception {

        mockMvc.perform(delete("/api/discounts/1")
                        .with(csrf()))
                .andExpect(status().isForbidden());

        verifyNoInteractions(discountService);
    }

    @EnableMethodSecurity
    static class MethodSecurityConfig {
    }
}