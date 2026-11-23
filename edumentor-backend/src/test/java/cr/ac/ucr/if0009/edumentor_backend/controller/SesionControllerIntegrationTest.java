package cr.ac.ucr.if0009.edumentor_backend.controller;

import cr.ac.ucr.if0009.edumentor_backend.entity.EstadoSesion;
import cr.ac.ucr.if0009.edumentor_backend.entity.ModalidadSesion;
import cr.ac.ucr.if0009.edumentor_backend.entity.Rol;
import cr.ac.ucr.if0009.edumentor_backend.entity.SesionTutoria;
import cr.ac.ucr.if0009.edumentor_backend.entity.Usuario;
import cr.ac.ucr.if0009.edumentor_backend.repository.MateriaRepository;
import cr.ac.ucr.if0009.edumentor_backend.repository.SesionTutoriaRepository;
import cr.ac.ucr.if0009.edumentor_backend.repository.UsuarioRepository;
import cr.ac.ucr.if0009.edumentor_backend.security.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.TemporalAdjusters;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@Transactional
class SesionControllerIntegrationTest {

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private MateriaRepository materiaRepository;

    @Autowired
    private SesionTutoriaRepository sesionRepository;

    private MockMvc mockMvc;
    private String tokenEstudiante;
    private String tokenTutor;
    private String tokenCoordinador;
    private String tokenOtroEstudiante;
    private Usuario estudiante;
    private Usuario tutor;
    private LocalDateTime proximoMiercoles;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();

        estudiante = usuarioRepository.findByEmail("estudiante@ucr.ac.cr").orElseThrow();
        tutor = usuarioRepository.findByEmail("tutor@ucr.ac.cr").orElseThrow();
        Usuario coordinador = usuarioRepository.findByEmail("coordinador@ucr.ac.cr").orElseThrow();
        Usuario otroEstudiante = usuarioRepository.save(Usuario.builder()
                .carne("ITEST01").email("otro.integracion@test.local").passwordHash("x")
                .nombre("Otro").apellido("Estudiante").rol(Rol.ESTUDIANTE).build());

        tokenEstudiante = jwtService.generateToken(estudiante);
        tokenTutor = jwtService.generateToken(tutor);
        tokenCoordinador = jwtService.generateToken(coordinador);
        tokenOtroEstudiante = jwtService.generateToken(otroEstudiante);

