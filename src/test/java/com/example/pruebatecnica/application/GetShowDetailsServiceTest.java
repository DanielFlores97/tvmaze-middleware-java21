package com.example.pruebatecnica.application;

import com.example.pruebatecnica.application.port.CommentReader;
import com.example.pruebatecnica.domain.Comment;
import com.example.pruebatecnica.domain.Show;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class GetShowDetailsServiceTest {
    private final GetShowService shows = mock(GetShowService.class);
    private final CommentReader comments = mock(CommentReader.class);
    private final GetShowDetailsService service = new GetShowDetailsService(shows, comments);

    @Test
    void reflectsNewCommentsWithoutMutatingCachedShow() {
        var cached = new Show(1, Map.of("id", 1, "name", "Show", "unknown", List.of("kept")));
        var comment = new Comment("New", BigDecimal.valueOf(5));
        when(shows.findById(1)).thenReturn(cached);
        when(comments.findByShowIds(List.of(1L))).thenReturn(Map.of(), Map.of(1L, List.of(comment)));
        assertThat(service.findById(1)).containsEntry("comments", List.of()).containsEntry("unknown", List.of("kept"));
        assertThat(service.findById(1)).containsEntry("comments", List.of(comment));
        assertThat(cached.attributes()).doesNotContainKey("comments");
        verify(comments, times(2)).findByShowIds(List.of(1L));
    }

    @Test
    void missingShowDoesNotQueryComments() {
        when(shows.findById(1)).thenThrow(new com.example.pruebatecnica.domain.AppException(
                com.example.pruebatecnica.domain.AppException.Code.SHOW_NOT_FOUND));
        assertThatThrownBy(() -> service.findById(1))
                .isInstanceOf(com.example.pruebatecnica.domain.AppException.class);
        verifyNoInteractions(comments);
    }
}
