package cr.ac.ucr.if0009.edumentor_backend.service;

import cr.ac.ucr.if0009.edumentor_backend.dto.DisponibilidadRequest;
import cr.ac.ucr.if0009.edumentor_backend.dto.DisponibilidadResponse;
import cr.ac.ucr.if0009.edumentor_backend.entity.DisponibilidadTutor;
import cr.ac.ucr.if0009.edumentor_backend.entity.Rol;
import cr.ac.ucr.if0009.edumentor_backend.entity.Usuario;
import cr.ac.ucr.if0009.edumentor_backend.exception.RecursoNoEncontradoException;
import cr.ac.ucr.if0009.edumentor_backend.exception.ReglaNegocioException;
import cr.ac.ucr.if0009.edumentor_backend.repository.DisponibilidadTutorRepository;
import cr.ac.ucr.if0009.edumentor_backend.repository.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DisponibilidadServiceTest {

    @Mock
    private DisponibilidadTutorRepository disponibilidadRepository;

    @Mock
    private UsuarioRepository usuarioRepository;

    @InjectMocks
    private DisponibilidadService disponibilidadService;

    private Usuario tutor;

    @BeforeEach
    void setUp() {
        tutor = Usuario.builder()
                .id(2L)
                .carne("B91234")
                .email("tutor@ucr.ac.cr")
                .nombre("Sebastián")
                .apellido("Quirós")
                .rol(Rol.TUTOR)
                .build();
    }

    @Test
    void crear_ConBloqueTraslapado_DebeLanzarReglaNegocioException() {
        DisponibilidadRequest request = new DisponibilidadRequest(
                null,
                DayOfWeek.MONDAY,
                LocalTime.of(9, 0),
                LocalTime.of(11, 0),
                true
        );

        when(usuarioRepository.findByEmail("tutor@ucr.ac.cr")).thenReturn(Optional.of(tutor));
        when(disponibilidadRepository.existeTraslapeActivo(
                2L, DayOfWeek.MONDAY, LocalTime.of(9, 0), LocalTime.of(11, 0), null
        )).thenReturn(true);

        ReglaNegocioException ex = assertThrows(
                ReglaNegocioException.class,
                () -> disponibilidadService.crear(request, "tutor@ucr.ac.cr")
        );

        assertTrue(ex.getMessage().toLowerCase().contains("traslapa"));
        verify(disponibilidadRepository, never()).save(any());
    }

    @Test
    void crear_ConHoraInicioMayorOIgualAHoraFin_DebeLanzarReglaNegocioException() {
        DisponibilidadRequest request = new DisponibilidadRequest(
                null,
                DayOfWeek.TUESDAY,
                LocalTime.of(14, 0),
                LocalTime.of(13, 0),
                true
        );

        when(usuarioRepository.findByEmail("tutor@ucr.ac.cr")).thenReturn(Optional.of(tutor));

        assertThrows(
                ReglaNegocioException.class,
                () -> disponibilidadService.crear(request, "tutor@ucr.ac.cr")
        );

        verify(disponibilidadRepository, never()).existeTraslapeActivo(any(), any(), any(), any(), any());
        verify(disponibilidadRepository, never()).save(any());
    }

    @Test
    void crear_SinTraslape_DebeGuardarYRetornarBloque() {
        DisponibilidadRequest request = new DisponibilidadRequest(
                null,
                DayOfWeek.THURSDAY,
                LocalTime.of(10, 0),
                LocalTime.of(12, 0),
                true
        );

        when(usuarioRepository.findByEmail("tutor@ucr.ac.cr")).thenReturn(Optional.of(tutor));
        when(disponibilidadRepository.existeTraslapeActivo(
                2L, DayOfWeek.THURSDAY, LocalTime.of(10, 0), LocalTime.of(12, 0), null
        )).thenReturn(false);
        when(disponibilidadRepository.save(any(DisponibilidadTutor.class))).thenAnswer(inv -> {
            DisponibilidadTutor saved = inv.getArgument(0);
            saved.setId(10L);
            return saved;
        });

        DisponibilidadResponse response = disponibilidadService.crear(request, "tutor@ucr.ac.cr");

        assertNotNull(response);
        assertEquals(10L, response.id());
        assertEquals(2L, response.tutorId());
        assertEquals(DayOfWeek.THURSDAY, response.diaSemana());
        assertEquals(LocalTime.of(10, 0), response.horaInicio());
        assertEquals(LocalTime.of(12, 0), response.horaFin());
        assertTrue(response.activo());
    }

    @Test
    void cambiarEstado_AlReactivarBloqueConTraslape_DebeLanzarReglaNegocioException() {
        DisponibilidadTutor inactivo = DisponibilidadTutor.builder()
                .id(5L)
                .tutor(tutor)
                .diaSemana(DayOfWeek.MONDAY)
                .horaInicio(LocalTime.of(8, 30))
                .horaFin(LocalTime.of(10, 30))
                .activo(false)
                .build();

        when(usuarioRepository.findByEmail("tutor@ucr.ac.cr")).thenReturn(Optional.of(tutor));
        when(disponibilidadRepository.findByIdWithTutor(5L)).thenReturn(Optional.of(inactivo));
        when(disponibilidadRepository.existeTraslapeActivo(
                2L, DayOfWeek.MONDAY, LocalTime.of(8, 30), LocalTime.of(10, 30), 5L
        )).thenReturn(true);

        assertThrows(
                ReglaNegocioException.class,
                () -> disponibilidadService.cambiarEstado(5L, true, "tutor@ucr.ac.cr")
        );

        verify(disponibilidadRepository, never()).save(any());
    }

    @Test
    void estaDisponible_DentroDeBloqueActivo_DebeRetornarTrue() {
        DisponibilidadTutor bloque = DisponibilidadTutor.builder()
                .id(1L)
                .tutor(tutor)
                .diaSemana(DayOfWeek.MONDAY)
                .horaInicio(LocalTime.of(8, 0))
                .horaFin(LocalTime.of(10, 0))
                .activo(true)
                .build();

        // 2026-11-23 es lunes
        LocalDateTime fechaSesion = LocalDateTime.of(2026, 11, 23, 8, 30);
        when(usuarioRepository.findById(2L)).thenReturn(Optional.of(tutor));
        when(disponibilidadRepository.findActivasByTutorIdAndDiaSemana(2L, DayOfWeek.MONDAY))
                .thenReturn(List.of(bloque));

        assertTrue(disponibilidadService.estaDisponible(2L, fechaSesion, 60));
    }

    @Test
    void estaDisponible_EntreBloquesContiguos_DebeRetornarTrue() {
        DisponibilidadTutor bloque1 = DisponibilidadTutor.builder()
                .id(1L)
                .tutor(tutor)
                .diaSemana(DayOfWeek.MONDAY)
                .horaInicio(LocalTime.of(8, 0))
                .horaFin(LocalTime.of(9, 0))
                .activo(true)
                .build();
        DisponibilidadTutor bloque2 = DisponibilidadTutor.builder()
                .id(2L)
                .tutor(tutor)
                .diaSemana(DayOfWeek.MONDAY)
                .horaInicio(LocalTime.of(9, 0))
                .horaFin(LocalTime.of(10, 0))
                .activo(true)
                .build();

        LocalDateTime fechaSesion = LocalDateTime.of(2026, 11, 23, 8, 30);
        when(usuarioRepository.findById(2L)).thenReturn(Optional.of(tutor));
        when(disponibilidadRepository.findActivasByTutorIdAndDiaSemana(2L, DayOfWeek.MONDAY))
                .thenReturn(List.of(bloque1, bloque2));

        assertTrue(disponibilidadService.estaDisponible(2L, fechaSesion, 60));
    }

    @Test
    void estaDisponible_FueraDeBloqueActivo_DebeRetornarFalse() {
        DisponibilidadTutor bloque = DisponibilidadTutor.builder()
                .id(1L)
                .tutor(tutor)
                .diaSemana(DayOfWeek.MONDAY)
                .horaInicio(LocalTime.of(8, 0))
                .horaFin(LocalTime.of(10, 0))
                .activo(true)
                .build();

        LocalDateTime fechaSesion = LocalDateTime.of(2026, 11, 23, 9, 30);
        when(usuarioRepository.findById(2L)).thenReturn(Optional.of(tutor));
        when(disponibilidadRepository.findActivasByTutorIdAndDiaSemana(2L, DayOfWeek.MONDAY))
                .thenReturn(List.of(bloque));

        assertFalse(disponibilidadService.estaDisponible(2L, fechaSesion, 60));
    }

    @Test
    void estaDisponible_ConUsuarioInexistente_DebeLanzarRecursoNoEncontrado() {
        when(usuarioRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(
                RecursoNoEncontradoException.class,
                () -> disponibilidadService.estaDisponible(99L, LocalDateTime.of(2026, 11, 23, 8, 30), 60)
        );
        verify(disponibilidadRepository, never()).findActivasByTutorIdAndDiaSemana(any(), any());
    }

    @Test
    void estaDisponible_ConUsuarioQueNoEsTutor_DebeLanzarReglaNegocioException() {
        Usuario estudiante = Usuario.builder().id(1L).email("estudiante@ucr.ac.cr").rol(Rol.ESTUDIANTE).build();
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(estudiante));

        assertThrows(
                ReglaNegocioException.class,
                () -> disponibilidadService.estaDisponible(1L, LocalDateTime.of(2026, 11, 23, 8, 30), 60)
        );
        verify(disponibilidadRepository, never()).findActivasByTutorIdAndDiaSemana(any(), any());
    }
}
