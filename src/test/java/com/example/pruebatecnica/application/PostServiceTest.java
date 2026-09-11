package com.example.pruebatecnica.application;

import com.example.pruebatecnica.application.port.PostCache;
import com.example.pruebatecnica.application.port.PostProvider;
import com.example.pruebatecnica.domain.AppException;
import com.example.pruebatecnica.domain.Post;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class PostServiceTest {
    private final PostProvider provider = mock(PostProvider.class);
    private final PostCache cache = mock(PostCache.class);
    private final PostService service = new PostService(provider, cache);
    private final Post post = new Post(1, 2, "Title", "Body");

    @Test
    void returnsCachedPostWithoutCallingProvider() {
        when(cache.findById(1)).thenReturn(Optional.of(post));
        assertThat(service.findById(1)).isEqualTo(post);
        verifyNoInteractions(provider);
        verify(cache, never()).put(any());
    }

    @Test
    void fetchesAndCachesOnMiss() {
        when(cache.findById(1)).thenReturn(Optional.empty());
        when(provider.findById(1)).thenReturn(post);
        assertThat(service.findById(1)).isEqualTo(post);
        verify(cache).put(post);
    }

    @Test
    void neverCachesProviderFailure() {
        when(cache.findById(1)).thenReturn(Optional.empty());
        when(provider.findById(1)).thenThrow(new AppException(AppException.Code.POST_NOT_FOUND));
        assertThatThrownBy(() -> service.findById(1)).isInstanceOf(AppException.class);
        verify(cache, never()).put(any());
    }

    @Test
    void rejectsInvalidIdsBeforeAccessingDependencies() {
        assertThatThrownBy(() -> service.findById(0)).isInstanceOf(AppException.class);
        assertThatThrownBy(() -> service.evict(-1)).isInstanceOf(AppException.class);
        verifyNoInteractions(cache, provider);
    }

    @Test
    void invalidatesOnlyRequestedEntry() {
        service.evict(1);
        verify(cache).evict(1);
        verifyNoInteractions(provider);
    }
}
