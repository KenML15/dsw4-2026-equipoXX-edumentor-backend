package cr.ac.ucr.if0009.edumentor_backend.dto;

public record AuthResponse(
    String token,
    UsuarioResponse usuario
) {}
