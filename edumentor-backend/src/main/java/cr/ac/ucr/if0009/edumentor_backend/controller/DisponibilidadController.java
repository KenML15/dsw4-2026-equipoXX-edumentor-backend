package cr.ac.ucr.if0009.edumentor_backend.controller;

import cr.ac.ucr.if0009.edumentor_backend.dto.DisponibilidadRequest;
import cr.ac.ucr.if0009.edumentor_backend.dto.DisponibilidadResponse;
import cr.ac.ucr.if0009.edumentor_backend.service.DisponibilidadService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/disponibilidades")
@RequiredArgsConstructor
public class DisponibilidadController {

    private final DisponibilidadService disponibilidadService;

    @GetMapping("/mias")
    public ResponseEntity<List<DisponibilidadResponse>> listarMias(Authentication authentication) {
        return ResponseEntity.ok(disponibilidadService.listarMias(authentication.getName()));
    }

    @GetMapping("/tutor/{tutorId}")
    public ResponseEntity<List<DisponibilidadResponse>> listarPorTutor(
            @PathVariable Long tutorId,
            @RequestParam(name = "soloActivas", defaultValue = "false") boolean soloActivas
    ) {
        return ResponseEntity.ok(disponibilidadService.listarPorTutor(tutorId, soloActivas));
    }

    @GetMapping("/verificar")
    public ResponseEntity<Map<String, Boolean>> verificarDisponibilidad(
            @RequestParam Long tutorId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime fechaHora,
            @RequestParam int duracion
    ) {
        boolean disponible = disponibilidadService.estaDisponible(tutorId, fechaHora, duracion);
        return ResponseEntity.ok(Map.of("disponible", disponible));
    }

    @PostMapping
    public ResponseEntity<DisponibilidadResponse> crear(
            @Valid @RequestBody DisponibilidadRequest request,
            Authentication authentication
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(disponibilidadService.crear(request, authentication.getName()));
    }

    @PutMapping("/{id}")
    public ResponseEntity<DisponibilidadResponse> actualizar(
            @PathVariable Long id,
            @Valid @RequestBody DisponibilidadRequest request,
            Authentication authentication
    ) {
        return ResponseEntity.ok(disponibilidadService.actualizar(id, request, authentication.getName()));
    }

    @PutMapping("/{id}/activar")
    public ResponseEntity<DisponibilidadResponse> activar(
            @PathVariable Long id,
            Authentication authentication
    ) {
        return ResponseEntity.ok(disponibilidadService.cambiarEstado(id, true, authentication.getName()));
    }

    @PutMapping("/{id}/desactivar")
    public ResponseEntity<DisponibilidadResponse> desactivar(
            @PathVariable Long id,
            Authentication authentication
    ) {
        return ResponseEntity.ok(disponibilidadService.cambiarEstado(id, false, authentication.getName()));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(
            @PathVariable Long id,
            Authentication authentication
    ) {
        disponibilidadService.eliminar(id, authentication.getName());
        return ResponseEntity.noContent().build();
    }
}
