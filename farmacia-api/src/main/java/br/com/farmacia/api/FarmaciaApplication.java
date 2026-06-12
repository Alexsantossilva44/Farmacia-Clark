package br.com.farmacia.api;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Classe principal do Sistema Farmacêutico.
 *
 * <p>Stack: Java 21 · Spring Boot 3.3 · PostgreSQL · RabbitMQ · Flyway</p>
 * <p>Arquitetura: DDD + Hexagonal | AlgaWorks ESR | QA Júlio de Lima</p>
 *
 * @author Alex Silva e Claude
 * @version 1.0.0
 */
@SpringBootApplication(scanBasePackages = "br.com.farmacia")
@EnableCaching
@EnableScheduling
public class FarmaciaApplication {

    public static void main(String[] args) {
        SpringApplication.run(FarmaciaApplication.class, args);
    }
}
