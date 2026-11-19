package cr.ac.ucr.if0009.edumentor_backend.dto;

import cr.ac.ucr.if0009.edumentor_backend.entity.Rol;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
    @NotBlank(message = "El carné es obligatorio")
    @Size(max = 20, message = "El carné no puede exceder 20 caracteres")
    String carne,

    @NotBlank(message = "El correo es obligatorio")
    @Email(message = "Formato de correo inválido")
    String email,

    @NotBlank(message = "La contraseña es obligatoria")
    @Size(min = 6, message = "La contraseña debe tener al menos 6 caracteres")
    String password,

    @NotBlank(message = "El nombre es obligatorio")
    String nombre,

    @NotBlank(message = "El apellido es obligatorio")
    String apellido,

    @NotNull(message = "El rol es obligatorio")
    Rol rol
) {}
