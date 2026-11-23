package cr.ac.ucr.if0009.edumentor_backend.service;

import cr.ac.ucr.if0009.edumentor_backend.dto.MateriaRequest;
import cr.ac.ucr.if0009.edumentor_backend.dto.MateriaResponse;
import cr.ac.ucr.if0009.edumentor_backend.entity.Materia;
import cr.ac.ucr.if0009.edumentor_backend.exception.RecursoNoEncontradoException;
import cr.ac.ucr.if0009.edumentor_backend.exception.ReglaNegocioException;
import cr.ac.ucr.if0009.edumentor_backend.repository.MateriaRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MateriaServiceTest {

    @Mock
    private MateriaRepository materiaRepository;

    @InjectMocks
    private MateriaService materiaService;

    @Test
    void crear_ConCodigoDuplicado_DebeLanzarReglaNegocioException() {
        MateriaRequest request = new MateriaRequest("if0009", "Desarrollo de Software IV", "Desc", 4, true);
        when(materiaRepository.existsByCodigoIgnoreCase("IF0009")).thenReturn(true);

        assertThrows(ReglaNegocioException.class, () -> materiaService.crear(request));
        verify(materiaRepository, never()).save(any());
    }

    @Test
    void crear_ConDatosValidos_DebeNormalizarCodigoYGuardar() {
        MateriaRequest request = new MateriaRequest(" if0010 ", "Ingeniería de Software", "Descripción", 4, true);
        when(materiaRepository.existsByCodigoIgnoreCase("IF0010")).thenReturn(false);
        when(materiaRepository.save(any(Materia.class))).thenAnswer(inv -> {
            Materia m = inv.getArgument(0);
            m.setId(1L);
            return m;
        });

        MateriaResponse response = materiaService.crear(request);

        assertEquals(1L, response.id());
        assertEquals("IF0010", response.codigo());
        assertEquals("Ingeniería de Software", response.nombre());
        assertEquals(4, response.creditos());
        assertTrue(response.activa());
    }

    @Test
    void obtenerPorId_NoExistente_DebeLanzarRecursoNoEncontradoException() {
        when(materiaRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(RecursoNoEncontradoException.class, () -> materiaService.obtenerPorId(99L));
    }
}
