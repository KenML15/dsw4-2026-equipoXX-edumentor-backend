package cr.ac.ucr.if0009.edumentor_backend.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "materias")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Materia {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 20)
    private String codigo;

    @Column(nullable = false, length = 100)
    private String nombre;

    @Column(length = 500)
    private String descripcion;

    @Column(nullable = false)
    private Integer creditos;

    @Column(nullable = false)
    @Builder.Default
    private Boolean activa = true;
}
