INSERT INTO usuarios (carne, email, password_hash, nombre, apellido, rol, promedio_calificacion, total_evaluaciones)
VALUES
('C5H153', 'estudiante@ucr.ac.cr', '$2a$10$geqXHaI8.nxLgzGLpoMo0eftKv6EVgJyXsKIA5WuoK9bDoYwhvi9q', 'Kenneth', 'Miranda', 'ESTUDIANTE', 0.0, 0),
('B91234', 'tutor@ucr.ac.cr', '$2a$10$geqXHaI8.nxLgzGLpoMo0eftKv6EVgJyXsKIA5WuoK9bDoYwhvi9q', 'Sebastián', 'Quirós', 'TUTOR', 0.0, 0),
('A80001', 'coordinador@ucr.ac.cr', '$2a$10$geqXHaI8.nxLgzGLpoMo0eftKv6EVgJyXsKIA5WuoK9bDoYwhvi9q', 'Jonathan', 'Granados', 'COORDINADOR', 0.0, 0);

INSERT INTO materias (codigo, nombre, descripcion, creditos, activa)
VALUES
('IF0009', 'Desarrollo de Software IV', 'Arquitectura web empresarial con Spring Boot, Angular, JWT y pruebas automatizadas.', 4, TRUE),
('IF0008', 'Bases de Datos Avanzadas', 'Modelado relacional, normalización, optimización de consultas SQL y transacciones.', 4, TRUE),
('IF0003', 'Estructuras de Datos', 'Árboles, grafos, tablas hash y análisis de complejidad algorítmica.', 4, TRUE),
('MA0291', 'Introducción a la Matemática Discreta', 'Lógica proposicional, teoría de conjuntos, relaciones y grafos.', 3, TRUE);

INSERT INTO disponibilidades_tutor (tutor_id, dia_semana, hora_inicio, hora_fin, activo)
VALUES
(2, 'MONDAY', '08:00:00', '10:00:00', TRUE),
(2, 'MONDAY', '14:00:00', '16:00:00', TRUE),
(2, 'WEDNESDAY', '09:00:00', '12:00:00', TRUE),
(2, 'FRIDAY', '13:00:00', '15:00:00', TRUE);

INSERT INTO sesiones_tutoria (estudiante_id, tutor_id, materia_id, fecha_hora, fecha_hora_fin, duracion_minutos, modalidad, estado, enlace_o_ubicacion, fecha_creacion)
VALUES
(1, 2, 1, DATEADD(HOUR, 8, CAST(DATEADD(DAY, -7, CURRENT_DATE) AS TIMESTAMP)), DATEADD(HOUR, 9, CAST(DATEADD(DAY, -7, CURRENT_DATE) AS TIMESTAMP)), 60, 'VIRTUAL', 'COMPLETED', 'https://meet.google.com/edu-ment-or1', DATEADD(DAY, -10, CURRENT_TIMESTAMP)),
(1, 2, 2, DATEADD(HOUR, 14, CAST(DATEADD(DAY, 10, CURRENT_DATE) AS TIMESTAMP)), DATEADD(HOUR, 15, CAST(DATEADD(DAY, 10, CURRENT_DATE) AS TIMESTAMP)), 60, 'PRESENCIAL', 'CONFIRMED', 'Aula 201, Sede Paraíso', CURRENT_TIMESTAMP);
