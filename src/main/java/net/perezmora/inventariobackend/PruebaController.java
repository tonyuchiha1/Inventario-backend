package net.perezmora.inventariobackend;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@CrossOrigin(origins = "http://localhost:4200")
public class PruebaController {

    private final JdbcTemplate jdbcTemplate;

    public PruebaController(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @GetMapping("/api/prueba")
    public Map<String, Object> probarConexion() {
        return jdbcTemplate.queryForMap("""
                SELECT current_database() AS base,
                       current_user AS usuario,
                       CURRENT_TIMESTAMP AS fecha
                """);
    }
}