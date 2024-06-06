/*
 * Copyright 2024 Peter Danielsson
 */
package org.capeph.lookup;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class LookupService {

  public static void main(String[] args) {
    SpringApplication.run(LookupService.class, args);
  }

  @Bean
  public OpenAPI openAPI() {
    return new OpenAPI()
        .info(
            new Info()
                .title("SpringDoc example")
                .description("SpringDoc application")
                .version("v0.0.1"));
  }

  @Bean
  public CommandLineRunner commandLineRunner(ApplicationContext ctx) {
    return args -> {

      // TODO: provide some better output.. like a list of REST endpoints
      System.out.println("Welcome to the Lookup Server");
    };
  }
}
