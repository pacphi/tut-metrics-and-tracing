package client;

import java.time.Duration;
import java.util.Arrays;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Slf4j
@SpringBootApplication
public class ClientApplication {

    private static final List<String> CONSOLES = List.of("ps4", "ps5", "xbox", "switch");
    public static void main(String[] args) {
        log.info("starting client");
        SpringApplication.run(ClientApplication.class, args);
    }

    @Bean
    public WebClient.Builder loadBalancedWebClientBuilder() {
        return WebClient.builder();
    }

    @Bean
    ApplicationListener<ApplicationReadyEvent> ready(AvailabilityClient client) {
        return applicationReadyEvent -> {
            checkConsoleAvailability(client);
        };
    }

    @Scheduled(fixedRate = 10000)
    protected void checkConsoleAvailability(AvailabilityClient client) {
        for (var console : CONSOLES) {
            Flux
                .range(0, 20)
                .delayElements(Duration.ofMillis(100))
                .subscribe(i -> client
                                    .checkAvailability(console)
                                    .subscribe(availability ->
                                        log.info("console: {}, availability: {} ", console, availability.isAvailable())
                                    )
                );
        }
    }
}

@Data
@AllArgsConstructor
@NoArgsConstructor
class Availability {
    private boolean available;
    private String console;
}

@Component
@RequiredArgsConstructor
class AvailabilityClient {

    private final WebClient.Builder webClientBuilder;
    private @Value("${console.server.port:8083}") Integer port;
    private @Value("${console.server.hostname:localhost}") String hostname;

    Mono<Availability> checkAvailability(String console) {
        return webClientBuilder
                .build()
                    .get()
                    .uri(String.format("http://%s:%d/availability/{console}", hostname, port), console)
                    .retrieve()
                    .bodyToMono(Availability.class)
                    .onErrorReturn(new Availability(false, console));
    }

}
