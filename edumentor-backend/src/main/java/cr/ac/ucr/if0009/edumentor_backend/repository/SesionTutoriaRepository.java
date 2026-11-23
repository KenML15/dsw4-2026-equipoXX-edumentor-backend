package cr.ac.ucr.if0009.edumentor_backend.repository;

import cr.ac.ucr.if0009.edumentor_backend.entity.EstadoSesion;
import cr.ac.ucr.if0009.edumentor_backend.entity.SesionTutoria;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface SesionTutoriaRepository extends JpaRepository<SesionTutoria, Long> {

    @Query("""
            SELECT s FROM SesionTutoria s
            JOIN FETCH s.estudiante
            JOIN FETCH s.tutor
            JOIN FETCH s.materia
            WHERE s.id = :id
            """)
    Optional<SesionTutoria> findByIdConDetalle(@Param("id") Long id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT s FROM SesionTutoria s WHERE s.id = :id")
    Optional<SesionTutoria> bloquearPorId(@Param("id") Long id);

    @Query("""
            SELECT s FROM SesionTutoria s
            JOIN FETCH s.estudiante
            JOIN FETCH s.tutor
            JOIN FETCH s.materia
            WHERE s.estudiante.id = :estudianteId
            ORDER BY s.fechaHora DESC
            """)
    List<SesionTutoria> findByEstudianteIdConDetalle(@Param("estudianteId") Long estudianteId);

    @Query("""
            SELECT s FROM SesionTutoria s
            JOIN FETCH s.estudiante
            JOIN FETCH s.tutor
            JOIN FETCH s.materia
            WHERE s.tutor.id = :tutorId
            ORDER BY s.fechaHora DESC
            """)
    List<SesionTutoria> findByTutorIdConDetalle(@Param("tutorId") Long tutorId);

    @EntityGraph(attributePaths = {"estudiante", "tutor", "materia"})
    List<SesionTutoria> findAllByOrderByFechaHoraDesc();

    @Query("""
            SELECT s FROM SesionTutoria s
            JOIN FETCH s.estudiante
            JOIN FETCH s.tutor
            JOIN FETCH s.materia
            WHERE s.estado = :estado
              AND s.fechaHora < :fin
              AND s.fechaHoraFin > :inicio
            ORDER BY s.fechaHora ASC
            """)
    List<SesionTutoria> findPorEstadoQueSeCruzan(
            @Param("estado") EstadoSesion estado,
            @Param("inicio") LocalDateTime inicio,
            @Param("fin") LocalDateTime fin
    );

    @Query("""
            SELECT CASE WHEN COUNT(s) > 0 THEN true ELSE false END
            FROM SesionTutoria s
            WHERE s.tutor.id = :tutorId
              AND s.estado IN :estados
              AND (:excluirId IS NULL OR s.id <> :excluirId)
              AND s.fechaHora < :fin
              AND s.fechaHoraFin > :inicio
            """)
    boolean existeTraslapeTutor(
            @Param("tutorId") Long tutorId,
            @Param("estados") Collection<EstadoSesion> estados,
            @Param("inicio") LocalDateTime inicio,
            @Param("fin") LocalDateTime fin,
            @Param("excluirId") Long excluirId
    );

    @Query("""
            SELECT CASE WHEN COUNT(s) > 0 THEN true ELSE false END
            FROM SesionTutoria s
            WHERE s.estudiante.id = :estudianteId
              AND s.estado IN :estados
              AND (:excluirId IS NULL OR s.id <> :excluirId)
              AND s.fechaHora < :fin
              AND s.fechaHoraFin > :inicio
            """)
    boolean existeTraslapeEstudiante(
            @Param("estudianteId") Long estudianteId,
            @Param("estados") Collection<EstadoSesion> estados,
            @Param("inicio") LocalDateTime inicio,
            @Param("fin") LocalDateTime fin,
            @Param("excluirId") Long excluirId
    );
}
