package io.cip.services.cake.service;

import io.cip.services.cake.exception.CakeNotFoundException;
import io.cip.services.cake.model.cake.CakeResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

import java.time.Duration;

@Service
@RequiredArgsConstructor
@Slf4j
public class ExternalCakeService {

    private final WebClient client;

    @Value("${cakes.external-provider.url}")
    private String externalProviderUrl;

    @Value("${cakes.web-client.timeout-in-seconds}")
    private Integer timeoutInSeconds;

    @Value("${cakes.web-client.retry.max-attempts}")
    private Integer maxRetries;

    @Value("${cakes.web-client.retry.min-backoff-in-millis}")
    private Integer minBackoffInMillis;

    public Mono<CakeResponse> getCakeById(long id) {
        return client
                .get()
                .uri(externalProviderUrl + "/{id}", id)
                .retrieve()
                .bodyToMono(CakeResponse.class)
                .timeout(Duration.ofSeconds(timeoutInSeconds))
                .retryWhen(Retry.backoff(maxRetries, Duration.ofMillis(minBackoffInMillis)))
                .onErrorMap(ex -> new CakeNotFoundException());
    }
}
