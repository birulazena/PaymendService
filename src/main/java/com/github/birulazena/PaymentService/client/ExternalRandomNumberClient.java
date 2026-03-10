package com.github.birulazena.PaymentService.client;

import com.github.birulazena.PaymentService.client.dto.RandomNumberDto;
import com.github.birulazena.PaymentService.exception.InvalidExternalResponseException;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

@Service
public class ExternalRandomNumberClient {

    private final WebClient webClient;

    public ExternalRandomNumberClient(WebClient.Builder builder,
                                      @Value("${external.random-number.url}") String url) {
        this.webClient = builder.baseUrl(url).build();
    }

    @CircuitBreaker(name = "random-number", fallbackMethod = "getRandomNumberFallback")
    public RandomNumberDto getRandomNumber() {
        Integer[] response = webClient.get()
                .uri(uri -> uri
                        .path("/csrng/csrng.php")
                        .queryParam("min", 1)
                        .queryParam("max", 100)
                        .queryParam("count", 1)
                        .build())
                .retrieve()
                .bodyToMono(Integer[].class)
                .block();

        if(response != null && response.length != 0)
            return new RandomNumberDto(response[0]);

        throw new InvalidExternalResponseException("Received response is empty or null");
    }

    public RandomNumberDto getRandomNumberFallback(Throwable throwable) {
        return new RandomNumberDto(null);
    }

}
