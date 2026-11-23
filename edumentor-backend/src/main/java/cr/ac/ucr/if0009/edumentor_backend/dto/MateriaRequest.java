package cr.ac.ucr.if0009.edumentor_backend.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record MateriaRequest(
        @NotBlank(message = "El código de la materia es obligatorio")
        @Size(max = 20, message = "El código no puede exceder 20 caracteres")
        String codigo,

        @NotBlank(message = "El nombre de la materia es obligatorio")
        @Size(max = 100, message = "El nombre no puede exceder 100 caracteres")
        String nombre,

        @Size(max = 500, message = "La descripción no puede exceder 500 caracteres")
        String descripcion,

        @NotNull(message = "Los créditos son obligatorios")
        @Min(value = 1, message = "Los créditos deben ser al menos 1")
        @Max(value = 20, message = "Los créditos no pueden exceder 20")
        Integer creditos,

        Boolean activa
) {}
