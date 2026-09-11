package com.example.pruebatecnica.integration;

import com.example.pruebatecnica.domain.Show;
import com.example.pruebatecnica.domain.ShowSummary;
import com.example.pruebatecnica.infrastructure.cache.ShowCacheDocument;
import com.example.pruebatecnica.infrastructure.http.TvMazeClient;
import com.example.pruebatecnica.infrastructure.persistence.CommentDocument;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ThreadLocalRandom;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class AtlasFlowIT {
    @Autowired MockMvc mvc;
    @Autowired MongoTemplate mongo;
    @MockitoBean TvMazeClient provider;
    private final long showId = ThreadLocalRandom.current().nextLong(1_000_000_000L, Long.MAX_VALUE);

    @DynamicPropertySource
    static void atlasProperties(DynamicPropertyRegistry registry) {
        String uri = Objects.requireNonNull(System.getenv("ATLAS_TEST_URI"),
                "Set ATLAS_TEST_URI to run the atlas-integration profile");
        String database = System.getenv().getOrDefault("ATLAS_TEST_DATABASE", "tvmaze_test");
        if (!database.startsWith("tvmaze_test")) {
            throw new IllegalArgumentException("ATLAS_TEST_DATABASE must start with tvmaze_test");
        }
        registry.add("spring.data.mongodb.uri", () -> uri);
        registry.add("spring.data.mongodb.database", () -> database);
    }

    @AfterEach
    void removeOnlyDocumentsCreatedByThisTest() {
        mongo.remove(Query.query(Criteria.where("_id").is(showId)), ShowCacheDocument.class);
        mongo.remove(Query.query(Criteria.where("showId").is(showId)), CommentDocument.class);
    }

    @Test
    void realAtlasCacheAndCommentsFlow() throws Exception {
        when(provider.findById(showId)).thenReturn(new Show(showId,
                Map.of("id", showId, "name", "Atlas fixture", "extra", Map.of("preserved", true))));
        when(provider.search("fixture")).thenReturn(List.of(
                new ShowSummary(showId, "Atlas fixture", "Network", null, List.of("Drama"))));

        mvc.perform(get("/api/v1/show").param("show_id", Long.toString(showId)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.comments").isEmpty());
        mvc.perform(get("/api/v1/show").param("show_id", Long.toString(showId)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.extra.preserved").value(true));
        verify(provider, times(1)).findById(showId);

        mvc.perform(post("/api/v1/comments").contentType("application/json")
                        .content("{\"show_id\":" + showId + ",\"comment\":\"Integration\",\"rating\":4.5}"))
                .andExpect(status().isCreated());
        mvc.perform(get("/api/v1/show").param("show_id", Long.toString(showId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.comments[0].comment").value("Integration"))
                .andExpect(jsonPath("$.comments[0].rating").value(4.5));
        mvc.perform(get("/api/v1/search").param("search_query", "fixture"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].comments[0].rating").value(4.5));
        verify(provider, times(1)).findById(showId);

        var cached = mongo.findById(showId, ShowCacheDocument.class);
        assertThat(cached).isNotNull();
        assertThat(cached.payload()).doesNotContain("comments");
        var indexes = mongo.indexOps(CommentDocument.class).getIndexInfo();
        assertThat(indexes).anyMatch(index -> "comments_by_show".equals(index.getName()));
    }
}
