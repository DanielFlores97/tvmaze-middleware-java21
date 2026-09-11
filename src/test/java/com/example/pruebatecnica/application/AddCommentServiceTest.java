package com.example.pruebatecnica.application;

import com.example.pruebatecnica.application.port.CommentWriter;
import com.example.pruebatecnica.domain.AppException;
import com.example.pruebatecnica.domain.Comment;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class AddCommentServiceTest {
    private final GetShowService shows = mock(GetShowService.class);
    private final CommentWriter comments = mock(CommentWriter.class);
    private final AddCommentService service = new AddCommentService(shows, comments);

    @Test
    void verifiesShowBeforeInserting() {
        service.add(1, " Good show ", new BigDecimal("4.5"));
        var order = inOrder(shows, comments);
        order.verify(shows).findById(1);
        order.verify(comments).save(1, new Comment("Good show", new BigDecimal("4.5")));
    }

    @Test
    void missingShowCannotReceiveComment() {
        when(shows.findById(1)).thenThrow(new AppException(AppException.Code.SHOW_NOT_FOUND));
        assertThatThrownBy(() -> service.add(1, "Good", BigDecimal.ONE)).isInstanceOf(AppException.class);
        verifyNoInteractions(comments);
    }

    @Test
    void invalidCommentDoesNotCallDependencies() {
        assertThatThrownBy(() -> service.add(1, "  ", BigDecimal.ONE)).isInstanceOf(AppException.class);
        assertThatThrownBy(() -> service.add(1, "Good", new BigDecimal("5.01"))).isInstanceOf(AppException.class);
        assertThatThrownBy(() -> service.add(1, "Good", new BigDecimal("-0.01"))).isInstanceOf(AppException.class);
        assertThatThrownBy(() -> service.add(1, "Good", null)).isInstanceOf(AppException.class);
        verifyNoInteractions(comments, shows);
    }
}
