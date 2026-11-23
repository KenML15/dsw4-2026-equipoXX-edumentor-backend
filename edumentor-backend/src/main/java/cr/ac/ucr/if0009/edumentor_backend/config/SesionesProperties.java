package cr.ac.ucr.if0009.edumentor_backend.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;
import java.time.ZoneId;

@ConfigurationProperties(prefix = "sesiones")
public record SesionesProperties(
        Duration toleranciaInicio,
        Duration toleranciaFin,
        Duration anticipacionCancelacion,
        ZoneId zonaHoraria
) {

    public static final ZoneId ZONA_POR_DEFECTO = ZoneId.of("America/Costa_Rica");

    public SesionesProperties {
        toleranciaInicio = valorOPorDefecto(toleranciaInicio, Duration.ofMinutes(15));
        toleranciaFin = valorOPorDefecto(toleranciaFin, Duration.ofMinutes(15));
        anticipacionCancelacion = valorOPorDefecto(anticipacionCancelacion, Duration.ofHours(2));
        zonaHoraria = zonaHoraria == null ? ZONA_POR_DEFECTO : zonaHoraria;
    }

    public static SesionesProperties porDefecto() {
        return new SesionesProperties(null, null, null, null);
    }

    private static Duration valorOPorDefecto(Duration valor, Duration porDefecto) {
        return valor == null || valor.isNegative() ? porDefecto : valor;
    }
}
