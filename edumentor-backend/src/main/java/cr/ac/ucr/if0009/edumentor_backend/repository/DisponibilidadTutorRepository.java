package cr.ac.ucr.if0009.edumentor_backend.repository;

import cr.ac.ucr.if0009.edumentor_backend.entity.DisponibilidadTutor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

public interface DisponibilidadTutorRepository extends JpaRepository<DisponibilidadTutor, Long> {

    @Query("""
            SELECT d FROM DisponibilidadTutor d
            JOIN FETCH d.tutor
            WHERE d.id = :id
            """)
    Optional<DisponibilidadTutor> findByIdWithTutor(@Param("id") Long id);

    @Query("""
            SELECT d FROM DisponibilidadTutor d
            JOIN FETCH d.tutor
            WHERE d.tutor.id = :tutorId
            ORDER BY d.diaSemana ASC, d.horaInicio ASC
            """)
    List<DisponibilidadTutor> findByTutorIdWithTutor(@Param("tutorId") Long tutorId);

    @Query("""
            SELECT d FROM DisponibilidadTutor d
            JOIN FETCH d.tutor
            WHERE d.tutor.id = :tutorId
              AND d.activo = true
            ORDER BY d.diaSemana ASC, d.horaInicio ASC
            """)
    List<DisponibilidadTutor> findActivasByTutorIdWithTutor(@Param("tutorId") Long tutorId);

    @Query("""
            SELECT d FROM DisponibilidadTutor d
            WHERE d.tutor.id = :tutorId
              AND d.diaSemana = :diaSemana
              AND d.activo = true
            ORDER BY d.horaInicio ASC
            """)
    List<DisponibilidadTutor> findActivasByTutorIdAndDiaSemana(
            @Param("tutorId") Long tutorId,
            @Param("diaSemana") DayOfWeek diaSemana
    );

    @Query("""
            SELECT CASE WHEN COUNT(d) > 0 THEN true ELSE false END
            FROM DisponibilidadTutor d
            WHERE d.tutor.id = :tutorId
              AND d.diaSemana = :diaSemana
              AND d.activo = true
              AND (:excluirId IS NULL OR d.id <> :excluirId)
              AND d.horaInicio < :horaFin
              AND d.horaFin > :horaInicio
            """)
    boolean existeTraslapeActivo(
            @Param("tutorId") Long tutorId,
            @Param("diaSemana") DayOfWeek diaSemana,
            @Param("horaInicio") LocalTime horaInicio,
            @Param("horaFin") LocalTime horaFin,
            @Param("excluirId") Long excluirId
    );
}
