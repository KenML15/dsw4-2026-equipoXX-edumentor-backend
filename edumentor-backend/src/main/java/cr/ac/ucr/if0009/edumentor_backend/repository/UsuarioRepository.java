package cr.ac.ucr.if0009.edumentor_backend.repository;


import org.springframework.data.jpa.repository.JpaRepository;
import cr.ac.ucr.if0009.edumentor_backend.entity.Usuario;

import java.util.Optional;

public interface UsuarioRepository extends JpaRepository<Usuario, Long> {
    Optional<Usuario> findByEmail(String email);
    boolean existsByEmail(String email);
    boolean existsByCarne(String carne);
}
