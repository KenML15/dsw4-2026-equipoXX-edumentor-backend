package cr.ac.ucr.if0009.edumentor_backend.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

@Configuration
@EnableConfigurationProperties(SesionesProperties.class)
public class TiempoConfig {

    @Bean
    public Clock clock(SesionesProperties propiedades) {
        return Clock.system(propiedades.zonaHoraria());
    }
}
