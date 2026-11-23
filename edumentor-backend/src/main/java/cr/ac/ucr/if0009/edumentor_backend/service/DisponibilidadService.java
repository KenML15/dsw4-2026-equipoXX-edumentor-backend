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
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class DisponibilidadService {

    private final DisponibilidadTutorRepository disponibilidadRepository;
    private final UsuarioRepository usuarioRepository;

    @Transactional(readOnly = true)
    public List<DisponibilidadResponse> listarPorTutor(Long tutorId, boolean soloActivas) {
        Usuario tutor = usuarioRepository.findById(tutorId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Tutor no encontrado con id: " + tutorId));

        if (tutor.getRol() != Rol.TUTOR) {
            throw new ReglaNegocioException("El usuario indicado no tiene el rol de TUTOR");
        }

        List<DisponibilidadTutor> bloques = soloActivas
                ? disponibilidadRepository.findActivasByTutorIdWithTutor(tutorId)
                : disponibilidadRepository.findByTutorIdWithTutor(tutorId);

        return bloques.stream().map(this::mapToResponse).toList();
    }

    @Transactional(readOnly = true)
    public List<DisponibilidadResponse> listarMias(String emailAutenticado) {
        Usuario usuario = obtenerUsuarioPorEmail(emailAutenticado);
        if (usuario.getRol() != Rol.TUTOR) {
            throw new ReglaNegocioException("Solo los usuarios con rol TUTOR tienen bloques de disponibilidad");
        }
        return disponibilidadRepository.findByTutorIdWithTutor(usuario.getId())
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Transactional
    public DisponibilidadResponse crear(DisponibilidadRequest request, String emailAutenticado) {
        Usuario autenticado = obtenerUsuarioPorEmail(emailAutenticado);
        Usuario tutor = resolverTutorObjetivo(autenticado, request.tutorId());

        validarRangoHoras(request.horaInicio(), request.horaFin());

        boolean activo = request.activo() != null ? request.activo() : true;
        if (activo) {
            validarNoTraslape(tutor.getId(), request.diaSemana(), request.horaInicio(), request.horaFin(), null);
        }

        DisponibilidadTutor bloque = DisponibilidadTutor.builder()
                .tutor(tutor)
                .diaSemana(request.diaSemana())
                .horaInicio(request.horaInicio())
                .horaFin(request.horaFin())
                .activo(activo)
                .build();

        return mapToResponse(disponibilidadRepository.save(bloque));
    }

    @Transactional
    public DisponibilidadResponse actualizar(Long id, DisponibilidadRequest request, String emailAutenticado) {
        Usuario autenticado = obtenerUsuarioPorEmail(emailAutenticado);
        DisponibilidadTutor bloque = disponibilidadRepository.findByIdWithTutor(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Bloque de disponibilidad no encontrado con id: " + id));

        validarPermisoGestion(autenticado, bloque.getTutor());
        validarRangoHoras(request.horaInicio(), request.horaFin());

        boolean activo = request.activo() != null ? request.activo() : bloque.getActivo();
        if (activo) {
            validarNoTraslape(bloque.getTutor().getId(), request.diaSemana(), request.horaInicio(), request.horaFin(), bloque.getId());
        }

        bloque.setDiaSemana(request.diaSemana());
        bloque.setHoraInicio(request.horaInicio());
        bloque.setHoraFin(request.horaFin());
        bloque.setActivo(activo);

        return mapToResponse(disponibilidadRepository.save(bloque));
    }

    @Transactional
    public DisponibilidadResponse cambiarEstado(Long id, boolean activo, String emailAutenticado) {
        Usuario autenticado = obtenerUsuarioPorEmail(emailAutenticado);
        DisponibilidadTutor bloque = disponibilidadRepository.findByIdWithTutor(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Bloque de disponibilidad no encontrado con id: " + id));

        validarPermisoGestion(autenticado, bloque.getTutor());

        if (activo) {
            validarNoTraslape(
                    bloque.getTutor().getId(),
                    bloque.getDiaSemana(),
                    bloque.getHoraInicio(),
                    bloque.getHoraFin(),
                    bloque.getId()
            );
        }

        bloque.setActivo(activo);
        return mapToResponse(disponibilidadRepository.save(bloque));
    }

    @Transactional
    public void eliminar(Long id, String emailAutenticado) {
        Usuario autenticado = obtenerUsuarioPorEmail(emailAutenticado);
        DisponibilidadTutor bloque = disponibilidadRepository.findByIdWithTutor(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Bloque de disponibilidad no encontrado con id: " + id));

        validarPermisoGestion(autenticado, bloque.getTutor());
        disponibilidadRepository.delete(bloque);
    }

    /**
     * Verifica si un tutor tiene disponibilidad activa continua para cubrir
     * una sesión que inicia en fechaHora con la duración indicada en minutos.
     * Método consumido por el módulo de Sesiones de Tutoría (Persona 3).
     */
    @Transactional(readOnly = true)
    public boolean estaDisponible(Long tutorId, LocalDateTime fechaHora, int duracion) {
        if (tutorId == null || fechaHora == null || duracion <= 0) {
            return false;
        }

        LocalDateTime fechaHoraFin = fechaHora.plusMinutes(duracion);
        if (!fechaHoraFin.toLocalDate().equals(fechaHora.toLocalDate())) {
            return false;
        }

        DayOfWeek dia = fechaHora.getDayOfWeek();
        LocalTime inicioSolicitado = fechaHora.toLocalTime();
        LocalTime finSolicitado = fechaHoraFin.toLocalTime();

        List<DisponibilidadTutor> bloquesActivos =
                disponibilidadRepository.findActivasByTutorIdAndDiaSemana(tutorId, dia);

        if (bloquesActivos.isEmpty()) {
            return false;
        }

        // Fusiona bloques contiguos (ej. 09:00-10:00 y 10:00-11:00 -> 09:00-11:00)
        LocalTime rangoInicio = null;
        LocalTime rangoFin = null;

        for (DisponibilidadTutor bloque : bloquesActivos) {
            if (rangoInicio == null) {
                rangoInicio = bloque.getHoraInicio();
                rangoFin = bloque.getHoraFin();
            } else if (bloque.getHoraInicio().equals(rangoFin)) {
                rangoFin = bloque.getHoraFin();
            } else {
                if (cubreIntervalo(rangoInicio, rangoFin, inicioSolicitado, finSolicitado)) {
                    return true;
                }
                rangoInicio = bloque.getHoraInicio();
                rangoFin = bloque.getHoraFin();
            }
        }

        return rangoInicio != null && cubreIntervalo(rangoInicio, rangoFin, inicioSolicitado, finSolicitado);
    }

    private boolean cubreIntervalo(LocalTime rangoInicio, LocalTime rangoFin, LocalTime inicio, LocalTime fin) {
        return !rangoInicio.isAfter(inicio) && !rangoFin.isBefore(fin);
    }

    private void validarRangoHoras(LocalTime horaInicio, LocalTime horaFin) {
        if (horaInicio == null || horaFin == null || !horaInicio.isBefore(horaFin)) {
            throw new ReglaNegocioException("La hora de inicio debe ser estrictamente anterior a la hora de fin");
        }
    }

    private void validarNoTraslape(Long tutorId, DayOfWeek diaSemana, LocalTime horaInicio, LocalTime horaFin, Long excluirId) {
        boolean hayTraslape = disponibilidadRepository.existeTraslapeActivo(
                tutorId, diaSemana, horaInicio, horaFin, excluirId
        );
        if (hayTraslape) {
            throw new ReglaNegocioException(
                    "El bloque de disponibilidad se traslapa con otro bloque activo del mismo tutor para el día " + diaSemana
            );
        }
    }

    private Usuario resolverTutorObjetivo(Usuario autenticado, Long tutorIdSolicitado) {
        if (autenticado.getRol() == Rol.TUTOR) {
            if (tutorIdSolicitado != null && !tutorIdSolicitado.equals(autenticado.getId())) {
                throw new ReglaNegocioException("Un tutor solo puede registrar bloques en su propio horario");
            }
            return autenticado;
        }

        if (autenticado.getRol() == Rol.COORDINADOR) {
            if (tutorIdSolicitado == null) {
                throw new ReglaNegocioException("El coordinador debe especificar el tutorId del bloque");
            }
            Usuario tutor = usuarioRepository.findById(tutorIdSolicitado)
                    .orElseThrow(() -> new RecursoNoEncontradoException("Tutor no encontrado con id: " + tutorIdSolicitado));
            if (tutor.getRol() != Rol.TUTOR) {
                throw new ReglaNegocioException("El usuario indicado no tiene el rol de TUTOR");
            }
            return tutor;
        }

        throw new ReglaNegocioException("No tiene permisos para gestionar bloques de disponibilidad");
    }

    private void validarPermisoGestion(Usuario autenticado, Usuario tutorDueno) {
        boolean esCoordinador = autenticado.getRol() == Rol.COORDINADOR;
        boolean esMismoTutor = autenticado.getRol() == Rol.TUTOR && autenticado.getId().equals(tutorDueno.getId());
        if (!esCoordinador && !esMismoTutor) {
            throw new ReglaNegocioException("Solo el tutor propietario o un coordinador pueden modificar este bloque");
        }
    }

    private Usuario obtenerUsuarioPorEmail(String email) {
        return usuarioRepository.findByEmail(email)
                .orElseThrow(() -> new RecursoNoEncontradoException("Usuario autenticado no encontrado"));
    }

    private DisponibilidadResponse mapToResponse(DisponibilidadTutor bloque) {
        Usuario tutor = bloque.getTutor();
        return new DisponibilidadResponse(
                bloque.getId(),
                tutor.getId(),
                tutor.getNombre() + " " + tutor.getApellido(),
                bloque.getDiaSemana(),
                bloque.getHoraInicio(),
                bloque.getHoraFin(),
                bloque.getActivo()
        );
    }
}
