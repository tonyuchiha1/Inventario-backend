package Equipo;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.text.Normalizer;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

@RestController
@RequestMapping("/api/equipos")
@CrossOrigin(origins = "http://localhost:4200")
public class EquipoController {

    private final RepositorioEquipo repository;

    private static final List<String> TIPOS = List.of(
            "CPU",
            "Laptop",
            "Monitor",
            "Teclado",
            "Mouse",
            "Proyector",
            "Impresora",
            "Regulador",
            "No break",
            "Switch",
            "Router",
            "Otro"
    );

    private static final Set<String> ESTADOS = Set.of(
            "disponible",
            "activo",
            "nuevo",
            "danado",
            "reparacion",
            "reparado",
            "desechado"
    );

    private static final List<String> AREAS = List.of(
            "Contabilidad Gubernamental",
            "Comercial",
            "Servicios Jurídicos",
            "Recursos Humanos, Tecnológicos y Materiales",
            "Contraloría Interna",
            "Control Financiero",
            "Operaciones",
            "Contabilidad Interna",
            "Investigación"
    );

    public EquipoController(RepositorioEquipo repository) {
        this.repository = repository;
    }

    @GetMapping
    public List<Equipo> listar() {
        return repository.findAll(
                Sort.by(Sort.Direction.DESC, "id")
        );
    }

    @GetMapping("/{id}")
    public Equipo consultar(@PathVariable("id") Long id) {
        return buscar(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Equipo agregar(@RequestBody Equipo equipo) {
        preparar(equipo);

        if (repository.existsByEtiquetaAndTipo(
                equipo.etiqueta,
                equipo.tipo
        )) {
            throw duplicado();
        }

        equipo.id = null;
        equipo.fechaRegistro = Instant.now();

        return guardar(equipo);
    }

    @PutMapping("/{id}")
    public Equipo editar(
            @PathVariable("id") Long id,
            @RequestBody Equipo equipo
    ) {
        Equipo original = buscar(id);

        preparar(equipo);

        if (repository.existsByEtiquetaAndTipoAndIdNot(
                equipo.etiqueta,
                equipo.tipo,
                id
        )) {
            throw duplicado();
        }

        equipo.id = original.id;
        equipo.fechaRegistro = original.fechaRegistro;

        return guardar(equipo);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void eliminar(@PathVariable("id") Long id) {
        repository.delete(buscar(id));
    }

    private Equipo buscar(Long id) {
        return repository.findById(id).orElseThrow(
                () -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "El equipo no existe."
                )
        );
    }

    private Equipo guardar(Equipo equipo) {
        try {
            return repository.saveAndFlush(equipo);
        } catch (DataIntegrityViolationException error) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "No se pudo guardar: revisa los datos y la etiqueta."
            );
        }
    }

    private void preparar(Equipo equipo) {
        equipo.etiqueta = texto(equipo.etiqueta)
                .toUpperCase(Locale.ROOT);

        String tipoRecibido = texto(equipo.tipo);

        equipo.tipo = TIPOS.stream()
                .filter(tipo -> tipo.equalsIgnoreCase(tipoRecibido))
                .findFirst()
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "Tipo de equipo no reconocido."
                ));

        equipo.marca = texto(equipo.marca);
        equipo.modelo = texto(equipo.modelo);
        equipo.serie = texto(equipo.serie);
        equipo.usuario = texto(equipo.usuario);
        equipo.ubicacion = texto(equipo.ubicacion);
        equipo.area = texto(equipo.area);
        equipo.observaciones = texto(equipo.observaciones);
        equipo.estado = texto(equipo.estado)
                .toLowerCase(Locale.ROOT);

        validarCampo("Etiqueta", equipo.etiqueta, 80, true);
        validarCampo("Marca", equipo.marca, 100, true);
        validarCampo("Modelo", equipo.modelo, 150, true);
        validarCampo("Serie", equipo.serie, 150, false);
        validarCampo("Usuario", equipo.usuario, 200, false);
        validarCampo("Ubicación", equipo.ubicacion, 200, true);
        validarCampo("Área", equipo.area, 200, false);
        validarCampo(
                "Observaciones",
                equipo.observaciones,
                2000,
                false
        );

        equipo.area = validarArea(equipo.area);

        if (!ESTADOS.contains(equipo.estado)) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Estado no válido."
            );
        }

        if (equipo.estado.equals("activo")
                && equipo.usuario.isBlank()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Los equipos asignados requieren un usuario."
            );
        }
    }

    private String validarArea(String valor) {
        String areaRecibida = normalizarArea(valor);

        // El área es opcional para los equipos sin clasificación.
        if (areaRecibida.isEmpty()) {
            return "";
        }

        // Devuelve el nombre del catálogo con sus acentos y mayúsculas.
        return AREAS.stream()
                .filter(area ->
                        normalizarArea(area).equals(areaRecibida)
                )
                .findFirst()
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "Selecciona una de las áreas del catálogo."
                ));
    }

    private String normalizarArea(String valor) {
        return Normalizer
                .normalize(
                        texto(valor),
                        Normalizer.Form.NFD
                )
                .replaceAll("\\p{M}+", "")
                .toLowerCase(Locale.ROOT)
                .replaceAll("\\s+", " ");
    }

    private void validarCampo(
            String nombre,
            String valor,
            int maximo,
            boolean obligatorio
    ) {
        if (obligatorio && valor.isBlank()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    nombre + " es obligatorio."
            );
        }

        if (valor.length() > maximo) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    nombre + " admite hasta " + maximo + " caracteres."
            );
        }
    }

    private String texto(String valor) {
        return valor == null ? "" : valor.trim();
    }

    private ResponseStatusException duplicado() {
        return new ResponseStatusException(
                HttpStatus.CONFLICT,
                "Ya existe un equipo del mismo tipo con esa etiqueta."
        );
    }

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<Map<String, String>> manejarError(
            ResponseStatusException error
    ) {
        String mensaje = error.getReason() == null
                ? "No se pudo realizar la operación."
                : error.getReason();

        return ResponseEntity
                .status(error.getStatusCode())
                .body(Map.of("mensaje", mensaje));
    }
}