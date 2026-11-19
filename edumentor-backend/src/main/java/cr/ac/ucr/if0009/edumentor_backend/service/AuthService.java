package cr.ac.ucr.if0009.edumentor_backend.service;


import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import cr.ac.ucr.if0009.edumentor_backend.dto.AuthResponse;
import cr.ac.ucr.if0009.edumentor_backend.dto.LoginRequest;
import cr.ac.ucr.if0009.edumentor_backend.dto.RegisterRequest;
import cr.ac.ucr.if0009.edumentor_backend.dto.UsuarioResponse;
import cr.ac.ucr.if0009.edumentor_backend.entity.Usuario;
import cr.ac.ucr.if0009.edumentor_backend.exception.RecursoNoEncontradoException;
import cr.ac.ucr.if0009.edumentor_backend.exception.ReglaNegocioException;
import cr.ac.ucr.if0009.edumentor_backend.repository.UsuarioRepository;
import cr.ac.ucr.if0009.edumentor_backend.security.JwtService;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (usuarioRepository.existsByEmail(request.email())) {
            throw new ReglaNegocioException("El correo electrónico ya está registrado");
        }
        if (usuarioRepository.existsByCarne(request.carne())) {
            throw new ReglaNegocioException("El número de carné ya está registrado");
        }

        Usuario usuario = Usuario.builder()
                .carne(request.carne())
                .email(request.email())
                .passwordHash(passwordEncoder.encode(request.password()))
                .nombre(request.nombre())
                .apellido(request.apellido())
                .rol(request.rol())
                .build();

        usuario = usuarioRepository.save(usuario);
        String token = jwtService.generateToken(usuario);

        return new AuthResponse(token, mapToResponse(usuario));
    }

    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.email(), request.password())
        );

        Usuario usuario = usuarioRepository.findByEmail(request.email())
                .orElseThrow(() -> new RecursoNoEncontradoException("Usuario no encontrado"));

        String token = jwtService.generateToken(usuario);
        return new AuthResponse(token, mapToResponse(usuario));
    }

    @Transactional(readOnly = true)
    public UsuarioResponse getPerfilActual(String email) {
        Usuario usuario = usuarioRepository.findByEmail(email)
                .orElseThrow(() -> new RecursoNoEncontradoException("Usuario no encontrado"));
        return mapToResponse(usuario);
    }

    private UsuarioResponse mapToResponse(Usuario u) {
        return new UsuarioResponse(
                u.getId(),
                u.getCarne(),
                u.getEmail(),
                u.getNombre(),
                u.getApellido(),
                u.getRol()
        );
    }
}
