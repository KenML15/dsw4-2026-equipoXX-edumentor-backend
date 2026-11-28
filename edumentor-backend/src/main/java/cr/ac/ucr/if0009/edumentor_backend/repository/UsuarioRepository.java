package cr.ac.ucr.if0009.edumentor_backend.repository;


import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import cr.ac.ucr.if0009.edumentor_backend.entity.Rol;
import cr.ac.ucr.if0009.edumentor_backend.entity.Usuario;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface UsuarioRepository extends JpaRepository<Usuario, Long> {
    Optional<Usuario> findByEmail(String email);
    boolean existsByEmail(String email);
    boolean existsByCarne(String carne);
    List<Usuario> findByRolOrderByNombreAscApellidoAsc(Rol rol);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT u FROM Usuario u WHERE u.id IN :ids ORDER BY u.id ASC")
    List<Usuario> bloquearPorIds(@Param("ids") Collection<Long> ids);
}
