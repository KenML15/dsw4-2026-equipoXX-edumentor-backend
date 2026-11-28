# Contrato entre Sesiones (P3) y Evaluaciones (P4)

Define cómo una sesión de tutoría pasa de `COMPLETED` a `REVIEWED` y qué puede hacer cada módulo.

## Responsabilidades

| Módulo | Responsable de |
| :--- | :--- |
| P3 — Sesiones | Ciclo de vida de `SesionTutoria` y la transición `COMPLETED → REVIEWED` |
| P4 — Evaluaciones | Endpoint `POST /api/sesiones/{id}/evaluacion`, entidad `EvaluacionTutoria` y promedio del tutor |

P4 no cambia `estado` directamente. Siempre usa el método público de P3:

```java
SesionTutoria SesionService.marcarComoEvaluada(Long sesionId, String emailEstudiante)
```

P3 no expone un endpoint de evaluación: `POST /api/sesiones/{id}/evaluacion` pertenece a P4. P3 es dueño de la transición de estado y P4 es dueño del endpoint, la entidad y el promedio.

## Reglas del método `marcarComoEvaluada`

1. Es `@Transactional` por sí mismo y bloquea la fila de la sesión con `PESSIMISTIC_WRITE`, por lo que es seguro aunque se invoque fuera de otra transacción. Si P4 lo llama desde su propio método `@Transactional`, se une a esa transacción (propagación `REQUIRED`) y el bloqueo se mantiene hasta que P4 confirme. `SesionConcurrenciaIntegrationTest` lo verifica con dos llamadas simultáneas directas: solo una queda en `REVIEWED`.
2. Solo el **estudiante de la sesión** puede evaluarla. Cualquier otro usuario recibe `AccessDeniedException` (HTTP 403).
3. Solo una sesión en estado `COMPLETED` puede evaluarse. En cualquier otro estado, incluido `REVIEWED`, lanza `ReglaNegocioException` (HTTP 400). Así se garantiza que la sesión se evalúa una sola vez.
4. Cambia el estado a `REVIEWED` y devuelve la entidad para que P4 la asocie a `EvaluacionTutoria`.

## Por qué devuelve la entidad y no un DTO

El resto de P3 responde con `SesionResponse` porque esas respuestas salen al cliente HTTP. `marcarComoEvaluada` es un método interno entre servicios: P4 necesita la entidad administrada `SesionTutoria` para asignarla en la relación `EvaluacionTutoria.sesion` (`@OneToOne`) y para leer `sesion.getTutor()` y actualizar su promedio en la misma transacción, sin volver a consultar la base de datos. La entidad nunca debe devolverse desde un controlador de P4; P4 construye su propio DTO de respuesta.

## Uso esperado desde P4

Todo ocurre dentro de una sola transacción de `EvaluacionService`:

```java
@Transactional
public EvaluacionResponse evaluar(Long sesionId, EvaluacionRequest request, String emailEstudiante) {
    SesionTutoria sesion = sesionService.marcarComoEvaluada(sesionId, emailEstudiante);
    EvaluacionTutoria evaluacion = evaluacionRepository.save(...);
    recalcularPromedioDelTutor(sesion.getTutor(), request.calificacion());
    return ...;
}
```

Si guardar la evaluación o recalcular el promedio falla, la transacción se revierte y la sesión vuelve a quedar en `COMPLETED`.

## Preguntas resueltas

| Pregunta | Decisión |
| :--- | :--- |
| ¿Quién puede evaluar? | Solo el estudiante que solicitó la sesión. |
| ¿Cuándo cambia a `REVIEWED`? | En el momento en que P4 registra la evaluación, dentro de la misma transacción. |
| ¿Qué pasa si nunca se evalúa? | La sesión queda en `COMPLETED` indefinidamente. Es un estado final válido y no hay vencimiento automático. |
| ¿Se puede modificar una sesión completada? | No. `COMPLETED` solo admite la transición a `REVIEWED`; `REVIEWED`, `REJECTED` y `CANCELLED` son estados finales. |
| ¿Se puede evaluar dos veces? | No. La segunda llamada encuentra la sesión en `REVIEWED` y responde 400. |

## Lista de verificación para P4

- [ ] `EvaluacionService.evaluar` es `@Transactional` y llama a `sesionService.marcarComoEvaluada` antes de guardar la evaluación.
- [ ] Ningún código de P4 llama a `sesion.setEstado(...)` ni guarda `SesionTutoria` directamente.
- [ ] El endpoint `POST /api/sesiones/{id}/evaluacion` está restringido a `ESTUDIANTE` en `SecurityConfig`.
- [ ] La respuesta del endpoint usa un DTO propio de P4, no la entidad.
- [ ] Existe una prueba de P4 que verifica que, si falla el guardado de la evaluación, la sesión sigue en `COMPLETED`.

## Pruebas existentes en P3

- `SesionServiceTest.MarcarComoEvaluada`: paso a `REVIEWED`, sesión no completada, sesión ya evaluada y estudiante ajeno.
- `SesionConcurrenciaIntegrationTest`: dos evaluaciones simultáneas de la misma sesión, sin transacción externa; solo una se aplica.
