package net.perezmora.inventariobackend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication(scanBasePackages = {
        "net.perezmora.inventariobackend",
        "Equipo",
        "ReporteFalla"
})
@EntityScan(basePackages = {
        "Equipo",
        "ReporteFalla"
})
@EnableJpaRepositories(basePackages = {
        "Equipo",
        "ReporteFalla"
})
public class InventarioBackendApplication {

    public static void main(String[] args) {
        SpringApplication.run(
                InventarioBackendApplication.class,
                args
        );
    }
}