package cr.ac.ucr.if0009.edumentor_backend.controller;


import cr.ac.ucr.if0009.edumentor_backend.dto.AuthResponse;
import cr.ac.ucr.if0009.edumentor_backend.dto.LoginRequest;
import cr.ac.ucr.if0009.edumentor_backend.dto.RegisterRequest;
import cr.ac.ucr.if0009.edumentor_backend.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(authService.register(request));
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }
}
