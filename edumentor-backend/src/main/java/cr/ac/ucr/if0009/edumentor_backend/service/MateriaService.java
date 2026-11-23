package cr.ac.ucr.if0009.edumentor_backend.service;

import cr.ac.ucr.if0009.edumentor_backend.dto.MateriaRequest;
import cr.ac.ucr.if0009.edumentor_backend.dto.MateriaResponse;
import cr.ac.ucr.if0009.edumentor_backend.entity.Materia;
import cr.ac.ucr.if0009.edumentor_backend.exception.RecursoNoEncontradoException;
import cr.ac.ucr.if0009.edumentor_backend.exception.ReglaNegocioException;
import cr.ac.ucr.if0009.edumentor_backend.repository.MateriaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class MateriaService {

    private final MateriaRepository materiaRepository;

    @Transactional(readOnly = true)
    public List<MateriaResponse> listar(boolean soloActivas) {
        List<Materia> materias = soloActivas
                ? materiaRepository.findByActivaTrueOrderByCodigoAsc()
                : materiaRepository.findAllByOrderByCodigoAsc();
        return materias.stream().map(this::mapToResponse).toList();
    }

    @Transactional(readOnly = true)
    public MateriaResponse obtenerPorId(Long id) {
        Materia materia = materiaRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Materia no encontrada con id: " + id));
        return mapToResponse(materia);
    }

    @Transactional
    public MateriaResponse crear(MateriaRequest request) {
        String codigoNormalizado = normalizarCodigo(request.codigo());
        if (materiaRepository.existsByCodigoIgnoreCase(codigoNormalizado)) {
            throw new ReglaNegocioException("Ya existe una materia registrada con el código: " + codigoNormalizado);
        }

        Materia materia = Materia.builder()
                .codigo(codigoNormalizado)
                .nombre(request.nombre().trim())
                .descripcion(request.descripcion() != null ? request.descripcion().trim() : null)
                .creditos(request.creditos())
                .activa(request.activa() != null ? request.activa() : true)
                .build();

        return mapToResponse(materiaRepository.save(materia));
    }

    @Transactional
    public MateriaResponse actualizar(Long id, MateriaRequest request) {
        Materia materia = materiaRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Materia no encontrada con id: " + id));

        String codigoNormalizado = normalizarCodigo(request.codigo());
        if (materiaRepository.existsByCodigoIgnoreCaseAndIdNot(codigoNormalizado, id)) {
            throw new ReglaNegocioException("Ya existe otra materia registrada con el código: " + codigoNormalizado);
        }

        materia.setCodigo(codigoNormalizado);
        materia.setNombre(request.nombre().trim());
        materia.setDescripcion(request.descripcion() != null ? request.descripcion().trim() : null);
        materia.setCreditos(request.creditos());
        if (request.activa() != null) {
            materia.setActiva(request.activa());
        }

        return mapToResponse(materiaRepository.save(materia));
    }

    @Transactional
    public void eliminar(Long id) {
        Materia materia = materiaRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Materia no encontrada con id: " + id));
        materiaRepository.delete(materia);
    }

    private String normalizarCodigo(String codigo) {
        return codigo.trim().toUpperCase();
    }

    private MateriaResponse mapToResponse(Materia materia) {
        return new MateriaResponse(
                materia.getId(),
                materia.getCodigo(),
                materia.getNombre(),
                materia.getDescripcion(),
                materia.getCreditos(),
                materia.getActiva()
        );
    }
}
