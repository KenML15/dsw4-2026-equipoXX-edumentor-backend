package cr.ac.ucr.if0009.edumentor_backend.config;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.ZoneId;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TiempoConfigTest {

    @Test
    void relojUsaCostaRicaPorDefectoSinImportarLaZonaDelServidor() {
        assertEquals(ZoneId.of("America/Costa_Rica"), new TiempoConfig().clock(SesionesProperties.porDefecto()).getZone());
    }

    @Test
    void relojUsaLaZonaConfigurada() {
        SesionesProperties propiedades = new SesionesProperties(null, null, null, ZoneId.of("UTC"));

        assertEquals(ZoneId.of("UTC"), new TiempoConfig().clock(propiedades).getZone());
    }

    @Test
    void valoresPorDefectoDeLasReglasDeTiempo() {
        SesionesProperties propiedades = SesionesProperties.porDefecto();

        assertEquals(Duration.ofMinutes(15), propiedades.toleranciaInicio());
        assertEquals(Duration.ofMinutes(15), propiedades.toleranciaFin());
        assertEquals(Duration.ofHours(2), propiedades.anticipacionCancelacion());
    }
}
