package com.example.carservice;

import lombok.Value;
import lombok.extern.log4j.Log4j2;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.netflix.eureka.EnableEurekaClient;
import org.springframework.context.annotation.Bean;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDate;
import java.time.Month;
import java.util.UUID;

@Log4j2
@EnableEurekaClient
@SpringBootApplication
public class CarServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(CarServiceApplication.class, args);
    }

    @Bean
    ApplicationRunner init(CarRepository repository) {
        Car ID = new Car(UUID.randomUUID(), "ID.", LocalDate.of(2019, Month.DECEMBER, 1));
        Car ID_CROZZ = new Car(UUID.randomUUID(), "ID. CROZZ", LocalDate.of(2021, Month.MAY, 1));
        Car ID_VIZZION = new Car(UUID.randomUUID(), "ID. VIZZION", LocalDate.of(2021, Month.DECEMBER, 1));
        Car ID_BUZZ = new Car(UUID.randomUUID(), "ID. BUZZ", LocalDate.of(2021, Month.DECEMBER, 1));

        return args -> {
            repository.deleteAll()
                .thenMany(repository.saveAll(Flux.just(ID, ID_BUZZ, ID_CROZZ, ID_VIZZION)))
                .thenMany(repository.findAll())
                .subscribe(c -> log.info("Saving: " + c));
        };
    }

}


@Document
@Value
class Car {
    @Id
    private final UUID id;
    private final String name;
    private final LocalDate releaseDate;
}


interface CarRepository extends ReactiveMongoRepository<Car, UUID> {
}


@RestController
class CarController {
    private CarRepository repository;

    public CarController(CarRepository repository) {
        this.repository = repository;
    }

    @PostMapping("/cars")
    @ResponseStatus(HttpStatus.CREATED)
    public Mono<Car> addCar(@RequestBody Car car) {
        return repository.save(car);
    }

    @GetMapping("/cars")
    public Flux<Car> getCars() {
        return repository.findAll();
    }

    @DeleteMapping("/car/{id}")
    public Mono<ResponseEntity<Void>> deleteCar(@PathVariable("id") UUID id) {
        return repository.deleteById(id)
            .then(Mono.just(new ResponseEntity<Void>(HttpStatus.OK)))
            .defaultIfEmpty(new ResponseEntity<Void>(HttpStatus.NOT_FOUND));
    }
}
