package cr.ac.ucr.if0009.edumentor_backend.controller;

import cr.ac.ucr.if0009.edumentor_backend.dto.MateriaRequest;
import cr.ac.ucr.if0009.edumentor_backend.dto.MateriaResponse;
import cr.ac.ucr.if0009.edumentor_backend.service.MateriaService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/materias")
@RequiredArgsConstructor
public class MateriaController {

    private final MateriaService materiaService;

    @GetMapping
    public ResponseEntity<List<MateriaResponse>> listar(
            @RequestParam(name = "soloActivas", defaultValue = "false") boolean soloActivas
    ) {
        return ResponseEntity.ok(materiaService.listar(soloActivas));
    }

    @GetMapping("/{id}")
    public ResponseEntity<MateriaResponse> obtenerPorId(@PathVariable Long id) {
        return ResponseEntity.ok(materiaService.obtenerPorId(id));
    }

    @PostMapping
    public ResponseEntity<MateriaResponse> crear(@Valid @RequestBody MateriaRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(materiaService.crear(request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<MateriaResponse> actualizar(
            @PathVariable Long id,
            @Valid @RequestBody MateriaRequest request
    ) {
        return ResponseEntity.ok(materiaService.actualizar(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        materiaService.eliminar(id);
        return ResponseEntity.noContent().build();
    }
}
