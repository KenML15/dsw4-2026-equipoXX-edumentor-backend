package cr.ac.ucr.if0009.edumentor_backend.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.DayOfWeek;
import java.time.LocalTime;

@Entity
@Table(
        name = "disponibilidades_tutor",
        indexes = {
                @Index(name = "idx_disp_tutor_dia_activo", columnList = "tutor_id, dia_semana, activo")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DisponibilidadTutor {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "tutor_id", nullable = false)
    private Usuario tutor;

    @Enumerated(EnumType.STRING)
    @Column(name = "dia_semana", nullable = false, length = 15)
    private DayOfWeek diaSemana;

    @Column(name = "hora_inicio", nullable = false)
    private LocalTime horaInicio;

    @Column(name = "hora_fin", nullable = false)
    private LocalTime horaFin;

    @Column(nullable = false)
    @Builder.Default
    private Boolean activo = true;
}
