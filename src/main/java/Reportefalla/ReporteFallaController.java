package Reportefalla;

import Equipo.Equipo;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;

import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

@RestController
@RequestMapping("/api/reportes-fallas")
@CrossOrigin(origins = "http://localhost:4200")
public class ReporteFallaController {

    private final RepositorioReporteFalla repositorio;
    private final EntityManager entityManager;

    private static final Set<String> PRIORIDADES = Set.of(
            "baja", "media", "alta"
    );

    private static final Set<String> ESTADOS = Set.of(
            "pendiente",
            "revision",
            "reparacion",
            "resuelto"
    );

    public ReporteFallaController(
            RepositorioReporteFalla repositorio,
            EntityManager entityManager
    ) {
        this.repositorio = repositorio;
        this.entityManager = entityManager;
    }

    public record NuevoReporte(
            Long equipoId,
            String reportadoPor,
            String descripcion,
            String prioridad
    ) {}

    public record Seguimiento(
            String estado,
            String diagnostico,
            String solucion
    ) {}

    @GetMapping
    public List<ReporteFalla> listar() {
        return repositorio.findAll(
                Sort.by(Sort.Direction.DESC, "id")
        );
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Transactional
    public ReporteFalla crear(@RequestBody NuevoReporte datos) {
        if (datos.equipoId() == null) {
            throw error("Selecciona un equipo.");
        }

        String reportadoPor = texto(datos.reportadoPor());
        String descripcion = texto(datos.descripcion());
        String prioridad = texto(datos.prioridad())
                .toLowerCase(Locale.ROOT);

        validar("Quién reporta", reportadoPor, 200, true);
        validar("Descripción de la falla", descripcion, 2000, true);

        if (!PRIORIDADES.contains(prioridad)) {
            throw error("Selecciona una prioridad válida.");
        }

        // Evita crear simultáneamente dos reportes abiertos del mismo equipo.
        Equipo equipo = entityManager.find(
                Equipo.class,
                datos.equipoId(),
                LockModeType.PESSIMISTIC_WRITE
        );

        if (equipo == null) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "El equipo no existe."
            );
        }

        if ("desechado".equals(equipo.estado)) {
            throw error("No se puede reportar un equipo dado de baja.");
        }

        if (repositorio.existsByEquipo_IdAndEstadoNot(
                equipo.id,
                "resuelto"
        )) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Este equipo ya tiene un reporte abierto. " +
                            "Consulta su seguimiento."
            );
        }

        Instant ahora = Instant.now();

        ReporteFalla reporte = new ReporteFalla();
        reporte.equipo = equipo;
        reporte.reportadoPor = reportadoPor;
        reporte.descripcion = descripcion;
        reporte.prioridad = prioridad;

        // Si el equipo ya está en reparación, el reporte refleja ese estado.
        reporte.estado = "reparacion".equals(equipo.estado)
                ? "reparacion"
                : "pendiente";

        reporte.diagnostico = "";
        reporte.solucion = "";
        reporte.fechaReporte = ahora;
        reporte.fechaActualizacion = ahora;

        return repositorio.saveAndFlush(reporte);
    }

    @PutMapping("/{id}/seguimiento")
    @Transactional
    public ReporteFalla actualizar(
            @PathVariable("id") Long id,
            @RequestBody Seguimiento datos
    ) {
        ReporteFalla reporte = entityManager.find(
                ReporteFalla.class,
                id,
                LockModeType.PESSIMISTIC_WRITE
        );

        if (reporte == null) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "El reporte no existe."
            );
        }

        String estado = texto(datos.estado())
                .toLowerCase(Locale.ROOT);

        String diagnostico = texto(datos.diagnostico());
        String solucion = texto(datos.solucion());

        if (!ESTADOS.contains(estado)) {
            throw error("Estado de reporte no válido.");
        }

        validar("Diagnóstico", diagnostico, 2000, false);
        validar("Solución", solucion, 2000, false);

        if ("resuelto".equals(reporte.estado)) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "El reporte ya está resuelto y se conserva como historial."
            );
        }

        boolean cambioPermitido =
                estado.equals(reporte.estado)
                        || (
                        "pendiente".equals(reporte.estado)
                                && "revision".equals(estado)
                )
                        || (
                        "revision".equals(reporte.estado)
                                && Set.of("reparacion", "resuelto").contains(estado)
                )
                        || (
                        "reparacion".equals(reporte.estado)
                                && "resuelto".equals(estado)
                );

        if (!cambioPermitido) {
            throw error(
                    "Sigue el orden: Pendiente, En revisión, " +
                            "En reparación y Resuelto. " +
                            "También puedes resolver desde En revisión."
            );
        }

        if (
                Set.of("reparacion", "resuelto").contains(estado)
                        && diagnostico.isBlank()
        ) {
            throw error("Captura el diagnóstico.");
        }

        if ("resuelto".equals(estado) && solucion.isBlank()) {
            throw error("Describe la solución antes de resolver el reporte.");
        }

        Equipo equipo = entityManager.find(
                Equipo.class,
                reporte.equipo.id,
                LockModeType.PESSIMISTIC_WRITE
        );

        if (
                !estado.equals(reporte.estado)
                        && Set.of("reparacion", "resuelto").contains(estado)
                        && "desechado".equals(equipo.estado)
        ) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "El equipo fue dado de baja. Revisa su estado en Equipos."
            );
        }

        if ("reparacion".equals(estado)) {
            equipo.estado = "reparacion";
        }

        if ("resuelto".equals(estado)) {
            // Solo cambia a reparado si el equipo estaba en reparación.
            if ("reparacion".equals(equipo.estado)) {
                equipo.estado = "reparado";
            }

            reporte.fechaResolucion = Instant.now();
        }

        reporte.estado = estado;
        reporte.diagnostico = diagnostico;
        reporte.solucion = solucion;
        reporte.fechaActualizacion = Instant.now();

        return repositorio.saveAndFlush(reporte);
    }

    private String texto(String valor) {
        return valor == null ? "" : valor.trim();
    }

    private void validar(
            String nombre,
            String valor,
            int maximo,
            boolean obligatorio
    ) {
        if (obligatorio && valor.isBlank()) {
            throw error(nombre + " es obligatorio.");
        }

        if (valor.length() > maximo) {
            throw error(
                    nombre + " admite hasta " + maximo + " caracteres."
            );
        }
    }

    private ResponseStatusException error(String mensaje) {
        return new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                mensaje
        );
    }

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<Map<String, String>> manejarError(
            ResponseStatusException error
    ) {
        return ResponseEntity
                .status(error.getStatusCode())
                .body(Map.of(
                        "mensaje",
                        error.getReason() == null
                                ? "No se pudo realizar la operación."
                                : error.getReason()
                ));
    }
}