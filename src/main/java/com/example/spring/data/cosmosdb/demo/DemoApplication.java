/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See LICENSE in the project root for
 * license information.
 */
package com.example.spring.data.cosmosdb.demo;

import com.google.common.collect.Lists;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.util.Assert;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.util.List;
import java.util.Optional;

@SpringBootApplication
public class DemoApplication implements CommandLineRunner {

    private static final Logger LOGGER = LoggerFactory.getLogger(DemoApplication.class);

    @Autowired
    private UserRepository repository;

    public static void main(String[] args) {
        SpringApplication.run(DemoApplication.class, args);
    }

    public void run(String... var1) throws Exception {
        final User testUser1 = new User("1", "Tasha", "Calderon", "4567 Main St Buffalo, NY 98052");
        final User testUser2 = new User("2", "John", "Doe", "4567 Main St Buffalo, NY 98052");
        final User testUser3 = new User("3", "Bob", "Martin", "4567 Main St Buffalo, NY 98052");
        final User testUser4 = new User("4", "Paul", "Martin", "4567 Main St Buffalo, NY 98052");
        final User testUser5 = new User("5", "Luke", "Robertson", "4567 Main St Buffalo, NY 98052");

        LOGGER.info("********Saving user: {}", testUser1);
        // Save the User class to Azure CosmosDB database.
        final Mono<User> saveUserMono = repository.save(testUser1);

        //  Nothing happens until we subscribe to these Monos.
        //  findById will not return the user as user is not present.
        final Mono<User> findByIdMono = repository.findById(testUser1.getId());
        final User findByIdUser = findByIdMono.block();
        Assert.isNull(findByIdUser, "User must be null");

        final User savedUser = saveUserMono.block();
        Assert.state(savedUser != null, "Saved user must not be null");
        Assert.state(savedUser.getFirstName().equals(testUser1.getFirstName()), "Saved user first" +
            " name doesn't match");

        LOGGER.info("********Finding Users by first name {}", testUser1.getFirstName());
        repository.findByFirstName(testUser1.getFirstName())
                  .flatMap(user -> {
                      LOGGER.info("User : {}", user);
                      return Flux.just(user);
                  })
                  .collectList()
                  .block();

        final Optional<User> optionalUserResult =
            repository.findById(testUser1.getId()).blockOptional();
        Assert.isTrue(optionalUserResult.isPresent(), "Cannot find user.");

        LOGGER.info("********Saving all users");
        repository.saveAll(Lists.newArrayList(testUser2, testUser3, testUser4, testUser5))
                  .flatMap(user -> {
                      LOGGER.info("User id {}", user.getId());
                      return Flux.just(user);
                  })
                  .collectList()
                  .block();

        LOGGER.info("********Finding all users");
        repository.findAll()
                  .flatMap(user -> {
                      LOGGER.info("User : {}:", user);
                      return Flux.just(user);
                  })
                  .collectList()
                  .block();

        LOGGER.info("********Finding user by id {}", testUser1.getId());
        final User result = optionalUserResult.get();
        Assert.state(result.getFirstName().equals(testUser1.getFirstName()), "query result " +
            "firstName doesn't match!");
        Assert.state(result.getLastName().equals(testUser1.getLastName()), "query result lastName" +
            " doesn't match!");

        LOGGER.info("User {}", result);

        String lastName = "Martin";
        LOGGER.info("********Finding users with last name {}", lastName);
        repository.findByLastName(lastName)
                  .flatMap(user -> {
                      LOGGER.info("Found user with last name {} : {}", lastName, user);
                      return Flux.just(user);
                  })
                  .collectList()
                  .block();

        LOGGER.info("********Deleting user with last name {}", lastName);
        repository.deleteByLastName(lastName)
                  .flatMap(user -> {
                      LOGGER.info("Deleted user with last name {} : {}", lastName, user);
                      return Flux.just(user);
                  })
                  .collectList()
                  .block();

        LOGGER.info("********Finding all users");
        repository.findAll()
                  .flatMap(user -> {
                      LOGGER.info("User : {}:", user);
                      return Flux.just(user);
                  })
                  .collectList()
                  .block();

        LOGGER.info("********Deleting user {}", testUser2);
        repository.delete(testUser2).block();

        LOGGER.info("********Finding all users");
        repository.findAll()
                  .flatMap(user -> {
                      LOGGER.info("User : {}:", user);
                      return Flux.just(user);
                  })
                  .collectList()
                  .block();
    }

    @PostConstruct
    public void setup() {
        // For this example, remove all of the existing records.
        LOGGER.info("Deleting existing resources");
        this.repository.deleteAll().block();
    }

    @PreDestroy
    public void cleanup() {
        LOGGER.info("Cleaning up resources");
        this.repository.deleteAll().block();
    }
}
