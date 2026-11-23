package cr.ac.ucr.if0009.edumentor_backend.exception;

import java.util.Map;

public class ReglaNegocioException extends RuntimeException {

    private final String codigo;
    private final Map<String, Object> parametros;

    public ReglaNegocioException(String mensaje) {
        this(null, mensaje, Map.of());
    }

    public ReglaNegocioException(String codigo, String mensaje) {
        this(codigo, mensaje, Map.of());
    }

    public ReglaNegocioException(String codigo, String mensaje, Map<String, Object> parametros) {
        super(mensaje);
        this.codigo = codigo;
        this.parametros = parametros == null ? Map.of() : Map.copyOf(parametros);
    }

    public String getCodigo() {
        return codigo;
    }

    public Map<String, Object> getParametros() {
        return parametros;
    }
}
