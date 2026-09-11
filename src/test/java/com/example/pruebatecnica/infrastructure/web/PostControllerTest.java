package com.example.pruebatecnica.infrastructure.web;

import com.example.pruebatecnica.application.PostService;
import com.example.pruebatecnica.config.WebConfiguration;
import com.example.pruebatecnica.domain.AppException;
import com.example.pruebatecnica.domain.Post;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(PostController.class)
@Import(WebConfiguration.class)
class PostControllerTest {
    @Autowired
    private MockMvc mvc;
    @MockitoBean
    private PostService service;

    @Test
    void returnsPostAndPropagatesRequestId() throws Exception {
        when(service.findById(1)).thenReturn(new Post(1, 2, "Title", "Body"));
        mvc.perform(get("/api/v1/posts/1").header("X-Request-ID", "test-123"))
                .andExpect(status().isOk())
                .andExpect(header().string("X-Request-ID", "test-123"))
                .andExpect(jsonPath("$.id").value(1));
        assertThat(MDC.get("requestId")).isNull();
    }

    @Test
    void defaultsToSpanish() throws Exception {
        when(service.findById(1)).thenThrow(new AppException(AppException.Code.POST_NOT_FOUND));
        mvc.perform(get("/api/v1/posts/1"))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith("application/problem+json"))
                .andExpect(jsonPath("$.detail").value("No se encontro la publicacion solicitada."))
                .andExpect(jsonPath("$.code").value("POST_NOT_FOUND"))
                .andExpect(jsonPath("$.requestId").isNotEmpty());
    }

    @Test
    void translatesEnglishIncludingRegionalLocale() throws Exception {
        when(service.findById(1)).thenThrow(new AppException(AppException.Code.POST_NOT_FOUND));
        mvc.perform(get("/api/v1/posts/1").header("Accept-Language", "en-US"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").value("The requested post was not found."));
    }

    @Test
    void rejectsNonNumericId() throws Exception {
        mvc.perform(get("/api/v1/posts/abc").header("Accept-Language", "en"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("The request contains invalid parameters."));
        verifyNoInteractions(service);
    }

    @Test
    void rejectsNonPositiveId() throws Exception {
        mvc.perform(get("/api/v1/posts/0")).andExpect(status().isBadRequest());
        verifyNoInteractions(service);
    }

    @Test
    void invalidatesCache() throws Exception {
        mvc.perform(delete("/api/v1/posts/1/cache")).andExpect(status().isNoContent());
        verify(service).evict(1);
    }

    @Test
    void reportsFailedInvalidation() throws Exception {
        doThrow(new AppException(AppException.Code.CACHE_UNAVAILABLE)).when(service).evict(1);
        mvc.perform(delete("/api/v1/posts/1/cache"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.code").value("CACHE_UNAVAILABLE"));
    }

    @Test
    void preservesMethodNotAllowedAndAllowHeader() throws Exception {
        mvc.perform(post("/api/v1/posts/1"))
                .andExpect(status().isMethodNotAllowed())
                .andExpect(header().exists("Allow"))
                .andExpect(jsonPath("$.code").value("HTTP_405"));
    }

    @Test
    void returnsGatewayTimeout() throws Exception {
        when(service.findById(1)).thenThrow(new AppException(AppException.Code.UPSTREAM_TIMEOUT));
        mvc.perform(get("/api/v1/posts/1")).andExpect(status().isGatewayTimeout());
    }

    @Test
    void hidesUnexpectedExceptionDetails() throws Exception {
        when(service.findById(1)).thenThrow(new IllegalStateException("private-detail"));
        mvc.perform(get("/api/v1/posts/1"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.detail").value("Ocurrio un error interno."))
                .andExpect(jsonPath("$.code").value("INTERNAL_ERROR"));
    }

    @Test
    void replacesUnsafeRequestId() throws Exception {
        mvc.perform(get("/api/v1/posts/0").header("X-Request-ID", "invalid request id"))
                .andExpect(header().string("X-Request-ID", org.hamcrest.Matchers.matchesPattern(
                        "[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}")));
    }
}
