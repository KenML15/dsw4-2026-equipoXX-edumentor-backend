package cr.ac.ucr.if0009.edumentor_backend.controller;

import cr.ac.ucr.if0009.edumentor_backend.dto.SesionRequest;
import cr.ac.ucr.if0009.edumentor_backend.dto.SesionResponse;
import cr.ac.ucr.if0009.edumentor_backend.dto.TutorResumenResponse;
import cr.ac.ucr.if0009.edumentor_backend.service.SesionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/sesiones")
@RequiredArgsConstructor
public class SesionController {

    private final SesionService sesionService;

    @PostMapping
    public ResponseEntity<SesionResponse> solicitar(
            @Valid @RequestBody SesionRequest request,
            Authentication authentication
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(sesionService.solicitar(request, authentication.getName()));
    }

    @GetMapping("/mias")
    public ResponseEntity<List<SesionResponse>> listarMias(Authentication authentication) {
        return ResponseEntity.ok(sesionService.listarMias(authentication.getName()));
    }

    @GetMapping("/tutores")
    public ResponseEntity<List<TutorResumenResponse>> listarTutores() {
        return ResponseEntity.ok(sesionService.listarTutores());
    }

    @GetMapping("/{id}")
    public ResponseEntity<SesionResponse> obtenerPorId(@PathVariable Long id, Authentication authentication) {
        return ResponseEntity.ok(sesionService.obtenerPorId(id, authentication.getName()));
    }

    @PutMapping("/{id}/aceptar")
    public ResponseEntity<SesionResponse> aceptar(@PathVariable Long id, Authentication authentication) {
        return ResponseEntity.ok(sesionService.aceptar(id, authentication.getName()));
    }

    @PutMapping("/{id}/rechazar")
    public ResponseEntity<SesionResponse> rechazar(@PathVariable Long id, Authentication authentication) {
        return ResponseEntity.ok(sesionService.rechazar(id, authentication.getName()));
    }

    @PutMapping("/{id}/cancelar")
    public ResponseEntity<SesionResponse> cancelar(@PathVariable Long id, Authentication authentication) {
        return ResponseEntity.ok(sesionService.cancelar(id, authentication.getName()));
    }

    @PutMapping("/{id}/iniciar")
    public ResponseEntity<SesionResponse> iniciar(@PathVariable Long id, Authentication authentication) {
        return ResponseEntity.ok(sesionService.iniciar(id, authentication.getName()));
    }

    @PutMapping("/{id}/finalizar")
    public ResponseEntity<SesionResponse> finalizar(@PathVariable Long id, Authentication authentication) {
        return ResponseEntity.ok(sesionService.finalizar(id, authentication.getName()));
    }
}
