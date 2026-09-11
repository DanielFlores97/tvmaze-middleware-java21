package com.example.pruebatecnica.infrastructure.http;

import com.example.pruebatecnica.domain.AppException;
import java.net.SocketTimeoutException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;

class JsonPlaceholderPostProviderTest {
    private MockRestServiceServer server;
    private JsonPlaceholderPostProvider provider;

    @BeforeEach
    void setUp() {
        var builder = RestClient.builder().baseUrl("https://example.test");
        server = MockRestServiceServer.bindTo(builder).build();
        provider = new JsonPlaceholderPostProvider(builder.build());
    }

    @Test
    void mapsExternalResponse() {
        server.expect(requestTo("https://example.test/posts/1")).andRespond(withSuccess(
                "{\"id\":1,\"userId\":2,\"title\":\"Title\",\"body\":\"Body\"}", MediaType.APPLICATION_JSON));
        var post = provider.findById(1);
        assertThat(post.id()).isEqualTo(1);
        assertThat(post.title()).isEqualTo("Title");
        server.verify();
    }

    @Test
    void mapsNotFound() {
        server.expect(anything()).andRespond(withStatus(HttpStatus.NOT_FOUND));
        assertFailure(AppException.Code.POST_NOT_FOUND);
    }

    @Test
    void mapsServerFailure() {
        server.expect(anything()).andRespond(withServerError());
        assertFailure(AppException.Code.UPSTREAM_UNAVAILABLE);
    }

    @Test
    void rejectsIncompleteResponse() {
        server.expect(anything()).andRespond(withSuccess("{}", MediaType.APPLICATION_JSON));
        assertFailure(AppException.Code.UPSTREAM_UNAVAILABLE);
    }

    @Test
    void rejectsMismatchedId() {
        server.expect(anything()).andRespond(withSuccess(
                "{\"id\":9,\"userId\":2,\"title\":\"Title\",\"body\":\"Body\"}", MediaType.APPLICATION_JSON));
        assertFailure(AppException.Code.UPSTREAM_UNAVAILABLE);
    }

    @Test
    void mapsMalformedJson() {
        server.expect(anything()).andRespond(withSuccess("{", MediaType.APPLICATION_JSON));
        assertFailure(AppException.Code.UPSTREAM_UNAVAILABLE);
    }

    @Test
    void mapsTimeout() {
        server.expect(anything()).andRespond(withException(new SocketTimeoutException("timeout")));
        assertFailure(AppException.Code.UPSTREAM_TIMEOUT);
    }

    private void assertFailure(AppException.Code code) {
        assertThatThrownBy(() -> provider.findById(1)).isInstanceOfSatisfying(AppException.class,
                exception -> assertThat(exception.code()).isEqualTo(code));
        server.verify();
    }
}
