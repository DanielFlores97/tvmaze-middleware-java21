package com.example.pruebatecnica.infrastructure.web;

import com.example.pruebatecnica.application.GetShowDetailsService;
import com.example.pruebatecnica.config.WebConfiguration;
import com.example.pruebatecnica.domain.AppException;
import com.example.pruebatecnica.domain.Comment;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ShowController.class)
@Import(WebConfiguration.class)
class ShowControllerTest {
    @Autowired MockMvc mvc;
    @MockitoBean GetShowDetailsService service;

    @Test
    void returnsFullShowWithoutEnvelope() throws Exception {
        when(service.findById(1)).thenReturn(Map.of("id", 1, "name", "Show",
                "futureField", Map.of("nested", true), "_links", Map.of("self", Map.of("href", "https://example.test")),
                "comments", List.of(new Comment("Good", new BigDecimal("4.5")))));
        mvc.perform(get("/api/v1/show").param("show_id", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.futureField.nested").value(true))
                .andExpect(jsonPath("$._links.self.href").value("https://example.test"))
                .andExpect(jsonPath("$.comments[0].comment").value("Good"))
                .andExpect(jsonPath("$.comments[0].rating").value(4.5))
                .andExpect(jsonPath("$.comments[0].showId").doesNotExist())
                .andExpect(jsonPath("$.attributes").doesNotExist());
    }

    @Test
    void validatesShowId() throws Exception {
        mvc.perform(get("/api/v1/show")).andExpect(status().isBadRequest());
        mvc.perform(get("/api/v1/show").param("show_id", "0")).andExpect(status().isBadRequest());
        mvc.perform(get("/api/v1/show").param("show_id", "-1")).andExpect(status().isBadRequest());
        mvc.perform(get("/api/v1/show").param("show_id", "abc")).andExpect(status().isBadRequest());
        verifyNoInteractions(service);
    }

    @Test
    void notFoundIsLocalizedAndCorrelated() throws Exception {
        when(service.findById(9)).thenThrow(new AppException(AppException.Code.SHOW_NOT_FOUND));
        mvc.perform(get("/api/v1/show").param("show_id", "9").header("Accept-Language", "en-US")
                        .header("X-Request-ID", "test-show"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").value("The requested show was not found."))
                .andExpect(jsonPath("$.requestId").value("test-show"));
    }
}
