package io.cip.services.cake.controller;

import io.cip.services.cake.CakeServiceSpringTest;
import org.assertj.core.api.Assertions;
import org.awaitility.core.ConditionTimeoutException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;

import java.time.Duration;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.github.tomakehurst.wiremock.stubbing.Scenario.STARTED;
import static org.awaitility.Awaitility.await;
import static org.springframework.http.MediaType.APPLICATION_JSON;

@CakeServiceSpringTest
class CakeControllerV2AsyncTest {

    public static final int UNKNOWN_CAKE_ID = 9999999;

    @Autowired
    private WebTestClient httpClient;

    @BeforeEach
    void resetWireMock() {
        resetAllRequests();
        resetToDefault();
    }

    @Nested
    class GetCakeById {

        @Test
        void shouldSucceedWithDelay() {
            // given
            final long cakeId = 1L;
            stubFor(get(urlEqualTo("/cakes/" + cakeId))
                    .willReturn(aResponse()
                            .withFixedDelay((int) Duration.ofMillis(500).toMillis())
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
            await()
                    .atMost(Duration.ofSeconds(5))
                    .pollInterval(Duration.ofMillis(300))
                    .untilAsserted(() -> getCakeByIdRequest(cakeId)
                            .headers(CakeControllerV2AsyncTest.this::withBasicAuth)
                            .exchange()
                            .expectStatus().isOk()
                            .expectHeader().contentType(APPLICATION_JSON)
                            .expectBody()
                            //
                            .jsonPath("$.id").isEqualTo(cakeId)
                            .jsonPath("$.title").isEqualTo("some title")
                            .jsonPath("$.description").isEqualTo("some description")
                    );
        }

        @Test
        void shouldRetryThreeTimesThenReturnNotFound() {
            final long cakeId = 1L;
            stubFor(get(urlEqualTo("/cakes/" + cakeId))
                    .willReturn(aResponse().withStatus(HttpStatus.INTERNAL_SERVER_ERROR.value())));

            getCakeByIdRequest(cakeId)
                    .headers(CakeControllerV2AsyncTest.this::withBasicAuth)
                    .exchange()
                    .expectStatus().isNotFound();

            verify(3, getRequestedFor(urlEqualTo("/cakes/" + cakeId)));
        }

        @Test
        void shouldSucceedAfterThirdRetry() {
            final long cakeId = 1L;
            stubFor(get(urlEqualTo("/cakes/" + cakeId))
                    .inScenario("Retry scenario")
                    .whenScenarioStateIs(STARTED)
                    .willReturn(aResponse().withStatus(HttpStatus.INTERNAL_SERVER_ERROR.value()))
                    .willSetStateTo("SECOND"));

            stubFor(get(urlEqualTo("/cakes/" + cakeId))
                    .inScenario("Retry scenario")
                    .whenScenarioStateIs("SECOND")
                    .willReturn(aResponse().withStatus(HttpStatus.INTERNAL_SERVER_ERROR.value()))
                    .willSetStateTo("THIRD"));

            stubFor(get(urlEqualTo("/cakes/" + cakeId))
                    .inScenario("Retry scenario")
                    .whenScenarioStateIs("THIRD")
                    .willReturn(aResponse()
                            .withStatus(HttpStatus.OK.value())
                            .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                            .withBody("""
                                        {
                                          "id": 1,
                                          "title": "retry cake",
                                          "description": "finally works"
                                        }
                                    """)));

            await()
                    .atMost(Duration.ofSeconds(5))
                    .pollInterval(Duration.ofMillis(300))
                    .untilAsserted(() -> getCakeByIdRequest(cakeId)
                            .headers(CakeControllerV2AsyncTest.this::withBasicAuth)
                            .exchange()
                            .expectStatus().isOk()
                            .expectBody()
                            .jsonPath("$.id").isEqualTo(cakeId)
                            .jsonPath("$.title").isEqualTo("retry cake")
                            .jsonPath("$.description").isEqualTo("finally works")
                    );

            verify(3, getRequestedFor(urlEqualTo("/cakes/" + cakeId)));
        }

        @Test
        void shouldTimeoutDueToLongDelay() {
            // given
            final long cakeId = 1L;
            stubFor(get(urlEqualTo("/cakes/" + cakeId))
                    .willReturn(aResponse()
                            .withFixedDelay((int) Duration.ofSeconds(3).toMillis())
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
            Assertions.assertThatThrownBy(() -> await()
                    .atMost(Duration.ofSeconds(5))
                    .pollInterval(Duration.ofMillis(300))
                    .untilAsserted(() -> getCakeByIdRequest(cakeId)
                            .headers(CakeControllerV2AsyncTest.this::withBasicAuth)
                            .exchange()
                            .expectStatus().is5xxServerError()
                    )
            ).isInstanceOf(ConditionTimeoutException.class);

            verify(3, getRequestedFor(urlEqualTo("/cakes/" + cakeId)));
        }

        @Test
        void shouldNotFoundAfterSecondRetry() {
            stubFor(get(urlEqualTo("/cakes/" + UNKNOWN_CAKE_ID))
                    .inScenario("Retry scenario")
                    .whenScenarioStateIs(STARTED)
                    .willReturn(aResponse().withStatus(HttpStatus.INTERNAL_SERVER_ERROR.value()))
                    .willSetStateTo("SECOND"));

            stubFor(get(urlEqualTo("/cakes/" + UNKNOWN_CAKE_ID))
                    .inScenario("Retry scenario")
                    .whenScenarioStateIs("SECOND")
                    .willReturn(aResponse().withStatus(HttpStatus.NOT_FOUND.value())));

            await()
                    .atMost(Duration.ofSeconds(5))
                    .pollInterval(Duration.ofMillis(300))
                    .untilAsserted(() -> getCakeByIdRequest(UNKNOWN_CAKE_ID)
                            .headers(CakeControllerV2AsyncTest.this::withBasicAuth)
                            .exchange()
                            .expectStatus().isNotFound()
                    );

            verify(lessThanOrExactly(3), getRequestedFor(urlEqualTo("/cakes/" + UNKNOWN_CAKE_ID)));
        }

        private WebTestClient.RequestHeadersSpec<?> getCakeByIdRequest(long cakeId) {
            return httpClient.get().uri("/v2/cakes/" + cakeId);
        }
    }

    private void withBasicAuth(HttpHeaders headers) {
        headers.setBasicAuth("cake-user-test", "cake-password-test");
    }
}
