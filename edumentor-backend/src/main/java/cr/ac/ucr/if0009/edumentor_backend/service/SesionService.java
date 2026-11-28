package cr.ac.ucr.if0009.edumentor_backend.service;

import cr.ac.ucr.if0009.edumentor_backend.config.SesionesProperties;
import cr.ac.ucr.if0009.edumentor_backend.dto.SesionRequest;
import cr.ac.ucr.if0009.edumentor_backend.dto.SesionResponse;
import cr.ac.ucr.if0009.edumentor_backend.dto.TutorResumenResponse;
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
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.URI;
import java.net.URISyntaxException;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class SesionService {

    static final Set<EstadoSesion> ESTADOS_QUE_OCUPAN_HORARIO =
            EnumSet.of(EstadoSesion.CONFIRMED, EstadoSesion.IN_SESSION);

    static final int PASO_DURACION_MINUTOS = 30;

    private static final Map<EstadoSesion, Set<EstadoSesion>> TRANSICIONES_VALIDAS = Map.of(
            EstadoSesion.REQUESTED, EnumSet.of(EstadoSesion.CONFIRMED, EstadoSesion.REJECTED, EstadoSesion.CANCELLED),
            EstadoSesion.CONFIRMED, EnumSet.of(EstadoSesion.IN_SESSION, EstadoSesion.CANCELLED),
            EstadoSesion.IN_SESSION, EnumSet.of(EstadoSesion.COMPLETED),
            EstadoSesion.COMPLETED, EnumSet.of(EstadoSesion.REVIEWED)
    );

    private static final int LARGO_MINIMO_UBICACION = 3;
    private static final int LARGO_MAXIMO_ENLACE = 500;
    private static final DateTimeFormatter FORMATO_HORA = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private final SesionTutoriaRepository sesionRepository;
    private final UsuarioRepository usuarioRepository;
    private final MateriaRepository materiaRepository;
    private final DisponibilidadService disponibilidadService;
    private final Clock clock;
    private final SesionesProperties propiedades;

    @Transactional
    public SesionResponse solicitar(SesionRequest request, String emailAutenticado) {
        Usuario estudiante = obtenerUsuarioPorEmail(emailAutenticado);
        if (estudiante.getRol() != Rol.ESTUDIANTE) {
            throw new AccessDeniedException("Solo los estudiantes pueden solicitar sesiones de tutoría");
        }

        Usuario tutor = usuarioRepository.findById(request.tutorId())
                .orElseThrow(() -> new RecursoNoEncontradoException("Tutor no encontrado con id: " + request.tutorId()));
        if (tutor.getRol() != Rol.TUTOR) {
            throw new ReglaNegocioException(CodigoErrorSesion.TUTOR_ROL_INVALIDO, "El usuario indicado no tiene el rol de TUTOR");
        }

        Materia materia = materiaRepository.findById(request.materiaId())
                .orElseThrow(() -> new RecursoNoEncontradoException("Materia no encontrada con id: " + request.materiaId()));
        if (!Boolean.TRUE.equals(materia.getActiva())) {
            throw new ReglaNegocioException(CodigoErrorSesion.MATERIA_INACTIVA, "La materia seleccionada no está activa");
        }

        validarDuracion(request.duracionMinutos());
        String enlaceOUbicacion = validarEnlaceOUbicacion(request.modalidad(), request.enlaceOUbicacion());

        LocalDateTime inicio = request.fechaHora();
        if (!inicio.isAfter(ahora())) {
            throw new ReglaNegocioException(CodigoErrorSesion.FECHA_PASADA, "La sesión debe programarse en una fecha y hora futura");
        }
        LocalDateTime fin = inicio.plusMinutes(request.duracionMinutos());

        if (!disponibilidadService.estaDisponible(tutor.getId(), inicio, request.duracionMinutos())) {
            throw new ReglaNegocioException(
                    CodigoErrorSesion.TUTOR_SIN_DISPONIBILIDAD,
                    "El tutor no tiene un bloque de disponibilidad activo que cubra toda la sesión solicitada"
            );
        }

        validarSinTraslape(tutor.getId(), estudiante.getId(), inicio, fin, null);

        SesionTutoria sesion = SesionTutoria.builder()
                .estudiante(estudiante)
                .tutor(tutor)
                .materia(materia)
                .fechaHora(inicio)
                .fechaHoraFin(fin)
                .duracionMinutos(request.duracionMinutos())
                .modalidad(request.modalidad())
                .estado(EstadoSesion.REQUESTED)
                .enlaceOUbicacion(enlaceOUbicacion)
                .fechaCreacion(ahora())
                .build();

        return mapToResponse(sesionRepository.save(sesion), estudiante);
    }

    @Transactional(readOnly = true)
    public List<SesionResponse> listarMias(String emailAutenticado) {
        Usuario usuario = obtenerUsuarioPorEmail(emailAutenticado);
        List<SesionTutoria> sesiones = switch (usuario.getRol()) {
            case ESTUDIANTE -> sesionRepository.findByEstudianteIdConDetalle(usuario.getId());
            case TUTOR -> sesionRepository.findByTutorIdConDetalle(usuario.getId());
            case COORDINADOR -> sesionRepository.findAllByOrderByFechaHoraDesc();
        };
        return sesiones.stream().map(s -> mapToResponse(s, usuario)).toList();
    }

    @Transactional(readOnly = true)
    public SesionResponse obtenerPorId(Long id, String emailAutenticado) {
        Usuario usuario = obtenerUsuarioPorEmail(emailAutenticado);
        SesionTutoria sesion = sesionRepository.findByIdConDetalle(id)
                .orElseThrow(() -> sesionNoEncontrada(id));
        boolean esParticipante = esEstudianteDe(usuario, sesion) || esTutorDe(usuario, sesion);
        if (!esParticipante && usuario.getRol() != Rol.COORDINADOR) {
            throw new AccessDeniedException("Solo los participantes de la sesión o un coordinador pueden consultarla");
        }
        return mapToResponse(sesion, usuario);
    }

    @Transactional(readOnly = true)
    public List<TutorResumenResponse> listarTutores() {
        return usuarioRepository.findByRolOrderByNombreAscApellidoAsc(Rol.TUTOR).stream()
                .map(t -> new TutorResumenResponse(
                        t.getId(),
                        t.getNombre() + " " + t.getApellido(),
                        t.getPromedioCalificacion(),
                        t.getTotalEvaluaciones()
                ))
                .toList();
    }

    @Transactional
    public SesionResponse aceptar(Long id, String emailAutenticado) {
        SesionTutoria sesion = bloquearSesionYParticipantes(id);
        Usuario usuario = obtenerUsuarioPorEmail(emailAutenticado);
        validarEsTutorDeLaSesion(usuario, sesion);
        validarTransicion(sesion, EstadoSesion.CONFIRMED, "aceptar");

        if (!ahora().isBefore(sesion.getFechaHora())) {
            throw new ReglaNegocioException(CodigoErrorSesion.SESION_YA_INICIO, "No se puede aceptar una sesión cuya hora de inicio ya pasó");
        }

        boolean hayTraslape = existeTraslape(
                sesion.getTutor().getId(),
                sesion.getEstudiante().getId(),
                sesion.getFechaHora(),
                sesion.getFechaHoraFin(),
                sesion.getId()
        );

        sesion.setEstado(hayTraslape ? EstadoSesion.REJECTED : EstadoSesion.CONFIRMED);
        return mapToResponse(sesionRepository.saveAndFlush(sesion), usuario);
    }

    @Transactional
    public SesionResponse rechazar(Long id, String emailAutenticado) {
        SesionTutoria sesion = bloquearSesion(id);
        Usuario usuario = obtenerUsuarioPorEmail(emailAutenticado);
        validarEsTutorDeLaSesion(usuario, sesion);
        validarTransicion(sesion, EstadoSesion.REJECTED, "rechazar");
        sesion.setEstado(EstadoSesion.REJECTED);
        return mapToResponse(sesionRepository.save(sesion), usuario);
    }

    @Transactional
    public SesionResponse cancelar(Long id, String emailAutenticado) {
        SesionTutoria sesion = bloquearSesion(id);
        Usuario usuario = obtenerUsuarioPorEmail(emailAutenticado);

        boolean esEstudiante = esEstudianteDe(usuario, sesion);
        boolean esTutor = esTutorDe(usuario, sesion);
        boolean esCoordinador = usuario.getRol() == Rol.COORDINADOR;
        if (!esEstudiante && !esTutor && !esCoordinador) {
            throw new AccessDeniedException("Solo los participantes de la sesión o un coordinador pueden cancelarla");
        }

        validarTransicion(sesion, EstadoSesion.CANCELLED, "cancelar");

        if (esTutor && sesion.getEstado() == EstadoSesion.REQUESTED) {
            throw new ReglaNegocioException(CodigoErrorSesion.TUTOR_DEBE_RECHAZAR, "El tutor no cancela solicitudes pendientes: debe rechazarlas");
        }

        LocalDateTime momento = ahora();
        if (!momento.isBefore(sesion.getFechaHora())) {
            throw new ReglaNegocioException(CodigoErrorSesion.SESION_YA_INICIO, "No se puede cancelar una sesión cuya hora de inicio ya pasó");
        }

        LocalDateTime limite = cancelableHasta(sesion, usuario);
        if (limite != null && momento.isAfter(limite)) {
            long horas = propiedades.anticipacionCancelacion().toHours();
            throw new ReglaNegocioException(
                    CodigoErrorSesion.CANCELACION_TARDIA,
                    "Solo se puede cancelar una sesión confirmada con al menos " + horas + " horas de anticipación",
                    Map.of("horas", horas)
            );
        }

        sesion.setEstado(EstadoSesion.CANCELLED);
        return mapToResponse(sesionRepository.save(sesion), usuario);
    }

    @Transactional
    public SesionResponse iniciar(Long id, String emailAutenticado) {
        SesionTutoria sesion = bloquearSesion(id);
        Usuario usuario = obtenerUsuarioPorEmail(emailAutenticado);
        validarEsTutorDeLaSesion(usuario, sesion);
        validarTransicion(sesion, EstadoSesion.IN_SESSION, "iniciar");

        LocalDateTime momento = ahora();
        LocalDateTime desde = inicioPermitido(sesion);
        if (momento.isBefore(desde)) {
            throw new ReglaNegocioException(
                    CodigoErrorSesion.INICIO_ANTICIPADO,
                    "La sesión todavía no puede iniciarse; podrá iniciarse a partir de " + desde.format(FORMATO_HORA),
                    Map.of("desde", desde.toString())
            );
        }
        if (!momento.isBefore(sesion.getFechaHoraFin())) {
            throw new ReglaNegocioException(CodigoErrorSesion.HORARIO_TERMINADO, "La sesión ya no puede iniciarse porque su horario terminó");
        }

        sesion.setEstado(EstadoSesion.IN_SESSION);
        return mapToResponse(sesionRepository.save(sesion), usuario);
    }

    @Transactional
    public SesionResponse finalizar(Long id, String emailAutenticado) {
        SesionTutoria sesion = bloquearSesion(id);
        Usuario usuario = obtenerUsuarioPorEmail(emailAutenticado);
        validarEsTutorDeLaSesion(usuario, sesion);
        validarTransicion(sesion, EstadoSesion.COMPLETED, "finalizar");

        LocalDateTime desde = finPermitido(sesion);
        if (ahora().isBefore(desde)) {
            throw new ReglaNegocioException(
                    CodigoErrorSesion.FIN_ANTICIPADO,
                    "La sesión todavía no puede finalizarse; podrá finalizarse a partir de " + desde.format(FORMATO_HORA),
                    Map.of("desde", desde.toString())
            );
        }

        sesion.setEstado(EstadoSesion.COMPLETED);
        return mapToResponse(sesionRepository.save(sesion), usuario);
    }

    @Transactional
    public SesionTutoria marcarComoEvaluada(Long id, String emailEstudiante) {
        SesionTutoria sesion = bloquearSesion(id);
        Usuario usuario = obtenerUsuarioPorEmail(emailEstudiante);
        if (!esEstudianteDe(usuario, sesion)) {
            throw new AccessDeniedException("Solo el estudiante de la sesión puede evaluarla");
        }
        validarTransicion(sesion, EstadoSesion.REVIEWED, "evaluar");
        sesion.setEstado(EstadoSesion.REVIEWED);
        return sesionRepository.save(sesion);
    }

    void validarDuracion(Integer duracionMinutos) {
        if (duracionMinutos == null || duracionMinutos <= 0 || duracionMinutos % PASO_DURACION_MINUTOS != 0) {
            throw new ReglaNegocioException(
                    CodigoErrorSesion.DURACION_INVALIDA,
                    "La duración debe ser múltiplo de " + PASO_DURACION_MINUTOS + " minutos",
                    Map.of("paso", PASO_DURACION_MINUTOS)
            );
        }
    }

    String validarEnlaceOUbicacion(ModalidadSesion modalidad, String valor) {
        String texto = valor == null ? "" : valor.trim();
        if (texto.length() > LARGO_MAXIMO_ENLACE) {
            throw new ReglaNegocioException(CodigoErrorSesion.ENLACE_LARGO, "El enlace o ubicación no puede exceder 500 caracteres");
        }
        if (modalidad == ModalidadSesion.VIRTUAL) {
            if (texto.isEmpty()) {
                throw new ReglaNegocioException(CodigoErrorSesion.ENLACE_REQUERIDO, "Las sesiones virtuales requieren un enlace de videollamada");
            }
            if (!esUrlHttpsValida(texto)) {
                throw new ReglaNegocioException(CodigoErrorSesion.ENLACE_INVALIDO, "El enlace de la sesión virtual debe ser una URL válida que use https");
            }
            return texto;
        }
        if (texto.length() < LARGO_MINIMO_UBICACION) {
            throw new ReglaNegocioException(
                    CodigoErrorSesion.UBICACION_REQUERIDA,
                    "Las sesiones presenciales requieren una ubicación (mínimo 3 caracteres)"
            );
        }
        return texto;
    }

    private boolean esUrlHttpsValida(String texto) {
        if (texto.chars().anyMatch(Character::isWhitespace)) {
            return false;
        }
        try {
            URI uri = new URI(texto);
            String host = uri.getHost();
            return "https".equalsIgnoreCase(uri.getScheme())
                    && host != null
                    && !host.isBlank()
                    && uri.getUserInfo() == null;
        } catch (URISyntaxException e) {
            return false;
        }
    }

    private void validarTransicion(SesionTutoria sesion, EstadoSesion destino, String accion) {
        Set<EstadoSesion> permitidos = TRANSICIONES_VALIDAS.getOrDefault(sesion.getEstado(), Set.of());
        if (!permitidos.contains(destino)) {
            throw new ReglaNegocioException(
                    CodigoErrorSesion.TRANSICION_INVALIDA,
                    "No se puede " + accion + " una sesión en estado " + sesion.getEstado(),
                    Map.of("accion", accion, "estado", sesion.getEstado().name())
            );
        }
    }

    private void validarSinTraslape(Long tutorId, Long estudianteId, LocalDateTime inicio, LocalDateTime fin, Long excluirId) {
        if (sesionRepository.existeTraslapeTutor(tutorId, ESTADOS_QUE_OCUPAN_HORARIO, inicio, fin, excluirId)) {
            throw new ReglaNegocioException(
                    CodigoErrorSesion.TRASLAPE_TUTOR,
                    "El tutor ya tiene una sesión confirmada que se traslapa con ese horario"
            );
        }
        if (sesionRepository.existeTraslapeEstudiante(estudianteId, ESTADOS_QUE_OCUPAN_HORARIO, inicio, fin, excluirId)) {
            throw new ReglaNegocioException(
                    CodigoErrorSesion.TRASLAPE_ESTUDIANTE,
                    "Ya tienes una sesión confirmada que se traslapa con ese horario"
            );
        }
    }

    private boolean existeTraslape(Long tutorId, Long estudianteId, LocalDateTime inicio, LocalDateTime fin, Long excluirId) {
        return sesionRepository.existeTraslapeTutor(tutorId, ESTADOS_QUE_OCUPAN_HORARIO, inicio, fin, excluirId)
                || sesionRepository.existeTraslapeEstudiante(estudianteId, ESTADOS_QUE_OCUPAN_HORARIO, inicio, fin, excluirId);
    }

    private void validarEsTutorDeLaSesion(Usuario usuario, SesionTutoria sesion) {
        if (!esTutorDe(usuario, sesion)) {
            throw new AccessDeniedException("Solo el tutor asignado puede realizar esta acción sobre la sesión");
        }
    }

    private boolean esTutorDe(Usuario usuario, SesionTutoria sesion) {
        return usuario.getRol() == Rol.TUTOR && usuario.getId().equals(sesion.getTutor().getId());
    }

    private boolean esEstudianteDe(Usuario usuario, SesionTutoria sesion) {
        return usuario.getRol() == Rol.ESTUDIANTE && usuario.getId().equals(sesion.getEstudiante().getId());
    }

    private SesionTutoria bloquearSesion(Long id) {
        SesionTutoria bloqueada = sesionRepository.bloquearPorId(id).orElseThrow(() -> sesionNoEncontrada(id));
        return sesionRepository.findByIdConDetalle(id).orElse(bloqueada);
    }

    private SesionTutoria bloquearSesionYParticipantes(Long id) {
        SesionTutoria bloqueada = sesionRepository.bloquearPorId(id).orElseThrow(() -> sesionNoEncontrada(id));
        usuarioRepository.bloquearPorIds(List.of(bloqueada.getEstudiante().getId(), bloqueada.getTutor().getId()));
        return sesionRepository.findByIdConDetalle(id).orElse(bloqueada);
    }

    private RecursoNoEncontradoException sesionNoEncontrada(Long id) {
        return new RecursoNoEncontradoException("Sesión de tutoría no encontrada con id: " + id);
    }

    private Usuario obtenerUsuarioPorEmail(String email) {
        return usuarioRepository.findByEmail(email)
                .orElseThrow(() -> new RecursoNoEncontradoException("Usuario autenticado no encontrado"));
    }

    private LocalDateTime ahora() {
        return LocalDateTime.now(clock);
    }

    private LocalDateTime inicioPermitido(SesionTutoria sesion) {
        return sesion.getFechaHora().minus(propiedades.toleranciaInicio());
    }

    private LocalDateTime finPermitido(SesionTutoria sesion) {
        return sesion.getFechaHoraFin().minus(propiedades.toleranciaFin());
    }

    LocalDateTime cancelableHasta(SesionTutoria sesion, Usuario usuario) {
        EstadoSesion estado = sesion.getEstado();
        if (estado != EstadoSesion.REQUESTED && estado != EstadoSesion.CONFIRMED) {
            return null;
        }
        if (usuario.getRol() == Rol.COORDINADOR) {
            return sesion.getFechaHora();
        }
        if (esTutorDe(usuario, sesion)) {
            return estado == EstadoSesion.CONFIRMED
                    ? sesion.getFechaHora().minus(propiedades.anticipacionCancelacion())
                    : null;
        }
        if (esEstudianteDe(usuario, sesion)) {
            return estado == EstadoSesion.CONFIRMED
                    ? sesion.getFechaHora().minus(propiedades.anticipacionCancelacion())
                    : sesion.getFechaHora();
        }
        return null;
    }

    private SesionResponse mapToResponse(SesionTutoria s, Usuario usuario) {
        Usuario estudiante = s.getEstudiante();
        Usuario tutor = s.getTutor();
        Materia materia = s.getMateria();
        return new SesionResponse(
                s.getId(),
                estudiante.getId(),
                estudiante.getNombre() + " " + estudiante.getApellido(),
                tutor.getId(),
                tutor.getNombre() + " " + tutor.getApellido(),
                materia.getId(),
                materia.getCodigo(),
                materia.getNombre(),
                s.getFechaHora(),
                s.getFechaHoraFin(),
                s.getDuracionMinutos(),
                s.getModalidad(),
                s.getEstado(),
                s.getEnlaceOUbicacion(),
                s.getFechaCreacion(),
                inicioPermitido(s),
                finPermitido(s),
                cancelableHasta(s, usuario)
        );
    }
}
