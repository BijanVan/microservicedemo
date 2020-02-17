package com.example.carservice;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Mono;

import java.time.LocalDate;
import java.time.Month;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.fail;
import static org.springframework.security.test.web.reactive.server.SecurityMockServerConfigurers.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class CarServiceApplicationTests {
    @Autowired
    ApplicationContext context;

    private WebTestClient client;

    @Autowired
    private CarRepository repository;

    @Test
    void contextLoads() {
    }

    @Test
    void addCar() {
        Car buggy = new Car(UUID.randomUUID(), "ID. BUGGY", LocalDate.of(2022, Month.DECEMBER, 1));

        client.mutateWith(mockJwt())
            .mutateWith(csrf())
            .post()
            .uri("/cars")
            .accept(MediaType.APPLICATION_JSON)
            .body(Mono.just(buggy), Car.class)
            .exchange()
            .expectStatus()
            .isCreated()
            .expectHeader()
            .contentType(MediaType.APPLICATION_JSON)
            .expectBody()
            .jsonPath("$.id")
            .isNotEmpty()
            .jsonPath("$.name")
            .isEqualTo("ID. BUGGY");
    }

    @Test
    void getCars() {
        client.mutateWith(mockJwt())
            .mutateWith(csrf())
            .get()
            .uri("/cars")
            .accept(MediaType.APPLICATION_JSON)
            .exchange()
            .expectStatus()
            .isOk()
            .expectHeader()
            .contentType(MediaType.APPLICATION_JSON)
            .expectBodyList(Car.class);
    }

    @Test
    void deleteCar() {
        Car buggy = new Car(UUID.randomUUID(), "ID. BUGGY", LocalDate.of(2022, Month.DECEMBER, 1));
        repository.save(buggy)
            .doOnSuccess(c ->
                client.mutateWith(mockJwt())
                    .mutateWith(csrf())
                    .delete()
                    .uri("/car/{id}", Map.of("id", c.getId()))
                    .accept(MediaType.APPLICATION_JSON)
                    .exchange()
                    .expectStatus()
                    .isOk())
            .doOnError(e -> fail("Error: " + e.getMessage()))
            .subscribe();
    }

    @BeforeEach
    public void setup() {
        this.client = WebTestClient
            .bindToApplicationContext(this.context)
            .apply(springSecurity())
            .configureClient()
            .build();
    }
}
