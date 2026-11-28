package cr.ac.ucr.if0009.edumentor_backend.dto;

public record TutorResumenResponse(
        Long id,
        String nombreCompleto,
        Double promedioCalificacion,
        Integer totalEvaluaciones
) {}
