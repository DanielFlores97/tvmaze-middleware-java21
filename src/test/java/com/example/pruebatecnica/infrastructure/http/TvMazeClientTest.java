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

class TvMazeClientTest {
    private MockRestServiceServer server;
    private TvMazeClient client;

    @BeforeEach
    void setUp() {
        var builder = RestClient.builder().baseUrl("https://api.tvmaze.com");
        server = MockRestServiceServer.bindTo(builder).build();
        client = new TvMazeClient(builder.build(), new com.fasterxml.jackson.databind.ObjectMapper());
    }

    @Test
    void searchFlattensWrapperAndPrefersNetwork() {
        server.expect(requestTo("https://api.tvmaze.com/search/shows?q=girls")).andRespond(withSuccess("""
                [{"score":1.5,"show":{"id":1,"name":"Girls","network":{"name":"HBO"},
                "webChannel":{"name":"Other"},"summary":"<p>Story</p>","genres":["Drama"]}}]
                """, MediaType.APPLICATION_JSON));
        var show = client.search("girls").getFirst();
        assertThat(show.id()).isEqualTo(1);
        assertThat(show.channel()).isEqualTo("HBO");
        assertThat(show.summary()).isEqualTo("<p>Story</p>");
        assertThat(show.genres()).containsExactly("Drama");
        server.verify();
    }

    @Test
    void searchFallsBackToWebChannelAndAllowsNullSummary() {
        server.expect(anything()).andRespond(withSuccess("""
                [{"show":{"id":2,"name":"Show","network":null,"webChannel":{"name":"Netflix"},
                "summary":null,"genres":[]}}]
                """, MediaType.APPLICATION_JSON));
        var show = client.search("show").getFirst();
        assertThat(show.channel()).isEqualTo("Netflix");
        assertThat(show.summary()).isNull();
    }

    @Test
    void queryIsEncodedWithoutParameterInjection() {
        server.expect(requestTo("https://api.tvmaze.com/search/shows?q=A%26B%20%2B%20%7Btest%7D"))
                .andRespond(withSuccess("[]", MediaType.APPLICATION_JSON));
        assertThat(client.search("A&B + {test}")).isEmpty();
        server.verify();
    }

    @Test
    void rejectsMalformedResponse() {
        server.expect(anything()).andRespond(withSuccess("{}", MediaType.APPLICATION_JSON));
        assertCode(AppException.Code.UPSTREAM_UNAVAILABLE);
    }

    @Test
    void mapsRateLimit() {
        server.expect(anything()).andRespond(withStatus(HttpStatus.TOO_MANY_REQUESTS));
        assertCode(AppException.Code.UPSTREAM_RATE_LIMITED);
    }

    @Test
    void mapsTimeout() {
        server.expect(anything()).andRespond(withException(new SocketTimeoutException("timeout")));
        assertCode(AppException.Code.UPSTREAM_TIMEOUT);
    }

    @Test
    void mapsServerError() {
        server.expect(anything()).andRespond(withServerError());
        assertCode(AppException.Code.UPSTREAM_UNAVAILABLE);
    }

    private void assertCode(AppException.Code code) {
        assertThatThrownBy(() -> client.search("show")).isInstanceOfSatisfying(AppException.class,
                exception -> assertThat(exception.code()).isEqualTo(code));
    }

    @Test
    void detailPreservesUnknownFieldsAndNulls() {
        server.expect(requestTo("https://api.tvmaze.com/shows/1")).andRespond(withSuccess("""
                {"id":1,"name":"Show","summary":null,"future":{"array":[1,true,null]},
                "_links":{"self":{"href":"https://api.tvmaze.com/shows/1"}}}
                """, MediaType.APPLICATION_JSON));
        var show = client.findById(1);
        assertThat(show.attributes()).containsEntry("summary", null).containsKeys("future", "_links");
        server.verify();
    }

    @Test
    void detailMapsNotFound() {
        server.expect(anything()).andRespond(withStatus(HttpStatus.NOT_FOUND));
        assertThatThrownBy(() -> client.findById(1)).isInstanceOfSatisfying(AppException.class,
                exception -> assertThat(exception.code()).isEqualTo(AppException.Code.SHOW_NOT_FOUND));
    }

    @Test
    void detailRejectsMismatchedId() {
        server.expect(anything()).andRespond(withSuccess("{\"id\":2,\"name\":\"Show\"}", MediaType.APPLICATION_JSON));
        assertThatThrownBy(() -> client.findById(1)).isInstanceOf(AppException.class);
    }
}
