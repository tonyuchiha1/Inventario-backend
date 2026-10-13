package Reportefalla;

import Equipo.Equipo;
import jakarta.persistence.*;

import java.time.Instant;

@Entity
@Table(name = "reportes_fallas")
public class ReporteFalla {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "equipo_id", nullable = false)
    public Equipo equipo;

    @Column(nullable = false, length = 200)
    public String reportadoPor;

    @Column(nullable = false, length = 2000)
    public String descripcion;

    @Column(nullable = false, length = 20)
    public String prioridad;

    @Column(nullable = false, length = 30)
    public String estado;

    @Column(length = 2000)
    public String diagnostico;

    @Column(length = 2000)
    public String solucion;

    @Column(nullable = false, updatable = false)
    public Instant fechaReporte;

    @Column(nullable = false)
    public Instant fechaActualizacion;

    public Instant fechaResolucion;

    public String getFolio() {
        return id == null
                ? ""
                : "INC-" + String.format("%06d", id);
    }
}