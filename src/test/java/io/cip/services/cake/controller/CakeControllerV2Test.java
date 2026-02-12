package io.cip.services.cake.controller;

import io.cip.services.cake.CakeServiceSpringTest;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.contract.wiremock.AutoConfigureWireMock;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.reactive.server.WebTestClient;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.springframework.http.MediaType.APPLICATION_JSON;

@CakeServiceSpringTest
@AutoConfigureWireMock(port = 0)
@TestPropertySource(properties = {
        "cakes.external-provider.url=http://localhost:${wiremock.server.port}/cakes"
})
class CakeControllerV2Test {

    public static final int UNKNOWN_CAKE_ID = 9999999;

    @Autowired
    private WebTestClient httpClient;

    @Nested
    class GetCakeById {

        @Test
        void shouldSucceed() {
            // given
            long cakeId = 1L;
            stubFor(get(urlEqualTo("/cakes/" + cakeId))
                    .willReturn(aResponse()
                            .withStatus(HttpStatus.OK.value())
                            .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                            .withBody("""
                                    {
                                      "id": 1,
                                      "title": "some title",
                                      "description": "some description"
                                    }
                                    """)));

            // when-then
            getCakeByIdRequest(cakeId)
                    .headers(CakeControllerV2Test.this::withBasicAuth)
                    .exchange()
                    .expectStatus().isOk()
                    .expectHeader().contentType(APPLICATION_JSON)
                    .expectBody()
                    //
                    .jsonPath("$.id").isEqualTo(cakeId)
                    .jsonPath("$.title").isEqualTo("some title")
                    .jsonPath("$.description").isEqualTo("some description");
        }

        @Test
        void shouldReturnNotFoundWhenCakeIsNotFound() {
            stubFor(get(urlEqualTo("/cakes/999"))
                    .willReturn(aResponse().withStatus(HttpStatus.NOT_FOUND.value())));

            getCakeByIdRequest(UNKNOWN_CAKE_ID)
                    .headers(CakeControllerV2Test.this::withBasicAuth)
                    .exchange()
                    .expectStatus().isNotFound()
                    .expectBody().isEmpty();
        }

        @Test
        void returnUnauthorizedWhenAuthorizationHeaderIsMissing() {

            getCakeByIdRequest(UNKNOWN_CAKE_ID)
                    .exchange()
                    .expectStatus().isUnauthorized()
                    .expectBody().isEmpty();
        }

        @Test
        void returnUnauthorizedWhenUsernameAndPasswordAreIncorrect() {

            getCakeByIdRequest(UNKNOWN_CAKE_ID)
                    .exchange()
                    .expectStatus().isUnauthorized()
                    .expectBody().isEmpty();
        }

        private WebTestClient.RequestHeadersSpec<?> getCakeByIdRequest(long cakeId) {
            return httpClient.get().uri("/v2/cakes/" + cakeId);
        }
    }

    private void withBasicAuth(HttpHeaders headers) {
        headers.setBasicAuth("cake-user-test", "cake-password-test");
    }
}
