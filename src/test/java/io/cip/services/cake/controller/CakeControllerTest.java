package io.cip.services.cake.controller;

import io.cip.services.cake.CakeServiceSpringTest;
import io.cip.services.cake.repository.CakeRepository;
import io.cip.services.cake.repository.model.Cake;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;

import static java.lang.Long.parseLong;
import static org.assertj.core.api.Assertions.assertThat;


/*
 * Implement integration tests for cake API.
 * Feel free in testing scenarios :)
 *
 * When you run the application locally you can find:
 * - API specification in Swagger-UI (take a look at README.md)
 * - Cake API is secured by Basic Auth (details in application.properties, for tests in application-test.properties)
 * - H2 database - http://localhost:8081/h2-console/ (application.properties contains login details)
 *
 * Feel free to ask in case of troubles :)
 */

@CakeServiceSpringTest
class CakeControllerTest {

    public static final String REQUEST_BODY = """
            {
              "title": "Protein cheesecake",
              "description": "Best breakfast"
            }
            """;

    @Autowired
    private WebTestClient webTestClient;

    @Autowired
    private CakeRepository cakeRepository;

    @Value("${cakes.authentication.username}")
    private String userName;
    @Value("${cakes.authentication.password}")
    private String password;

    @BeforeEach
    void cleanup() {
        cakeRepository.deleteAll();
    }

    @Nested
    @DisplayName("Authorization tests")
    class authorizationTests {

        @Test
        void shouldReturnUnauthorizedWhenUnauthorized() {
            webTestClient
                    .get()
                    .uri("/cakes")
                    .exchange()
                    .expectStatus().isUnauthorized()
                    .expectBody().isEmpty();
        }
    }

    @Nested
    @DisplayName("GET all cakes")
    class getRequestForAllCakes {

        @Test
        void shouldReturnAllCakes() {

            var cakeId = cakeRepository.save(Cake.builder()
                            .title("WZ")
                            .description("Mniam")
                            .build())
                    .getId();

            webTestClient
                    .get()
                    .uri("/cakes")
                    .headers(headers -> headers.setBasicAuth(userName, password))
                    .exchange()
                    .expectStatus().isOk()
                    .expectHeader().contentType("application/json")
                    .expectBody()
                    .jsonPath("$['cakes']").isArray()
                    .jsonPath("$.cakes[0].id").isEqualTo(cakeId)
                    .jsonPath("$.cakes[0].title").isEqualTo("WZ")
                    .jsonPath("$.cakes[0].description").isEqualTo("Mniam");
        }

        @Test
        void shouldReturnNotFoundWhenCakeIsNotFound() {
            webTestClient
                    .get()
                    .uri("/cakes/{id}", 999)
                    .headers(headers -> headers.setBasicAuth(userName, password))
                    .exchange()
                    .expectStatus().isNotFound()
                    .expectBody().isEmpty();
        }
    }

    @Nested
    @DisplayName("GET cake by id")
    class getRequestByCakeId {

        @Test
        void shouldReturnCakeById() {

            var cakeId = cakeRepository.save(Cake.builder()
                            .title("WZ")
                            .description("Mniam")
                            .build())
                    .getId();

            webTestClient
                    .get()
                    .uri("/cakes/{id}", cakeId)
                    .headers(headers -> headers.setBasicAuth(userName, password))
                    .exchange()
                    .expectStatus().isOk()
                    .expectBody()
                    .jsonPath("$['id']").isEqualTo(cakeId)
                    .jsonPath("$['title']").isEqualTo("WZ")
                    .jsonPath("$.description").isEqualTo("Mniam");
        }
    }

    @Nested
    @DisplayName("POST a cake")
    class postRequest {

        @Test
        void createNewCakeRequest() {

            webTestClient
                    .post()
                    .uri("/cakes")
                    .headers(headers -> {
                        headers.setBasicAuth(userName, password);
                        headers.setContentType(MediaType.APPLICATION_JSON);
                    })
                    .bodyValue(CakeControllerTest.REQUEST_BODY)
                    .exchange()
                    .expectStatus().isCreated()
                    .expectHeader().contentType(MediaType.APPLICATION_JSON)
                    .expectBody()
                    .jsonPath("$.title").isEqualTo("Protein cheesecake")
                    .jsonPath("$.description").isEqualTo("Best breakfast")
                    .jsonPath("$.id").value(id -> {
                        Cake newCake = cakeRepository.findById(parseLong(id.toString())).get();
                        assertThat(newCake)
                                .isEqualTo(new Cake(parseLong(id.toString()), "Protein cheesecake", "Best breakfast"));
                    });
        }
    }

    @Nested
    @DisplayName("PUT a cake by id")
    class putRequestByCakeId {

        @Test
        void shouldBeAbleToUpdateCake() {

            String requestBodyEdited = """
                    {
                      "title": "Edited cake",
                      "description": "Edited description"
                    }
                    """;

            var cakeId = cakeRepository.save(Cake.builder()
                            .title("WZ")
                            .description("Mniam")
                            .build())
                    .getId();

            webTestClient
                    .put()
                    .uri("/cakes/{id}", cakeId)
                    .headers(headers -> {
                        headers.setBasicAuth(userName, password);
                        headers.setContentType(MediaType.APPLICATION_JSON);
                    })
                    .bodyValue(requestBodyEdited)
                    .exchange()
                    .expectStatus().is2xxSuccessful();

            Cake savedCake = cakeRepository.findById(cakeId).get();
            assertThat(savedCake).isEqualTo(new Cake(cakeId, "Edited cake", "Edited description"));

        }

        @Test
        void shouldReturnNotFoundWhenTryingToUpdateNonExistingCake() {

            String requestBodyEdited = """
                    {
                      "title": "Protein cheesecake EDIT",
                      "description": "Best breakfast EDIT"
                    }
                    """;

            webTestClient
                    .put()
                    .uri("/cakes/999")
                    .headers(headers -> {
                        headers.setBasicAuth(userName, password);
                        headers.setContentType(MediaType.APPLICATION_JSON);
                    })
                    .bodyValue(requestBodyEdited)
                    .exchange()
                    .expectStatus().isNotFound();
        }
    }

    @Nested
    @DisplayName("DELETE a cake by id")
    class deleteRequestByCakeId {

        @Test
        void shouldBeAbleToDeleteCake() {

            var cakeId = cakeRepository.save(Cake.builder()
                            .title("WZ")
                            .description("Mniam")
                            .build())
                    .getId();

            webTestClient
                    .delete()
                    .uri("/cakes/{id}", cakeId)
                    .headers(headers -> {
                        headers.setBasicAuth(userName, password);
                        headers.setContentType(MediaType.APPLICATION_JSON);
                    })
                    .exchange()
                    .expectStatus().isNoContent()
                    .expectBody().isEmpty();

            assertThat(cakeRepository.findById(cakeId)).isNotPresent();

        }

        @Test
        void shouldReturnErrorWhenTryingToDeleteNonExistingCake() {
            webTestClient
                    .delete()
                    .uri("/cakes/999")
                    .headers(headers -> {
                        headers.setBasicAuth(userName, password);
                        headers.setContentType(MediaType.APPLICATION_JSON);
                    })
                    .exchange()
                    .expectStatus().is4xxClientError();
        }
    }
}
