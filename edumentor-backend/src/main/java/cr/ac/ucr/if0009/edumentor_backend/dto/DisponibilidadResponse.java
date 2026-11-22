package cr.ac.ucr.if0009.edumentor_backend.dto;

import java.time.DayOfWeek;
import java.time.LocalTime;

public record DisponibilidadResponse(
        Long id,
        Long tutorId,
        String tutorNombre,
        DayOfWeek diaSemana,
        LocalTime horaInicio,
        LocalTime horaFin,
        Boolean activo
) {}
