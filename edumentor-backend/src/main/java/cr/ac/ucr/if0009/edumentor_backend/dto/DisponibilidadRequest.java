package cr.ac.ucr.if0009.edumentor_backend.dto;

import jakarta.validation.constraints.NotNull;

import java.time.DayOfWeek;
import java.time.LocalTime;

public record DisponibilidadRequest(
        Long tutorId,

        @NotNull(message = "El día de la semana es obligatorio")
        DayOfWeek diaSemana,

        @NotNull(message = "La hora de inicio es obligatoria")
        LocalTime horaInicio,

        @NotNull(message = "La hora de fin es obligatoria")
        LocalTime horaFin,

        Boolean activo
) {}
