package com.example.pruebatecnica.application;

import com.example.pruebatecnica.application.port.CommentReader;
import com.example.pruebatecnica.application.port.ShowSearchProvider;
import com.example.pruebatecnica.domain.AppException;
import com.example.pruebatecnica.domain.Comment;
import com.example.pruebatecnica.domain.ShowSummary;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class SearchShowsServiceTest {
    private final ShowSearchProvider provider = mock(ShowSearchProvider.class);
    private final CommentReader comments = mock(CommentReader.class);
    private final SearchShowsService service = new SearchShowsService(provider, comments);

    @Test
    void enrichesInOneBatchAndPreservesRelevanceOrder() {
        var first = new ShowSummary(2, "First", null, null, List.of());
        var second = new ShowSummary(1, "Second", "HBO", "Summary", List.of("Drama"));
        var comment = new Comment("Good", BigDecimal.valueOf(5));
        when(provider.search("query")).thenReturn(List.of(first, second));
        when(comments.findByShowIds(List.of(2L, 1L))).thenReturn(Map.of(1L, List.of(comment)));
        var results = service.search(" query ");
        assertThat(results).extracting("id").containsExactly(2L, 1L);
        assertThat(results.getFirst().comments()).isEmpty();
        assertThat(results.getLast().comments()).containsExactly(comment);
        verify(comments, times(1)).findByShowIds(List.of(2L, 1L));
        verifyNoMoreInteractions(comments);
    }

    @Test
    void emptyResultsDoNotQueryMongo() {
        when(provider.search("missing")).thenReturn(List.of());
        assertThat(service.search("missing")).isEmpty();
        verifyNoInteractions(comments);
    }

    @Test
    void invalidSearchDoesNotQueryDependencies() {
        assertThatThrownBy(() -> service.search(" ")).isInstanceOf(AppException.class);
        assertThatThrownBy(() -> service.search(null)).isInstanceOf(AppException.class);
        assertThatThrownBy(() -> service.search("a".repeat(201))).isInstanceOf(AppException.class);
        verifyNoInteractions(provider, comments);
    }

    @Test
    void databaseFailureDoesNotPretendThereAreNoComments() {
        when(provider.search("query")).thenReturn(List.of(new ShowSummary(1, "Show", null, null, List.of())));
        when(comments.findByShowIds(any())).thenThrow(new AppException(AppException.Code.DATABASE_UNAVAILABLE));
        assertThatThrownBy(() -> service.search("query")).isInstanceOf(AppException.class);
    }
}
