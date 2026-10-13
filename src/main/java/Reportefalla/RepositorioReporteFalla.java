package Reportefalla;

import Reportefalla.ReporteFalla;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RepositorioReporteFalla
        extends JpaRepository<ReporteFalla, Long> {

    boolean existsByEquipo_IdAndEstadoNot(
            Long equipoId,
            String estado
    );
}