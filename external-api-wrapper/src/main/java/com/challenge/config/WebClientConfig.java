package com.challenge.config;

import com.challenge.exceptions.ExternalApiException;
import com.challenge.exceptions.InvalidInputException;
import com.challenge.exceptions.NotFoundException;
import com.challenge.exceptions.TooManyRequestsException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@Configuration
public class WebClientConfig {

    @Bean
    public WebClient webClient(WebClient.Builder builder) {
        return builder
                .baseUrl("https://api.discogs.com")
                .filter(errorHandlingFilter())
                .build();
    }

    private ExchangeFilterFunction errorHandlingFilter() {
        return ExchangeFilterFunction.ofResponseProcessor(clientResponse -> {
            if (clientResponse.statusCode().isError()) {
                return clientResponse.bodyToMono(String.class)
                        .flatMap(errorBody -> {
                            HttpStatusCode statusCode = clientResponse.statusCode();
                            HttpStatus status = HttpStatus.resolve(statusCode.value());
                            if (status == null) {
                                return Mono.error(new ExternalApiException("Unknown error: " + errorBody));
                            }
                            switch (status) {
                                case NOT_FOUND:
                                    return Mono.error(new NotFoundException("Resource not found: " + extractMessage(errorBody)));
                                case TOO_MANY_REQUESTS:
                                    return Mono.error(new TooManyRequestsException("Rate limit exceeded: " + extractMessage(errorBody)));
                                case UNPROCESSABLE_ENTITY:
                                    return Mono.error(new InvalidInputException("Invalid input: " + extractMessage(errorBody)));
                                default:
                                    return Mono.error(new ExternalApiException("Unexpected error: " + extractMessage(errorBody)));
                            }
                        });
            }
            return Mono.just(clientResponse);
        });
    }


    private String extractMessage(String errorBody) {
        try {
            JsonNode root = new ObjectMapper().readTree(errorBody);
            return root.path("message").asText();
        } catch (JsonProcessingException e) {
            return errorBody;
        }
    }
}