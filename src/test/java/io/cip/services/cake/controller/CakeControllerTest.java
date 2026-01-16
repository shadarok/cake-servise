package io.cip.services.cake.controller;

import io.cip.services.cake.CakeServiceSpringTest;
import io.cip.services.cake.repository.CakeRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.reactive.server.WebTestClient;

@CakeServiceSpringTest
class CakeControllerTest {

    @Autowired
    private WebTestClient httpClient;

    @Autowired
    private CakeRepository cakeRepository;

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
}
