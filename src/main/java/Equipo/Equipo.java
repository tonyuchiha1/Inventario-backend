package Equipo;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(
        name = "equipos",
        uniqueConstraints = @UniqueConstraint(
                name = "uq_equipo_etiqueta_tipo",
                columnNames = {"etiqueta", "tipo"}
        )
)
public class Equipo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;

    @Column(nullable = false, length = 80)
    public String etiqueta;

    @Column(nullable = false, length = 50)
    public String tipo;

    @Column(nullable = false, length = 100)
    public String marca;

    @Column(nullable = false, length = 150)
    public String modelo;

    @Column(length = 150)
    public String serie;

    @Column(length = 200)
    public String usuario;

    @Column(nullable = false, length = 200)
    public String ubicacion;

    @Column(length = 200)
    public String area;

    @Column(length = 2000)
    public String observaciones;

    @Column(nullable = false, length = 30)
    public String estado;

    @Column(nullable = false, updatable = false)
    public Instant fechaRegistro;
}