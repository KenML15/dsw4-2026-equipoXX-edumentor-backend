package cr.ac.ucr.if0009.edumentor_backend.dto;

import cr.ac.ucr.if0009.edumentor_backend.entity.Rol;

public record UsuarioResponse(
    Long id,
    String carne,
    String email,
    String nombre,
    String apellido,
    Rol rol
) {}
