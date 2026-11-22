package cr.ac.ucr.if0009.edumentor_backend.dto;

public record MateriaResponse(
        Long id,
        String codigo,
        String nombre,
        String descripcion,
        Integer creditos,
        Boolean activa
) {}
