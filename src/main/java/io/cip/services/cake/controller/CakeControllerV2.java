package io.cip.services.cake.controller;

import io.cip.services.cake.exception.CakeNotFoundException;
import io.cip.services.cake.model.cake.CakeResponse;
import io.cip.services.cake.service.ExternalCakeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import static org.springframework.http.HttpStatus.*;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

@RequiredArgsConstructor
@Slf4j
@RestController
@RequestMapping("/v2/cakes")
public class CakeControllerV2 {

    private final ExternalCakeService cakeService;

    @GetMapping(path = "/{id}", produces = APPLICATION_JSON_VALUE)
    public Mono<CakeResponse> getCakeById(@PathVariable long id) {
        log.info("getting cake with id {}", id);

        return cakeService.getCakeById(id);
    }

    @ExceptionHandler(CakeNotFoundException.class)
    @ResponseStatus(value = NOT_FOUND)
    private void cakeNotFoundException() {
    }
}
