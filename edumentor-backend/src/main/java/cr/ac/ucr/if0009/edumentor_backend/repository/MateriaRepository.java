package cr.ac.ucr.if0009.edumentor_backend.repository;

import cr.ac.ucr.if0009.edumentor_backend.entity.Materia;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface MateriaRepository extends JpaRepository<Materia, Long> {

    Optional<Materia> findByCodigoIgnoreCase(String codigo);

    boolean existsByCodigoIgnoreCase(String codigo);

    boolean existsByCodigoIgnoreCaseAndIdNot(String codigo, Long id);

    List<Materia> findAllByOrderByCodigoAsc();

    List<Materia> findByActivaTrueOrderByCodigoAsc();
}
