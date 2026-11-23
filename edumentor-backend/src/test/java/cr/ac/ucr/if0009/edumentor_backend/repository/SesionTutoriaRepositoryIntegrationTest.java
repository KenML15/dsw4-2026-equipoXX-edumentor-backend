package cr.ac.ucr.if0009.edumentor_backend.repository;

import cr.ac.ucr.if0009.edumentor_backend.entity.EstadoSesion;
import cr.ac.ucr.if0009.edumentor_backend.entity.ModalidadSesion;
import cr.ac.ucr.if0009.edumentor_backend.entity.SesionTutoria;
import cr.ac.ucr.if0009.edumentor_backend.entity.Usuario;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.EnumSet;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(properties = "spring.jpa.properties.hibernate.generate_statistics=true")
@Transactional
class SesionTutoriaRepositoryIntegrationTest {

    private static final EnumSet<EstadoSesion> OCUPAN = EnumSet.of(EstadoSesion.CONFIRMED, EstadoSesion.IN_SESSION);

    @Autowired
    private SesionTutoriaRepository sesionRepository;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private MateriaRepository materiaRepository;

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private EntityManagerFactory entityManagerFactory;

    private Usuario estudiante;
    private Usuario tutor;
    private LocalDateTime base;

    @BeforeEach
    void setUp() {
        estudiante = usuarioRepository.findByEmail("estudiante@ucr.ac.cr").orElseThrow();
        tutor = usuarioRepository.findByEmail("tutor@ucr.ac.cr").orElseThrow();
        base = LocalDate.now().plusDays(60).atTime(9, 0);
    }

    private SesionTutoria guardar(EstadoSesion estado, LocalDateTime inicio, int minutos) {
        return sesionRepository.save(SesionTutoria.builder()
                .estudiante(estudiante)
                .tutor(tutor)
                .materia(materiaRepository.findById(1L).orElseThrow())
                .fechaHora(inicio)
                .fechaHoraFin(inicio.plusMinutes(minutos))
                .duracionMinutos(minutos)
                .modalidad(ModalidadSesion.PRESENCIAL)
                .enlaceOUbicacion("Aula 201")
                .estado(estado)
                .fechaCreacion(LocalDateTime.now())
                .build());
    }

    @Test
    void findPorEstadoQueSeCruzan_DevuelveSoloConfirmadasQueSeCruzanConElRango() {
        SesionTutoria cruza = guardar(EstadoSesion.CONFIRMED, base, 60);
        guardar(EstadoSesion.CONFIRMED, base.plusHours(1), 60);
        guardar(EstadoSesion.REQUESTED, base, 60);
        guardar(EstadoSesion.CONFIRMED, base.minusHours(1), 60);

        List<SesionTutoria> resultado = sesionRepository.findPorEstadoQueSeCruzan(
                EstadoSesion.CONFIRMED, base.plusMinutes(30), base.plusMinutes(60)
        );

        assertEquals(List.of(cruza.getId()), resultado.stream().map(SesionTutoria::getId).toList());
    }

    @Test
    void existeTraslape_SesionesContiguasNoSeTraslapan() {
        guardar(EstadoSesion.CONFIRMED, base, 60);

        assertFalse(sesionRepository.existeTraslapeTutor(tutor.getId(), OCUPAN, base.plusHours(1), base.plusHours(2), null));
        assertFalse(sesionRepository.existeTraslapeTutor(tutor.getId(), OCUPAN, base.minusHours(1), base, null));
        assertTrue(sesionRepository.existeTraslapeTutor(tutor.getId(), OCUPAN, base.plusMinutes(59), base.plusHours(2), null));
        assertTrue(sesionRepository.existeTraslapeEstudiante(estudiante.getId(), OCUPAN, base.minusMinutes(30), base.plusMinutes(1), null));
    }

    @Test
    void existeTraslape_IgnoraLaPropiaSesionYEstadosQueNoOcupan() {
        SesionTutoria propia = guardar(EstadoSesion.CONFIRMED, base, 60);
        guardar(EstadoSesion.CANCELLED, base, 60);
        guardar(EstadoSesion.REJECTED, base, 60);

        assertFalse(sesionRepository.existeTraslapeTutor(tutor.getId(), OCUPAN, base, base.plusHours(1), propia.getId()));
    }

    @Test
    void listarPorTutor_CargaRelacionesEnUnaSolaConsulta() {
        guardar(EstadoSesion.CONFIRMED, base, 60);
        guardar(EstadoSesion.REQUESTED, base.plusDays(1), 60);
        guardar(EstadoSesion.COMPLETED, base.plusDays(2), 60);
        entityManager.flush();
        entityManager.clear();

        Statistics estadisticas = entityManagerFactory.unwrap(SessionFactory.class).getStatistics();
        estadisticas.clear();

        List<SesionTutoria> sesiones = sesionRepository.findByTutorIdConDetalle(tutor.getId());
        sesiones.forEach(s -> {
            s.getEstudiante().getNombre();
            s.getTutor().getNombre();
            s.getMateria().getNombre();
        });

        assertTrue(sesiones.size() >= 3);
        assertEquals(1, estadisticas.getPrepareStatementCount());
    }

    @Test
    void listarTodasParaCoordinador_UsaEntityGraphSinNMasUno() {
        guardar(EstadoSesion.CONFIRMED, base, 60);
        guardar(EstadoSesion.REQUESTED, base.plusDays(1), 60);
        entityManager.flush();
        entityManager.clear();

        Statistics estadisticas = entityManagerFactory.unwrap(SessionFactory.class).getStatistics();
        estadisticas.clear();

        sesionRepository.findAllByOrderByFechaHoraDesc()
                .forEach(s -> s.getMateria().getNombre().length());

        assertEquals(1, estadisticas.getPrepareStatementCount());
    }
}
