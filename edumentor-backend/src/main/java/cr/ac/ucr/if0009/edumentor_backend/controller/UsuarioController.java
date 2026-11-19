package cr.ac.ucr.if0009.edumentor_backend.controller;


import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import cr.ac.ucr.if0009.edumentor_backend.dto.UsuarioResponse;
import cr.ac.ucr.if0009.edumentor_backend.service.AuthService;

@RestController
@RequestMapping("/api/usuarios")
@RequiredArgsConstructor
public class UsuarioController {

    private final AuthService authService;

    @GetMapping("/me")
    public ResponseEntity<UsuarioResponse> getMe(Authentication authentication) {
        return ResponseEntity.ok(authService.getPerfilActual(authentication.getName()));
    }
}
