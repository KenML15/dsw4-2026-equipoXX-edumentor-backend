package cr.ac.ucr.if0009.edumentor_backend.dto;

import cr.ac.ucr.if0009.edumentor_backend.entity.EstadoSesion;
import cr.ac.ucr.if0009.edumentor_backend.entity.ModalidadSesion;

import java.time.LocalDateTime;

public record SesionResponse(
        Long id,
        Long estudianteId,
        String estudianteNombre,
        Long tutorId,
        String tutorNombre,
        Long materiaId,
        String materiaCodigo,
        String materiaNombre,
        LocalDateTime fechaHora,
        LocalDateTime fechaHoraFin,
        Integer duracionMinutos,
        ModalidadSesion modalidad,
        EstadoSesion estado,
        String enlaceOUbicacion,
        LocalDateTime fechaCreacion,
        LocalDateTime iniciableDesde,
        LocalDateTime finalizableDesde,
        LocalDateTime cancelableHasta
) {}
