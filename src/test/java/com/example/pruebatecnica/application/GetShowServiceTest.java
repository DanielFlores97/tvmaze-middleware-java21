package com.example.pruebatecnica.application;

import com.example.pruebatecnica.application.port.ShowCache;
import com.example.pruebatecnica.application.port.ShowProvider;
import com.example.pruebatecnica.domain.AppException;
import com.example.pruebatecnica.domain.Show;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class GetShowServiceTest {
    private final ShowProvider provider = mock(ShowProvider.class);
    private final ShowCache cache = mock(ShowCache.class);
    private final GetShowService service = new GetShowService(provider, cache);
    private final Show show = new Show(1, Map.of("id", 1, "name", "Show"));

    @Test
    void hitNeverCallsTvMaze() {
        when(cache.findById(1)).thenReturn(Optional.of(show));
        assertThat(service.findById(1)).isEqualTo(show);
        verifyNoInteractions(provider);
        verify(cache, never()).save(any());
    }

    @Test
    void missSavesBeforeReturning() {
        when(cache.findById(1)).thenReturn(Optional.empty());
        when(provider.findById(1)).thenReturn(show);
        assertThat(service.findById(1)).isEqualTo(show);
        var order = inOrder(cache, provider);
        order.verify(cache).findById(1);
        order.verify(provider).findById(1);
        order.verify(cache).save(show);
    }

    @Test
    void failedWriteDoesNotReturnSuccess() {
        when(provider.findById(1)).thenReturn(show);
        doThrow(new AppException(AppException.Code.DATABASE_UNAVAILABLE)).when(cache).save(show);
        assertThatThrownBy(() -> service.findById(1)).isInstanceOf(AppException.class);
    }

    @Test
    void providerErrorsAreNotCached() {
        when(provider.findById(1)).thenThrow(new AppException(AppException.Code.SHOW_NOT_FOUND));
        assertThatThrownBy(() -> service.findById(1)).isInstanceOf(AppException.class);
        verify(cache, never()).save(any());
    }

    @Test
    void invalidIdDoesNotReachDependencies() {
        assertThatThrownBy(() -> service.findById(0)).isInstanceOf(AppException.class);
        verifyNoInteractions(cache, provider);
    }
}
