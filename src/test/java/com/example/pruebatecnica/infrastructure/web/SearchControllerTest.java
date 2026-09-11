package com.example.pruebatecnica.infrastructure.web;

import com.example.pruebatecnica.application.SearchShowsService;
import com.example.pruebatecnica.config.WebConfiguration;
import com.example.pruebatecnica.domain.AppException;
import com.example.pruebatecnica.domain.ShowSearchResult;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(SearchController.class)
@Import(WebConfiguration.class)
class SearchControllerTest {
    @Autowired MockMvc mvc;
    @MockitoBean SearchShowsService service;

    @Test
    void returnsArrayWithRequiredFields() throws Exception {
        when(service.search("girls")).thenReturn(List.of(
                new ShowSearchResult(1, "Girls", "HBO", null, List.of("Drama"), List.of())));
        mvc.perform(get("/api/v1/search").param("search_query", "girls").header("X-Request-ID", "search-1"))
                .andExpect(status().isOk())
                .andExpect(header().string("X-Request-ID", "search-1"))
                .andExpect(jsonPath("$[0].name").value("Girls"))
                .andExpect(jsonPath("$[0].channel").value("HBO"))
                .andExpect(jsonPath("$[0].genres[0]").value("Drama"))
                .andExpect(jsonPath("$[0].comments").isArray())
                .andExpect(jsonPath("$[0].comments").isEmpty())
                .andExpect(jsonPath("$[0].score").doesNotExist());
    }

    @Test
    void validatesMissingBlankAndLongQuery() throws Exception {
        mvc.perform(get("/api/v1/search")).andExpect(status().isBadRequest());
        mvc.perform(get("/api/v1/search").param("search_query", "  ")).andExpect(status().isBadRequest());
        mvc.perform(get("/api/v1/search").param("search_query", "a".repeat(201)))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(service);
    }

    @Test
    void rateLimitHasRetryAfterAndLocalizedProblem() throws Exception {
        when(service.search("show")).thenThrow(new AppException(AppException.Code.UPSTREAM_RATE_LIMITED));
        mvc.perform(get("/api/v1/search").param("search_query", "show").header("Accept-Language", "en"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(header().string("Retry-After", "10"))
                .andExpect(content().contentTypeCompatibleWith("application/problem+json"))
                .andExpect(jsonPath("$.code").value("UPSTREAM_RATE_LIMITED"))
                .andExpect(jsonPath("$.detail").value("TVMaze rate limit reached. Please retry shortly."));
    }
}
