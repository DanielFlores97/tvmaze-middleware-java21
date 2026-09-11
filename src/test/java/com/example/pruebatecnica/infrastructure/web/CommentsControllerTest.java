package com.example.pruebatecnica.infrastructure.web;

import com.example.pruebatecnica.application.AddCommentService;
import com.example.pruebatecnica.config.WebConfiguration;
import com.example.pruebatecnica.domain.AppException;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(CommentsController.class)
@Import(WebConfiguration.class)
class CommentsControllerTest {
    @Autowired MockMvc mvc;
    @MockitoBean AddCommentService service;

    @ParameterizedTest
    @ValueSource(strings = {"0", "5", "4.5"})
    void acceptsRatingBoundariesAndDecimals(String rating) throws Exception {
        mvc.perform(post("/api/v1/comments").contentType("application/json")
                        .content("{\"show_id\":1,\"comment\":\"Good\",\"rating\":" + rating + "}"))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.status").value("created"));
        verify(service).add(1, "Good", new BigDecimal(rating));
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "{}",
            "{\"show_id\":1,\"comment\":\"Good\",\"rating\":-1}",
            "{\"show_id\":1,\"comment\":\"Good\",\"rating\":5.01}",
            "{\"show_id\":1,\"comment\":\"  \",\"rating\":3}",
            "{\"show_id\":0,\"comment\":\"Good\",\"rating\":3}",
            "{\"show_id\":1.5,\"comment\":\"Good\",\"rating\":3}",
            "{\"show_id\":1,\"comment\":\"Good\",\"rating\":null}",
            "{"
    })
    void rejectsInvalidBody(String body) throws Exception {
        mvc.perform(post("/api/v1/comments").contentType("application/json").content(body))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith("application/problem+json"));
        verifyNoInteractions(service);
    }

    @Test
    void databaseFailureDoesNotReportCreated() throws Exception {
        doThrow(new AppException(AppException.Code.DATABASE_UNAVAILABLE))
                .when(service).add(1, "Good", BigDecimal.ONE);
        mvc.perform(post("/api/v1/comments").contentType("application/json")
                        .content("{\"show_id\":1,\"comment\":\"Good\",\"rating\":1}"))
                .andExpect(status().isServiceUnavailable());
    }
}
