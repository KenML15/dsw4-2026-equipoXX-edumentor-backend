package cr.ac.ucr.if0009.edumentor_backend.service;

import cr.ac.ucr.if0009.edumentor_backend.config.SesionesProperties;
import cr.ac.ucr.if0009.edumentor_backend.dto.SesionRequest;
import cr.ac.ucr.if0009.edumentor_backend.dto.SesionResponse;
import cr.ac.ucr.if0009.edumentor_backend.entity.EstadoSesion;
import cr.ac.ucr.if0009.edumentor_backend.entity.Materia;
import cr.ac.ucr.if0009.edumentor_backend.entity.ModalidadSesion;
import cr.ac.ucr.if0009.edumentor_backend.entity.Rol;
import cr.ac.ucr.if0009.edumentor_backend.entity.SesionTutoria;
import cr.ac.ucr.if0009.edumentor_backend.entity.Usuario;
import cr.ac.ucr.if0009.edumentor_backend.exception.CodigoErrorSesion;
import cr.ac.ucr.if0009.edumentor_backend.exception.RecursoNoEncontradoException;
import cr.ac.ucr.if0009.edumentor_backend.exception.ReglaNegocioException;
import cr.ac.ucr.if0009.edumentor_backend.repository.MateriaRepository;
import cr.ac.ucr.if0009.edumentor_backend.repository.SesionTutoriaRepository;
import cr.ac.ucr.if0009.edumentor_backend.repository.UsuarioRepository;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SesionServiceTest {

    private static final ZoneId ZONA = ZoneId.of("America/Costa_Rica");
    private static final LocalDateTime AHORA = LocalDateTime.of(2026, 11, 23, 10, 0);
    private static final LocalDateTime INICIO_SESION = LocalDateTime.of(2026, 11, 30, 8, 0);
    private static final String EMAIL_ESTUDIANTE = "estudiante@ucr.ac.cr";
    private static final String EMAIL_TUTOR = "tutor@ucr.ac.cr";
    private static final String EMAIL_COORDINADOR = "coordinador@ucr.ac.cr";
    private static final String EMAIL_OTRO_ESTUDIANTE = "otro@ucr.ac.cr";
    private static final String EMAIL_OTRO_TUTOR = "otrotutor@ucr.ac.cr";

    @Mock
    private SesionTutoriaRepository sesionRepository;

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private MateriaRepository materiaRepository;

    @Mock
    private DisponibilidadService disponibilidadService;

    private SesionService sesionService;

    private Usuario estudiante;
    private Usuario otroEstudiante;
    private Usuario tutor;
    private Usuario otroTutor;
    private Usuario coordinador;
    private Materia materia;

    @BeforeEach
    void setUp() {
        Clock reloj = Clock.fixed(AHORA.atZone(ZONA).toInstant(), ZONA);
        sesionService = new SesionService(
                sesionRepository, usuarioRepository, materiaRepository, disponibilidadService,
                reloj, SesionesProperties.porDefecto()
        );

        estudiante = usuario(1L, EMAIL_ESTUDIANTE, "Kenneth", Rol.ESTUDIANTE);
        tutor = usuario(2L, EMAIL_TUTOR, "Sebastián", Rol.TUTOR);
        coordinador = usuario(3L, EMAIL_COORDINADOR, "Jonathan", Rol.COORDINADOR);
        otroEstudiante = usuario(4L, EMAIL_OTRO_ESTUDIANTE, "Ana", Rol.ESTUDIANTE);
        otroTutor = usuario(5L, EMAIL_OTRO_TUTOR, "Laura", Rol.TUTOR);
        materia = Materia.builder()
                .id(1L).codigo("IF0009").nombre("Desarrollo de Software IV").creditos(4).activa(true).build();
    }

    private static Usuario usuario(Long id, String email, String nombre, Rol rol) {
        return Usuario.builder()
                .id(id).carne("C" + id).email(email).nombre(nombre).apellido("Prueba").rol(rol).build();
    }

    private void autenticado(Usuario usuario) {
        when(usuarioRepository.findByEmail(usuario.getEmail())).thenReturn(Optional.of(usuario));
    }

    private SesionRequest solicitud(int duracion, ModalidadSesion modalidad, String enlace) {
        return new SesionRequest(2L, 1L, INICIO_SESION, duracion, modalidad, enlace);
    }

    private SesionRequest solicitudValida() {
        return solicitud(60, ModalidadSesion.VIRTUAL, "https://meet.google.com/abc-defg-hij");
    }

    private void prepararSolicitud(int duracion) {
        autenticado(estudiante);
        when(usuarioRepository.findById(2L)).thenReturn(Optional.of(tutor));
        when(materiaRepository.findById(1L)).thenReturn(Optional.of(materia));
        when(disponibilidadService.estaDisponible(2L, INICIO_SESION, duracion)).thenReturn(true);
    }

    private void sinTraslapes() {
        when(sesionRepository.existeTraslapeTutor(eq(2L), anyCollection(), any(), any(), isNull())).thenReturn(false);
        when(sesionRepository.existeTraslapeEstudiante(eq(1L), anyCollection(), any(), any(), isNull())).thenReturn(false);
    }

    private void guardarDevuelveArgumento() {
        when(sesionRepository.save(any(SesionTutoria.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    private SesionTutoria sesion(EstadoSesion estado, LocalDateTime inicio) {
        return SesionTutoria.builder()
                .id(10L)
                .estudiante(estudiante)
                .tutor(tutor)
                .materia(materia)
                .fechaHora(inicio)
                .fechaHoraFin(inicio.plusMinutes(60))
                .duracionMinutos(60)
                .modalidad(ModalidadSesion.PRESENCIAL)
                .enlaceOUbicacion("Aula 201")
                .estado(estado)
                .fechaCreacion(AHORA.minusDays(1))
                .build();
    }

    private SesionTutoria existente(EstadoSesion estado, LocalDateTime inicio) {
        SesionTutoria s = sesion(estado, inicio);
        when(sesionRepository.bloquearPorId(10L)).thenReturn(Optional.of(s));
        when(sesionRepository.findByIdConDetalle(10L)).thenReturn(Optional.of(s));
        return s;
    }

    @Nested
    class Solicitar {

        @Test
        void reservaValida_DebeCrearSesionEnEstadoRequested() {
            prepararSolicitud(60);
            sinTraslapes();
            when(sesionRepository.save(any(SesionTutoria.class))).thenAnswer(inv -> {
                SesionTutoria s = inv.getArgument(0);
                s.setId(99L);
                return s;
            });

            SesionResponse response = sesionService.solicitar(solicitudValida(), EMAIL_ESTUDIANTE);

            assertEquals(99L, response.id());
            assertEquals(EstadoSesion.REQUESTED, response.estado());
            assertEquals(INICIO_SESION.plusMinutes(60), response.fechaHoraFin());
            assertEquals(AHORA, response.fechaCreacion());
            assertEquals(INICIO_SESION, response.cancelableHasta());
            assertEquals(INICIO_SESION.minusMinutes(15), response.iniciableDesde());
            assertEquals(INICIO_SESION.plusMinutes(45), response.finalizableDesde());
        }

        @ParameterizedTest
        @ValueSource(ints = {30, 240})
        void duracionesLimite_DebenAceptarse(int duracion) {
            prepararSolicitud(duracion);
            sinTraslapes();
            guardarDevuelveArgumento();

            SesionResponse response = sesionService.solicitar(
                    solicitud(duracion, ModalidadSesion.PRESENCIAL, "Aula 201"), EMAIL_ESTUDIANTE
            );

            assertEquals(duracion, response.duracionMinutos());
            assertEquals(INICIO_SESION.plusMinutes(duracion), response.fechaHoraFin());
        }

        @ParameterizedTest
        @ValueSource(ints = {0, 29, 241, 600})
        void duracionesInvalidas_DebenFallarValidacionDelDto(int duracion) {
            Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

            var violaciones = validator.validate(solicitud(duracion, ModalidadSesion.VIRTUAL, "https://meet.test/a"));

            assertTrue(violaciones.stream().anyMatch(v -> v.getPropertyPath().toString().equals("duracionMinutos")));
        }

        @Test
        void conTraslapeDelTutor_DebeRechazarReserva() {
            prepararSolicitud(60);
            when(sesionRepository.existeTraslapeTutor(
                    2L, SesionService.ESTADOS_QUE_OCUPAN_HORARIO, INICIO_SESION, INICIO_SESION.plusMinutes(60), null
            )).thenReturn(true);

            ReglaNegocioException ex = assertThrows(ReglaNegocioException.class,
                    () -> sesionService.solicitar(solicitudValida(), EMAIL_ESTUDIANTE));

            assertTrue(ex.getMessage().contains("traslapa"));
            verify(sesionRepository, never()).save(any());
        }

        @Test
        void conTraslapeDelEstudiante_DebeRechazarReserva() {
            prepararSolicitud(60);
            when(sesionRepository.existeTraslapeTutor(eq(2L), anyCollection(), any(), any(), isNull())).thenReturn(false);
            when(sesionRepository.existeTraslapeEstudiante(eq(1L), anyCollection(), any(), any(), isNull())).thenReturn(true);

            assertThrows(ReglaNegocioException.class,
                    () -> sesionService.solicitar(solicitudValida(), EMAIL_ESTUDIANTE));
            verify(sesionRepository, never()).save(any());
        }

        @Test
        void sinDisponibilidadDelTutor_DebeRechazarReserva() {
            prepararSolicitud(60);
            when(disponibilidadService.estaDisponible(2L, INICIO_SESION, 60)).thenReturn(false);

            assertThrows(ReglaNegocioException.class,
                    () -> sesionService.solicitar(solicitudValida(), EMAIL_ESTUDIANTE));
            verify(sesionRepository, never()).save(any());
        }

        @Test
        void comoTutor_DebeLanzarAccessDenied() {
            autenticado(tutor);

            assertThrows(AccessDeniedException.class,
                    () -> sesionService.solicitar(solicitudValida(), EMAIL_TUTOR));
            verify(sesionRepository, never()).save(any());
        }

        @Test
        void materiaInactiva_DebeRechazarReserva() {
            autenticado(estudiante);
            when(usuarioRepository.findById(2L)).thenReturn(Optional.of(tutor));
            materia.setActiva(false);
            when(materiaRepository.findById(1L)).thenReturn(Optional.of(materia));

            ReglaNegocioException ex = assertThrows(ReglaNegocioException.class,
                    () -> sesionService.solicitar(solicitudValida(), EMAIL_ESTUDIANTE));

            assertTrue(ex.getMessage().contains("no está activa"));
        }

        @Test
        void tutorInexistente_DebeLanzarRecursoNoEncontrado() {
            autenticado(estudiante);
            when(usuarioRepository.findById(2L)).thenReturn(Optional.empty());

            assertThrows(RecursoNoEncontradoException.class,
                    () -> sesionService.solicitar(solicitudValida(), EMAIL_ESTUDIANTE));
        }

        @Test
        void tutorConRolIncorrecto_DebeRechazarReserva() {
            autenticado(estudiante);
            when(usuarioRepository.findById(2L)).thenReturn(Optional.of(otroEstudiante));

            ReglaNegocioException ex = assertThrows(ReglaNegocioException.class,
                    () -> sesionService.solicitar(solicitudValida(), EMAIL_ESTUDIANTE));

            assertTrue(ex.getMessage().contains("TUTOR"));
        }

        @Test
        void fechaPasada_DebeRechazarReserva() {
            autenticado(estudiante);
            when(usuarioRepository.findById(2L)).thenReturn(Optional.of(tutor));
            when(materiaRepository.findById(1L)).thenReturn(Optional.of(materia));
            SesionRequest pasada = new SesionRequest(2L, 1L, AHORA.minusHours(1), 60, ModalidadSesion.PRESENCIAL, "Aula 201");

            assertThrows(ReglaNegocioException.class, () -> sesionService.solicitar(pasada, EMAIL_ESTUDIANTE));
            verify(disponibilidadService, never()).estaDisponible(any(), any(), anyInt());
        }
    }

    @Nested
    class DuracionYCodigosDeError {

        @ParameterizedTest
        @ValueSource(ints = {45, 75, 105, 31, 239})
        void duracionQueNoEsMultiploDe30_DebeRechazarseConCodigo(int duracion) {
            ReglaNegocioException ex = assertThrows(ReglaNegocioException.class,
                    () -> sesionService.validarDuracion(duracion));

            assertEquals(CodigoErrorSesion.DURACION_INVALIDA, ex.getCodigo());
            assertEquals(30, ex.getParametros().get("paso"));
        }

        @ParameterizedTest
        @ValueSource(ints = {30, 60, 90, 120, 150, 180, 210, 240})
        void duracionMultiploDe30_DebeAceptarse(int duracion) {
            assertDoesNotThrow(() -> sesionService.validarDuracion(duracion));
        }

        @Test
        void solicitarCon45Minutos_NoDebeConsultarDisponibilidadNiGuardar() {
            autenticado(estudiante);
            when(usuarioRepository.findById(2L)).thenReturn(Optional.of(tutor));
            when(materiaRepository.findById(1L)).thenReturn(Optional.of(materia));

            ReglaNegocioException ex = assertThrows(ReglaNegocioException.class,
                    () -> sesionService.solicitar(solicitud(45, ModalidadSesion.PRESENCIAL, "Aula 201"), EMAIL_ESTUDIANTE));

            assertEquals(CodigoErrorSesion.DURACION_INVALIDA, ex.getCodigo());
            verify(disponibilidadService, never()).estaDisponible(any(), any(), anyInt());
            verify(sesionRepository, never()).save(any());
        }

        @Test
        void traslapeDelTutor_DebeIncluirCodigo() {
            prepararSolicitud(60);
            when(sesionRepository.existeTraslapeTutor(eq(2L), anyCollection(), any(), any(), isNull())).thenReturn(true);

            ReglaNegocioException ex = assertThrows(ReglaNegocioException.class,
                    () -> sesionService.solicitar(solicitudValida(), EMAIL_ESTUDIANTE));

            assertEquals(CodigoErrorSesion.TRASLAPE_TUTOR, ex.getCodigo());
        }

        @Test
        void inicioAnticipado_DebeIncluirCodigoYMomentoPermitido() {
            existente(EstadoSesion.CONFIRMED, INICIO_SESION);
            autenticado(tutor);

            ReglaNegocioException ex = assertThrows(ReglaNegocioException.class,
                    () -> sesionService.iniciar(10L, EMAIL_TUTOR));

            assertEquals(CodigoErrorSesion.INICIO_ANTICIPADO, ex.getCodigo());
            assertEquals(INICIO_SESION.minusMinutes(15).toString(), ex.getParametros().get("desde"));
        }

        @Test
        void transicionInvalida_DebeIncluirAccionYEstado() {
            existente(EstadoSesion.CANCELLED, INICIO_SESION);
            autenticado(tutor);

            ReglaNegocioException ex = assertThrows(ReglaNegocioException.class,
                    () -> sesionService.aceptar(10L, EMAIL_TUTOR));

            assertEquals(CodigoErrorSesion.TRANSICION_INVALIDA, ex.getCodigo());
            assertEquals("aceptar", ex.getParametros().get("accion"));
            assertEquals("CANCELLED", ex.getParametros().get("estado"));
        }

        @Test
        void cancelacionTardia_DebeIncluirCodigoYHoras() {
            existente(EstadoSesion.CONFIRMED, AHORA.plusMinutes(30));
            autenticado(estudiante);

            ReglaNegocioException ex = assertThrows(ReglaNegocioException.class,
                    () -> sesionService.cancelar(10L, EMAIL_ESTUDIANTE));

            assertEquals(CodigoErrorSesion.CANCELACION_TARDIA, ex.getCodigo());
            assertEquals(2L, ex.getParametros().get("horas"));
        }
    }

    @Nested
    class CancelableHastaSegunUsuario {

        @Test
        void estudianteConSolicitudPendiente_PuedeHastaLaHoraDeInicio() {
            assertEquals(INICIO_SESION, sesionService.cancelableHasta(sesion(EstadoSesion.REQUESTED, INICIO_SESION), estudiante));
        }

        @Test
        void tutorConSolicitudPendiente_NoPuedeCancelar() {
            assertNull(sesionService.cancelableHasta(sesion(EstadoSesion.REQUESTED, INICIO_SESION), tutor));
        }

        @Test
        void participantesConSesionConfirmada_PuedenHastaDosHorasAntes() {
            SesionTutoria confirmada = sesion(EstadoSesion.CONFIRMED, INICIO_SESION);

            assertEquals(INICIO_SESION.minusHours(2), sesionService.cancelableHasta(confirmada, estudiante));
            assertEquals(INICIO_SESION.minusHours(2), sesionService.cancelableHasta(confirmada, tutor));
        }

        @Test
        void coordinador_PuedeHastaLaHoraDeInicio() {
            assertEquals(INICIO_SESION, sesionService.cancelableHasta(sesion(EstadoSesion.CONFIRMED, INICIO_SESION), coordinador));
            assertEquals(INICIO_SESION, sesionService.cancelableHasta(sesion(EstadoSesion.REQUESTED, INICIO_SESION), coordinador));
        }

        @Test
        void usuarioAjenoOEstadoFinal_NoPuedeCancelar() {
            assertNull(sesionService.cancelableHasta(sesion(EstadoSesion.CONFIRMED, INICIO_SESION), otroEstudiante));
            assertNull(sesionService.cancelableHasta(sesion(EstadoSesion.IN_SESSION, INICIO_SESION), coordinador));
            assertNull(sesionService.cancelableHasta(sesion(EstadoSesion.COMPLETED, INICIO_SESION), estudiante));
        }

        @Test
        void respuestaParaCoordinador_UsaSuPropioLimite() {
            when(sesionRepository.findByIdConDetalle(10L)).thenReturn(Optional.of(sesion(EstadoSesion.CONFIRMED, INICIO_SESION)));
            autenticado(coordinador);

            assertEquals(INICIO_SESION, sesionService.obtenerPorId(10L, EMAIL_COORDINADOR).cancelableHasta());
        }
    }

    @Nested
    class ModalidadYEnlace {

        @ParameterizedTest
        @ValueSource(strings = {"", "   ", "meet.google.com/abc", "http://meet.google.com/abc",
                "javascript:alert(1)", "ftp://archivos.ucr.ac.cr/a", "https://", "https://meet google.com",
                "https://usuario:clave@meet.google.com/abc"})
        void virtualConEnlaceInvalido_DebeRechazarse(String enlace) {
            assertThrows(ReglaNegocioException.class,
                    () -> sesionService.validarEnlaceOUbicacion(ModalidadSesion.VIRTUAL, enlace));
        }

        @Test
        void virtualSinEnlace_DebeRechazarse() {
            assertThrows(ReglaNegocioException.class,
                    () -> sesionService.validarEnlaceOUbicacion(ModalidadSesion.VIRTUAL, null));
        }

        @Test
        void virtualConHttps_DebeAceptarseRecortado() {
            assertEquals("https://meet.google.com/abc",
                    sesionService.validarEnlaceOUbicacion(ModalidadSesion.VIRTUAL, "  https://meet.google.com/abc "));
        }

        @ParameterizedTest
        @ValueSource(strings = {"", "  ", "A1"})
        void presencialSinUbicacion_DebeRechazarse(String ubicacion) {
            assertThrows(ReglaNegocioException.class,
                    () -> sesionService.validarEnlaceOUbicacion(ModalidadSesion.PRESENCIAL, ubicacion));
        }

        @Test
        void presencialConUbicacion_DebeAceptarse() {
            assertEquals("Aula 201",
                    sesionService.validarEnlaceOUbicacion(ModalidadSesion.PRESENCIAL, " Aula 201 "));
        }

        @Test
        void enlaceDemasiadoLargo_DebeRechazarse() {
            String largo = "https://meet.google.com/" + "a".repeat(480);

            assertThrows(ReglaNegocioException.class,
                    () -> sesionService.validarEnlaceOUbicacion(ModalidadSesion.VIRTUAL, largo));
        }

        @Test
        void solicitarVirtualSinEnlace_NoDebeGuardar() {
            autenticado(estudiante);
            when(usuarioRepository.findById(2L)).thenReturn(Optional.of(tutor));
            when(materiaRepository.findById(1L)).thenReturn(Optional.of(materia));

            assertThrows(ReglaNegocioException.class,
                    () -> sesionService.solicitar(solicitud(60, ModalidadSesion.VIRTUAL, null), EMAIL_ESTUDIANTE));
            verify(sesionRepository, never()).save(any());
        }
    }

    @Nested
    class Aceptar {

        @Test
        void sinTraslape_DebePasarAConfirmed() {
            existente(EstadoSesion.REQUESTED, INICIO_SESION);
            autenticado(tutor);
            when(sesionRepository.existeTraslapeTutor(eq(2L), anyCollection(), any(), any(), eq(10L))).thenReturn(false);
            when(sesionRepository.existeTraslapeEstudiante(eq(1L), anyCollection(), any(), any(), eq(10L))).thenReturn(false);
            when(sesionRepository.saveAndFlush(any(SesionTutoria.class))).thenAnswer(inv -> inv.getArgument(0));

            SesionResponse response = sesionService.aceptar(10L, EMAIL_TUTOR);

            assertEquals(EstadoSesion.CONFIRMED, response.estado());
            assertEquals(INICIO_SESION.minusHours(2), response.cancelableHasta());
            verify(usuarioRepository).bloquearPorIds(List.of(1L, 2L));
        }

        @Test
        void conTraslapeAlRevalidar_DebePasarARejected() {
            existente(EstadoSesion.REQUESTED, INICIO_SESION);
            autenticado(tutor);
            when(sesionRepository.existeTraslapeTutor(eq(2L), anyCollection(), any(), any(), eq(10L))).thenReturn(true);
            when(sesionRepository.saveAndFlush(any(SesionTutoria.class))).thenAnswer(inv -> inv.getArgument(0));

            assertEquals(EstadoSesion.REJECTED, sesionService.aceptar(10L, EMAIL_TUTOR).estado());
        }

        @Test
        void dobleAceptacion_DebeLanzarReglaNegocioException() {
            existente(EstadoSesion.CONFIRMED, INICIO_SESION);
            autenticado(tutor);

            ReglaNegocioException ex = assertThrows(ReglaNegocioException.class,
                    () -> sesionService.aceptar(10L, EMAIL_TUTOR));

            assertTrue(ex.getMessage().contains("CONFIRMED"));
            verify(sesionRepository, never()).saveAndFlush(any());
        }

        @Test
        void tutorDeOtraSesion_DebeLanzarAccessDenied() {
            existente(EstadoSesion.REQUESTED, INICIO_SESION);
            autenticado(otroTutor);

            assertThrows(AccessDeniedException.class, () -> sesionService.aceptar(10L, EMAIL_OTRO_TUTOR));
            verify(sesionRepository, never()).saveAndFlush(any());
        }

        @Test
        void sesionCuyaHoraYaPaso_DebeLanzarReglaNegocioException() {
            existente(EstadoSesion.REQUESTED, AHORA.minusMinutes(5));
            autenticado(tutor);

            assertThrows(ReglaNegocioException.class, () -> sesionService.aceptar(10L, EMAIL_TUTOR));
        }
    }

    @Nested
    class Rechazar {

        @Test
        void requested_DebePasarARejected() {
            existente(EstadoSesion.REQUESTED, INICIO_SESION);
            autenticado(tutor);
            guardarDevuelveArgumento();

            assertEquals(EstadoSesion.REJECTED, sesionService.rechazar(10L, EMAIL_TUTOR).estado());
        }

        @Test
        void confirmed_DebeLanzarReglaNegocioException() {
            existente(EstadoSesion.CONFIRMED, INICIO_SESION);
            autenticado(tutor);

            assertThrows(ReglaNegocioException.class, () -> sesionService.rechazar(10L, EMAIL_TUTOR));
        }
    }

    @Nested
    class Cancelar {

        @Test
        void confirmadaConMenosDeDosHoras_DebeLanzarReglaNegocioException() {
            SesionTutoria s = existente(EstadoSesion.CONFIRMED, AHORA.plusMinutes(119));
            autenticado(estudiante);

            ReglaNegocioException ex = assertThrows(ReglaNegocioException.class,
                    () -> sesionService.cancelar(10L, EMAIL_ESTUDIANTE));

            assertTrue(ex.getMessage().contains("2 horas"));
            assertEquals(EstadoSesion.CONFIRMED, s.getEstado());
            verify(sesionRepository, never()).save(any());
        }

        @Test
        void confirmadaConExactamenteDosHoras_DebeCancelar() {
            existente(EstadoSesion.CONFIRMED, AHORA.plusHours(2));
            autenticado(estudiante);
            guardarDevuelveArgumento();

            assertEquals(EstadoSesion.CANCELLED, sesionService.cancelar(10L, EMAIL_ESTUDIANTE).estado());
        }

        @Test
        void confirmadaPorTutorConAnticipacion_DebeCancelar() {
            existente(EstadoSesion.CONFIRMED, INICIO_SESION);
            autenticado(tutor);
            guardarDevuelveArgumento();

            assertEquals(EstadoSesion.CANCELLED, sesionService.cancelar(10L, EMAIL_TUTOR).estado());
        }

        @Test
        void requestedPorEstudianteSinRestriccionDeDosHoras_DebeCancelar() {
            existente(EstadoSesion.REQUESTED, AHORA.plusMinutes(30));
            autenticado(estudiante);
            guardarDevuelveArgumento();

            assertEquals(EstadoSesion.CANCELLED, sesionService.cancelar(10L, EMAIL_ESTUDIANTE).estado());
        }

        @Test
        void requestedPorTutor_DebeRechazarseYSugerirRechazar() {
            existente(EstadoSesion.REQUESTED, INICIO_SESION);
            autenticado(tutor);

            ReglaNegocioException ex = assertThrows(ReglaNegocioException.class,
                    () -> sesionService.cancelar(10L, EMAIL_TUTOR));

            assertTrue(ex.getMessage().contains("rechazar"));
        }

        @Test
        void requestedCuyaHoraYaPaso_DebeLanzarReglaNegocioException() {
            existente(EstadoSesion.REQUESTED, AHORA.minusMinutes(1));
            autenticado(estudiante);

            assertThrows(ReglaNegocioException.class, () -> sesionService.cancelar(10L, EMAIL_ESTUDIANTE));
        }

        @Test
        void usuarioNoParticipante_DebeLanzarAccessDenied() {
            existente(EstadoSesion.CONFIRMED, INICIO_SESION);
            autenticado(otroEstudiante);

            assertThrows(AccessDeniedException.class, () -> sesionService.cancelar(10L, EMAIL_OTRO_ESTUDIANTE));
            verify(sesionRepository, never()).save(any());
        }

        @Test
        void coordinadorPuedeCancelarConfirmadaSinAnticipacionMinima() {
            existente(EstadoSesion.CONFIRMED, AHORA.plusMinutes(30));
            autenticado(coordinador);
            guardarDevuelveArgumento();

            assertEquals(EstadoSesion.CANCELLED, sesionService.cancelar(10L, EMAIL_COORDINADOR).estado());
        }

        @Test
        void enCurso_DebeLanzarReglaNegocioException() {
            existente(EstadoSesion.IN_SESSION, AHORA.minusMinutes(10));
            autenticado(estudiante);

            assertThrows(ReglaNegocioException.class, () -> sesionService.cancelar(10L, EMAIL_ESTUDIANTE));
        }
    }

    @Nested
    class Iniciar {

        @Test
        void variosDiasAntes_DebeLanzarReglaNegocioException() {
            existente(EstadoSesion.CONFIRMED, AHORA.plusDays(3));
            autenticado(tutor);

            ReglaNegocioException ex = assertThrows(ReglaNegocioException.class,
                    () -> sesionService.iniciar(10L, EMAIL_TUTOR));

            assertTrue(ex.getMessage().contains("todavía no puede iniciarse"));
            verify(sesionRepository, never()).save(any());
        }

        @Test
        void unMinutoAntesDeLaTolerancia_DebeLanzarReglaNegocioException() {
            existente(EstadoSesion.CONFIRMED, AHORA.plusMinutes(16));
            autenticado(tutor);

            assertThrows(ReglaNegocioException.class, () -> sesionService.iniciar(10L, EMAIL_TUTOR));
        }

        @Test
        void dentroDeLaTolerancia_DebePasarAInSession() {
            existente(EstadoSesion.CONFIRMED, AHORA.plusMinutes(15));
            autenticado(tutor);
            guardarDevuelveArgumento();

            assertEquals(EstadoSesion.IN_SESSION, sesionService.iniciar(10L, EMAIL_TUTOR).estado());
        }

        @Test
        void despuesDeLaHoraFinal_DebeLanzarReglaNegocioException() {
            existente(EstadoSesion.CONFIRMED, AHORA.minusMinutes(60));
            autenticado(tutor);

            ReglaNegocioException ex = assertThrows(ReglaNegocioException.class,
                    () -> sesionService.iniciar(10L, EMAIL_TUTOR));

            assertTrue(ex.getMessage().contains("terminó"));
        }

        @Test
        void sesionRequested_DebeLanzarReglaNegocioException() {
            existente(EstadoSesion.REQUESTED, AHORA);
            autenticado(tutor);

            assertThrows(ReglaNegocioException.class, () -> sesionService.iniciar(10L, EMAIL_TUTOR));
        }
    }

    @Nested
    class Finalizar {

        @Test
        void justoDespuesDeIniciar_DebeLanzarReglaNegocioException() {
            existente(EstadoSesion.IN_SESSION, AHORA.minusMinutes(1));
            autenticado(tutor);

            ReglaNegocioException ex = assertThrows(ReglaNegocioException.class,
                    () -> sesionService.finalizar(10L, EMAIL_TUTOR));

            assertTrue(ex.getMessage().contains("todavía no puede finalizarse"));
        }

        @Test
        void dentroDeLaToleranciaFinal_DebePasarACompleted() {
            existente(EstadoSesion.IN_SESSION, AHORA.minusMinutes(45));
            autenticado(tutor);
            guardarDevuelveArgumento();

            assertEquals(EstadoSesion.COMPLETED, sesionService.finalizar(10L, EMAIL_TUTOR).estado());
        }

        @Test
        void sesionRequested_DebeLanzarReglaNegocioException() {
            existente(EstadoSesion.REQUESTED, INICIO_SESION);
            autenticado(tutor);

            assertThrows(ReglaNegocioException.class, () -> sesionService.finalizar(10L, EMAIL_TUTOR));
            verify(sesionRepository, never()).save(any());
        }

        @Test
        void flujoCompletoDentroDelHorario_DebeLlegarACompleted() {
            SesionTutoria s = existente(EstadoSesion.CONFIRMED, AHORA.minusMinutes(50));
            autenticado(tutor);
            guardarDevuelveArgumento();

            sesionService.iniciar(10L, EMAIL_TUTOR);
            sesionService.finalizar(10L, EMAIL_TUTOR);

            assertEquals(EstadoSesion.COMPLETED, s.getEstado());
        }
    }

    @Nested
    class Consultar {

        @Test
        void coordinadorPuedeVerCualquierSesion() {
            when(sesionRepository.findByIdConDetalle(10L)).thenReturn(Optional.of(sesion(EstadoSesion.CONFIRMED, INICIO_SESION)));
            autenticado(coordinador);

            assertEquals(10L, sesionService.obtenerPorId(10L, EMAIL_COORDINADOR).id());
        }

        @Test
        void estudianteAjeno_DebeLanzarAccessDenied() {
            when(sesionRepository.findByIdConDetalle(10L)).thenReturn(Optional.of(sesion(EstadoSesion.CONFIRMED, INICIO_SESION)));
            autenticado(otroEstudiante);

            assertThrows(AccessDeniedException.class, () -> sesionService.obtenerPorId(10L, EMAIL_OTRO_ESTUDIANTE));
        }

        @Test
        void sesionInexistente_DebeLanzarRecursoNoEncontrado() {
            when(sesionRepository.findByIdConDetalle(77L)).thenReturn(Optional.empty());
            autenticado(coordinador);

            assertThrows(RecursoNoEncontradoException.class, () -> sesionService.obtenerPorId(77L, EMAIL_COORDINADOR));
        }

        @Test
        void listarMiasComoCoordinador_DebeConsultarTodas() {
            autenticado(coordinador);
            when(sesionRepository.findAllByOrderByFechaHoraDesc()).thenReturn(List.of(sesion(EstadoSesion.REQUESTED, INICIO_SESION)));

            assertEquals(1, sesionService.listarMias(EMAIL_COORDINADOR).size());
            verify(sesionRepository, never()).findByEstudianteIdConDetalle(any());
            verify(sesionRepository, never()).findByTutorIdConDetalle(any());
        }

        @Test
        void listarMiasComoTutor_DebeConsultarSoloLasSuyas() {
            autenticado(tutor);
            when(sesionRepository.findByTutorIdConDetalle(2L)).thenReturn(List.of());

            assertTrue(sesionService.listarMias(EMAIL_TUTOR).isEmpty());
            verify(sesionRepository, never()).findAllByOrderByFechaHoraDesc();
        }
    }

    @Nested
    class MarcarComoEvaluada {

        @Test
        void completadaPorSuEstudiante_DebePasarAReviewed() {
            existente(EstadoSesion.COMPLETED, AHORA.minusDays(1));
            autenticado(estudiante);
            guardarDevuelveArgumento();

            assertEquals(EstadoSesion.REVIEWED, sesionService.marcarComoEvaluada(10L, EMAIL_ESTUDIANTE).getEstado());
        }

        @Test
        void sesionNoCompletada_DebeLanzarReglaNegocioException() {
            existente(EstadoSesion.CONFIRMED, INICIO_SESION);
            autenticado(estudiante);

            assertThrows(ReglaNegocioException.class, () -> sesionService.marcarComoEvaluada(10L, EMAIL_ESTUDIANTE));
        }

        @Test
        void sesionYaEvaluada_DebeLanzarReglaNegocioException() {
            existente(EstadoSesion.REVIEWED, AHORA.minusDays(1));
            autenticado(estudiante);

            assertThrows(ReglaNegocioException.class, () -> sesionService.marcarComoEvaluada(10L, EMAIL_ESTUDIANTE));
        }

        @Test
        void estudianteAjeno_DebeLanzarAccessDenied() {
            existente(EstadoSesion.COMPLETED, AHORA.minusDays(1));
            autenticado(otroEstudiante);

            assertThrows(AccessDeniedException.class,
                    () -> sesionService.marcarComoEvaluada(10L, EMAIL_OTRO_ESTUDIANTE));
        }
    }
}