        LocalDate fecha = LocalDate.now().plusDays(1).with(TemporalAdjusters.nextOrSame(DayOfWeek.WEDNESDAY));
        proximoMiercoles = fecha.atTime(LocalTime.of(10, 0));
    }

    private String solicitudJson(LocalDateTime fechaHora, int duracion, String modalidad, String enlace) {
        String enlaceJson = enlace == null ? "null" : "\"" + enlace + "\"";
        return """
                {"tutorId": %d, "materiaId": 1, "fechaHora": "%s", "duracionMinutos": %d,
                 "modalidad": "%s", "enlaceOUbicacion": %s}
                """.formatted(tutor.getId(), fechaHora, duracion, modalidad, enlaceJson);
    }

    private ResultActions solicitar(String token, String json) throws Exception {
        return mockMvc.perform(post("/api/sesiones")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(json));
    }

    private SesionTutoria sesionGuardada(EstadoSesion estado, LocalDateTime inicio) {
        return sesionRepository.save(SesionTutoria.builder()
                .estudiante(estudiante)
                .tutor(tutor)
                .materia(materiaRepository.findById(1L).orElseThrow())
                .fechaHora(inicio)
                .fechaHoraFin(inicio.plusMinutes(60))
                .duracionMinutos(60)
                .modalidad(ModalidadSesion.PRESENCIAL)
                .enlaceOUbicacion("Aula 201")
                .estado(estado)
                .fechaCreacion(LocalDateTime.now())
                .build());
    }

    @Test
    void sinToken_DebeResponder401EnFormatoProblemDetail() throws Exception {
        mockMvc.perform(get("/api/sesiones/mias"))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.title").value("No autenticado"));
    }

    @Test
    void tokenInvalido_DebeResponder401() throws Exception {
        mockMvc.perform(get("/api/sesiones/mias").header("Authorization", "Bearer token.invalido.x"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void tutorNoPuedeSolicitar_DebeResponder403() throws Exception {
        solicitar(tokenTutor, solicitudJson(proximoMiercoles, 60, "PRESENCIAL", "Aula 201"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403));
    }

    @Test
    void estudianteSolicitaReservaValida_DebeResponder201Requested() throws Exception {
        solicitar(tokenEstudiante, solicitudJson(proximoMiercoles, 60, "VIRTUAL", "https://meet.google.com/abc-defg-hij"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.estado").value("REQUESTED"))
                .andExpect(jsonPath("$.tutorId").value(tutor.getId()))
                .andExpect(jsonPath("$.cancelableHasta").exists())
                .andExpect(jsonPath("$.iniciableDesde").exists());
    }

    @Test
    void fueraDeLaDisponibilidad_DebeResponder400() throws Exception {
        solicitar(tokenEstudiante, solicitudJson(proximoMiercoles.withHour(17), 60, "PRESENCIAL", "Aula 201"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value(containsString("disponibilidad")));
    }

    @Test
    void duracionQueSeSaleDelBloque_DebeResponder400() throws Exception {
        solicitar(tokenEstudiante, solicitudJson(proximoMiercoles.withHour(11), 90, "PRESENCIAL", "Aula 201"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void virtualConEnlaceHttp_DebeResponder400() throws Exception {
        solicitar(tokenEstudiante, solicitudJson(proximoMiercoles, 60, "VIRTUAL", "http://meet.google.com/abc"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value(containsString("https")));
    }

    @Test
    void erroresDeReglaDeNegocio_IncluyenCodigoEstable() throws Exception {
        solicitar(tokenEstudiante, solicitudJson(proximoMiercoles, 60, "VIRTUAL", "http://meet.google.com/abc"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo").value("ENLACE_INVALIDO"));
    }

    @Test
    void duracionQueNoEsMultiploDe30_DebeResponder400ConCodigo() throws Exception {
        solicitar(tokenEstudiante, solicitudJson(proximoMiercoles, 45, "PRESENCIAL", "Aula 201"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo").value("DURACION_INVALIDA"))
                .andExpect(jsonPath("$.parametros.paso").value(30));
    }

    @Test
    void fechaPasada_DebeResponder400ConCodigo() throws Exception {
        solicitar(tokenEstudiante, solicitudJson(LocalDateTime.now().minusDays(1).withHour(10).withMinute(0), 60, "PRESENCIAL", "Aula 201"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo").value("FECHA_PASADA"));
    }

    @Test
    void cancelableHasta_SeCalculaParaElUsuarioQueConsulta() throws Exception {
        SesionTutoria sesion = sesionGuardada(EstadoSesion.CONFIRMED, proximoMiercoles);

        mockMvc.perform(get("/api/sesiones/{id}", sesion.getId()).header("Authorization", "Bearer " + tokenCoordinador))
                .andExpect(jsonPath("$.cancelableHasta").value(proximoMiercoles.toString() + ":00"));
        mockMvc.perform(get("/api/sesiones/{id}", sesion.getId()).header("Authorization", "Bearer " + tokenEstudiante))
                .andExpect(jsonPath("$.cancelableHasta").value(proximoMiercoles.minusHours(2).toString() + ":00"));
    }

    @Test
    void verificarDisponibilidadDeUnUsuarioQueNoEsTutor_DebeResponder400() throws Exception {
        mockMvc.perform(get("/api/disponibilidades/verificar")
                        .header("Authorization", "Bearer " + tokenEstudiante)
                        .param("tutorId", String.valueOf(estudiante.getId()))
                        .param("fechaHora", proximoMiercoles.toString())
                        .param("duracion", "60"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void rutaInexistente_DebeResponder404EnFormatoProblemDetail() throws Exception {
        mockMvc.perform(get("/api/sesiones/{id}/no-existe", 1).header("Authorization", "Bearer " + tokenCoordinador))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    void modalidadInexistente_DebeResponder400() throws Exception {
        solicitar(tokenEstudiante, solicitudJson(proximoMiercoles, 60, "HIBRIDA", "Aula 201"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    void duracionMenorAlMinimo_DebeResponder400() throws Exception {
        solicitar(tokenEstudiante, solicitudJson(proximoMiercoles, 20, "PRESENCIAL", "Aula 201"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value(containsString("duracionMinutos")));
    }

    @Test
    void estudianteNoPuedeAceptar_DebeResponder403() throws Exception {
        SesionTutoria sesion = sesionGuardada(EstadoSesion.REQUESTED, proximoMiercoles);

        mockMvc.perform(put("/api/sesiones/{id}/aceptar", sesion.getId())
                        .header("Authorization", "Bearer " + tokenEstudiante))
                .andExpect(status().isForbidden());
    }

    @Test
    void tutorAceptaYLuegoNoPuedeAceptarDeNuevo() throws Exception {
        SesionTutoria sesion = sesionGuardada(EstadoSesion.REQUESTED, proximoMiercoles);

        mockMvc.perform(put("/api/sesiones/{id}/aceptar", sesion.getId())
                        .header("Authorization", "Bearer " + tokenTutor))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("CONFIRMED"));

        mockMvc.perform(put("/api/sesiones/{id}/aceptar", sesion.getId())
                        .header("Authorization", "Bearer " + tokenTutor))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value(containsString("CONFIRMED")));
    }

    @Test
    void iniciarSesionFutura_DebeResponder400() throws Exception {
        SesionTutoria sesion = sesionGuardada(EstadoSesion.CONFIRMED, proximoMiercoles);

        mockMvc.perform(put("/api/sesiones/{id}/iniciar", sesion.getId())
                        .header("Authorization", "Bearer " + tokenTutor))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value(containsString("todavía no puede iniciarse")));
    }

    @Test
    void coordinadorPuedeCancelar() throws Exception {
        SesionTutoria sesion = sesionGuardada(EstadoSesion.CONFIRMED, proximoMiercoles);

        mockMvc.perform(put("/api/sesiones/{id}/cancelar", sesion.getId())
                        .header("Authorization", "Bearer " + tokenCoordinador))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("CANCELLED"));
    }

    @Test
    void coordinadorVeTodasLasSesiones() throws Exception {
        sesionGuardada(EstadoSesion.REQUESTED, proximoMiercoles);

        mockMvc.perform(get("/api/sesiones/mias").header("Authorization", "Bearer " + tokenCoordinador))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(greaterThanOrEqualTo(1)));
    }

    @Test
    void estudianteAjenoNoPuedeVerDetalle_DebeResponder403() throws Exception {
        SesionTutoria sesion = sesionGuardada(EstadoSesion.REQUESTED, proximoMiercoles);

        mockMvc.perform(get("/api/sesiones/{id}", sesion.getId())
                        .header("Authorization", "Bearer " + tokenOtroEstudiante))
                .andExpect(status().isForbidden());
    }

    @Test
    void sesionInexistente_DebeResponder404() throws Exception {
        mockMvc.perform(get("/api/sesiones/{id}", 999999)
                        .header("Authorization", "Bearer " + tokenCoordinador))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }
}
