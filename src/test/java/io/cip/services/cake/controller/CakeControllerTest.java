package io.cip.services.cake.controller;

import io.cip.services.cake.CakeServiceSpringTest;
import io.cip.services.cake.repository.CakeRepository;
import net.minidev.json.JSONArray;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;


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

//@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT) //<-
//@AutoConfigureWebTestClient
//@ActiveProfiles("test")

class CakeServiceIntegrationTest {

    public static final String REQUEST_BODY = """
            {
              "title": "Protein cheesecake",
              "description": "Best breakfast"
            }
            """;

    @Autowired
    WebTestClient webTestClient;

    @Value("${cakes.authentication.username}")
    private String userName;
    @Value("${cakes.authentication.password}")
    private String password;

//    private static final String USERNAME = "cake-user-test";
//    private static final String PASSWORD = "cake-password-test";

    @Autowired
    private CakeRepository cakeRepository;

    @Test
    void shouldReturnAllCakes() {
        webTestClient
                .get()
                .uri("/cakes")
                .headers(headers -> headers.setBasicAuth(userName, password))
                .exchange()
                .expectStatus().isOk()
                .expectHeader().contentType("application/json")
                .expectBody()
                .jsonPath("$['cakes']").isArray();
    }

    @Test
    void createNewCakeRequest() {

        webTestClient
                .post()
                .uri("/cakes")
                .headers(headers -> {
                    headers.setBasicAuth(userName, password);
                    headers.setContentType(MediaType.APPLICATION_JSON);
                })
                .bodyValue(CakeServiceIntegrationTest.REQUEST_BODY)
                .exchange()
                .expectStatus().isCreated()
                .expectHeader().contentType(MediaType.APPLICATION_JSON)
                .expectBody()
                .jsonPath("$.title").isEqualTo("Protein cheesecake")
                .jsonPath("$.description").isEqualTo("Best breakfast")
                .jsonPath("$.id").isEqualTo(5);
    }

    @Test
    void shouldReturnCakeById() {
        webTestClient
                .get()
                .uri("/cakes/{id}", 4)
                .headers(headers -> headers.setBasicAuth(userName, password))
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$['id']").isEqualTo(4)
                .jsonPath("$['title']").isEqualTo("cake with extra field")
                .jsonPath("$.description").isEqualTo("some desc 2");
    }

    @Test
    void shouldBeAbleToDeleteCake() {

        createNewCakeRequest();

        webTestClient
                .delete()
                .uri("/cakes/5")
                .headers(headers -> {
                    headers.setBasicAuth(userName, password);
                    headers.setContentType(MediaType.APPLICATION_JSON);
                })
                .exchange()
                .expectStatus().isNoContent()
                .expectBody().isEmpty();

        webTestClient
                .get()
                .uri("/cakes/{id}", 5)
                .headers(headers -> headers.setBasicAuth(userName, password))
                .exchange()
                .expectStatus().is4xxClientError();
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

    // how to assert that the cake is actually gone-gone?

    @Test
    void shouldBeAbleToUpdateCake() {

        String requestBodyEdited = """
                {
                  "title": "Protein cheesecake EDIT",
                  "description": "Best breakfast EDIT"
                }
                """;

        createNewCakeRequest();

        webTestClient
                .put()
                .uri("/cakes/5")
                .headers(headers -> {
                    headers.setBasicAuth(userName, password);
                    headers.setContentType(MediaType.APPLICATION_JSON);
                })
                .bodyValue(requestBodyEdited)
                .exchange()
                .expectStatus().is2xxSuccessful();

        webTestClient
                .get()
                .uri("/cakes/{id}", 5)
                .headers(headers -> headers.setBasicAuth(userName, password))
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$['id']").isNotEmpty()
                .jsonPath("$['title']").isEqualTo("Protein cheesecake EDIT")
                .jsonPath("$.description").isEqualTo("Best breakfast EDIT");
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
                .expectStatus().is4xxClientError();
    }


    @Test
    void shouldReturnNotFoundWhenCakeIsNotFound() {
        webTestClient
                .get()
                .uri("/cakes/{id}", 999)
                .exchange()
                .expectStatus().is4xxClientError()
                .expectBody().isEmpty();
    }

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
