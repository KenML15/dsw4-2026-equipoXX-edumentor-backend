package cr.ac.ucr.if0009.edumentor_backend.service;

import cr.ac.ucr.if0009.edumentor_backend.entity.EstadoSesion;
import cr.ac.ucr.if0009.edumentor_backend.entity.ModalidadSesion;
import cr.ac.ucr.if0009.edumentor_backend.entity.Rol;
import cr.ac.ucr.if0009.edumentor_backend.entity.SesionTutoria;
import cr.ac.ucr.if0009.edumentor_backend.entity.Usuario;
import cr.ac.ucr.if0009.edumentor_backend.exception.ReglaNegocioException;
import cr.ac.ucr.if0009.edumentor_backend.repository.MateriaRepository;
import cr.ac.ucr.if0009.edumentor_backend.repository.SesionTutoriaRepository;
import cr.ac.ucr.if0009.edumentor_backend.repository.UsuarioRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.RepeatedTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

@SpringBootTest
class SesionConcurrenciaIntegrationTest {

    @Autowired
    private SesionService sesionService;

    @Autowired
    private SesionTutoriaRepository sesionRepository;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private MateriaRepository materiaRepository;

    private final List<Long> sesionesCreadas = new ArrayList<>();
    private Usuario estudiante;
    private Usuario otroEstudiante;
    private Usuario tutor;
    private ExecutorService executor;

    @BeforeEach
    void setUp() {
        estudiante = usuarioRepository.findByEmail("estudiante@ucr.ac.cr").orElseThrow();
        tutor = usuarioRepository.findByEmail("tutor@ucr.ac.cr").orElseThrow();
        otroEstudiante = usuarioRepository.findByEmail("concurrencia@test.local").orElseGet(() ->
                usuarioRepository.save(Usuario.builder()
                        .carne("ICONC01").email("concurrencia@test.local").passwordHash("x")
                        .nombre("Concurrencia").apellido("Prueba").rol(Rol.ESTUDIANTE).build()));
        executor = Executors.newFixedThreadPool(2);
    }

    @AfterEach
    void limpiar() {
        executor.shutdownNow();
        sesionRepository.deleteAllById(sesionesCreadas);
        sesionesCreadas.clear();
    }

    private SesionTutoria crearSolicitud(Usuario solicitante, LocalDateTime inicio) {
        SesionTutoria sesion = sesionRepository.save(SesionTutoria.builder()
                .estudiante(solicitante)
                .tutor(tutor)
                .materia(materiaRepository.findById(1L).orElseThrow())
                .fechaHora(inicio)
                .fechaHoraFin(inicio.plusMinutes(60))
                .duracionMinutos(60)
                .modalidad(ModalidadSesion.PRESENCIAL)
                .enlaceOUbicacion("Aula 201")
                .estado(EstadoSesion.REQUESTED)
                .fechaCreacion(LocalDateTime.now())
                .build());
        sesionesCreadas.add(sesion.getId());
        return sesion;
    }

    private List<Object> ejecutarEnParalelo(List<Callable<Object>> tareas) throws Exception {
        CountDownLatch salida = new CountDownLatch(1);
        List<Future<Object>> futuros = new ArrayList<>();
        for (Callable<Object> tarea : tareas) {
            futuros.add(executor.submit(() -> {
                salida.await();
                try {
                    return tarea.call();
                } catch (Exception e) {
                    return e;
                }
            }));
        }
        salida.countDown();
        List<Object> resultados = new ArrayList<>();
        for (Future<Object> futuro : futuros) {
            resultados.add(futuro.get(30, TimeUnit.SECONDS));
        }
        return resultados;
    }

    @RepeatedTest(5)
    void aceptarDosSolicitudesTraslapadasALaVez_SoloUnaQuedaConfirmada() throws Exception {
        LocalDateTime inicio = LocalDate.now().plusDays(40).atTime(9, 0);
        SesionTutoria primera = crearSolicitud(estudiante, inicio);
        SesionTutoria segunda = crearSolicitud(otroEstudiante, inicio.plusMinutes(30));

        List<Object> resultados = ejecutarEnParalelo(List.of(
                () -> sesionService.aceptar(primera.getId(), "tutor@ucr.ac.cr").estado(),
                () -> sesionService.aceptar(segunda.getId(), "tutor@ucr.ac.cr").estado()
        ));

        long confirmadas = resultados.stream().filter(r -> r == EstadoSesion.CONFIRMED).count();
        long rechazadas = resultados.stream().filter(r -> r == EstadoSesion.REJECTED).count();
        assertEquals(1, confirmadas, "Resultados: " + resultados);
        assertEquals(1, rechazadas, "Resultados: " + resultados);
    }

    @RepeatedTest(5)
    void evaluarLaMismaSesionDosVecesALaVezSinTransaccionExterna_SoloUnaQuedaReviewed() throws Exception {
        SesionTutoria sesion = crearSolicitud(estudiante, LocalDate.now().minusDays(3).atTime(9, 0));
        sesion.setEstado(EstadoSesion.COMPLETED);
        sesionRepository.save(sesion);

        List<Object> resultados = ejecutarEnParalelo(List.of(
                () -> sesionService.marcarComoEvaluada(sesion.getId(), "estudiante@ucr.ac.cr").getEstado(),
                () -> sesionService.marcarComoEvaluada(sesion.getId(), "estudiante@ucr.ac.cr").getEstado()
        ));

        long evaluadas = resultados.stream().filter(r -> r == EstadoSesion.REVIEWED).count();
        assertEquals(1, evaluadas, "Resultados: " + resultados);
        Object fallido = resultados.stream().filter(r -> r != EstadoSesion.REVIEWED).findFirst().orElseThrow();
        assertInstanceOf(ReglaNegocioException.class, fallido, "Resultados: " + resultados);
        assertEquals(EstadoSesion.REVIEWED, sesionRepository.findById(sesion.getId()).orElseThrow().getEstado());
    }

    @RepeatedTest(5)
    void aceptarLaMismaSolicitudDosVecesALaVez_UnaFallaPorTransicionInvalida() throws Exception {
        SesionTutoria sesion = crearSolicitud(estudiante, LocalDate.now().plusDays(41).atTime(9, 0));

        List<Object> resultados = ejecutarEnParalelo(List.of(
                () -> sesionService.aceptar(sesion.getId(), "tutor@ucr.ac.cr").estado(),
                () -> sesionService.aceptar(sesion.getId(), "tutor@ucr.ac.cr").estado()
        ));

        long confirmadas = resultados.stream().filter(r -> r == EstadoSesion.CONFIRMED).count();
        assertEquals(1, confirmadas, "Resultados: " + resultados);
        Object fallido = resultados.stream().filter(r -> r != EstadoSesion.CONFIRMED).findFirst().orElseThrow();
        assertInstanceOf(ReglaNegocioException.class, fallido, "Resultados: " + resultados);
        assertEquals(EstadoSesion.CONFIRMED, sesionRepository.findById(sesion.getId()).orElseThrow().getEstado());
    }
}
