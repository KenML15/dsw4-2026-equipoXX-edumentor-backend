package cr.ac.ucr.if0009.edumentor_backend.dto;

import cr.ac.ucr.if0009.edumentor_backend.entity.ModalidadSesion;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;

public record SesionRequest(
        @NotNull(message = "El tutor es obligatorio")
        Long tutorId,

        @NotNull(message = "La materia es obligatoria")
        Long materiaId,

        @NotNull(message = "La fecha y hora son obligatorias")
        LocalDateTime fechaHora,

        @NotNull(message = "La duración es obligatoria")
        @Min(value = 30, message = "La duración mínima es de 30 minutos")
        @Max(value = 240, message = "La duración máxima es de 240 minutos")
        Integer duracionMinutos,

        @NotNull(message = "La modalidad es obligatoria")
        ModalidadSesion modalidad,

        @Size(max = 500, message = "El enlace o ubicación no puede exceder 500 caracteres")
        String enlaceOUbicacion
) {}
