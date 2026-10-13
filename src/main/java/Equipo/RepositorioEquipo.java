package Equipo;

import org.springframework.data.jpa.repository.JpaRepository;

public interface RepositorioEquipo
        extends JpaRepository<Equipo, Long> {

    boolean existsByEtiquetaAndTipo(
            String etiqueta,
            String tipo
    );

    boolean existsByEtiquetaAndTipoAndIdNot(
            String etiqueta,
            String tipo,
            Long id
    );
}