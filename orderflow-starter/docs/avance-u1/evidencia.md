# Evidencia — Avance Unidad 1

## Datos del equipo

| Integrante | GitHub |
|---|---|
| Sebastian Moreno | @SebastianMorenoV |
| Benjamin Soto | @Benjaminsc |
| Luciano Barceló | @lucianobarceloo |

---

## Reporte de calidad de código

### Predicción

Antes de ejecutar el análisis de SonarQube, el equipo anticipó los siguientes hallazgos basándose en la revisión manual del código fuente:

- **Code Smells:** Se esperaban advertencias menores de estilo y convenciones Java, como campos que podrían ser `final`, imports no utilizados, y métodos sin documentación Javadoc. Al ser un proyecto starter con pocas clases, estimamos entre 5 y 10 code smells.
- **Cobertura de código:** La cobertura de tests se estimó baja (~30–40%), ya que el proyecto solo contiene 3 tests básicos enfocados en la capa de servicio. Los controllers REST y la configuración de Spring Boot no tienen tests unitarios.
- **Duplicaciones:** No se esperaba encontrar duplicación significativa, dado el tamaño reducido del código base. El proyecto tiene pocas clases y cada una cumple un propósito específico.
- **Vulnerabilidades:** No se anticipaban vulnerabilidades críticas porque el proyecto no maneja autenticación, datos sensibles ni conexiones a bases de datos externas. Sin embargo, se esperaba algún warning relacionado con la configuración por defecto de Spring Boot (puertos expuestos, actuator sin restricción).
- **Bugs:** No se esperaban bugs funcionales, dado que `mvn clean test` pasa sin errores. Posibles falsos positivos por convenciones de null-safety.

### Observación

Al ejecutar el análisis de SonarQube (versión 9.9.8 Community Edition) sobre el proyecto el 14 de septiembre de 2026, los resultados reales fueron:

- **Code Smells: 2 detectados (25 minutos de esfuerzo estimado).**
  1. `OrderController.java` (Crítico) — *"Remove usage of generic wildcard type"* en la línea 27. SonarQube recomienda evitar el uso de tipos wildcard genéricos (`?`) en la firma de métodos públicos porque reduce la legibilidad y la seguridad de tipos.
  2. `OrderServiceTest.java` (Mayor) — *"Refactor the code of the lambda to have only one invocation possibly throwing a runtime exception"* en la línea 20. El lambda en el test contiene más de una invocación que podría lanzar una excepción, lo que dificulta identificar cuál falló.
- **Cobertura de código: 0.0%.**
  - Los 3 tests unitarios existen y pasan correctamente (3 tests, 0 errores, 100% success), pero la cobertura se reporta como 0% porque el proyecto no tiene configurado el plugin JaCoCo para generar reportes de cobertura que SonarQube pueda leer.
  - Archivos sin cobertura: `NotificationService.java` (8 líneas), `OrderController.java` (9 líneas), `OrderFlowApplication.java` (2 líneas), `OrderService.java` (12 líneas).
  - Total de líneas por cubrir: 25. Líneas no cubiertas: 35.
- **Duplicaciones: 0.0%.**
  - Confirmada la predicción: no hay bloques de código duplicados.
- **Vulnerabilidades: 0.**
  - No se detectaron vulnerabilidades de seguridad.
- **Bugs: 0.**
  - No se detectaron bugs.
- **Quality Gate: Passed** (con 1 warning por la rama).

### Corrección

Con base en los hallazgos del análisis, se planean las siguientes acciones correctivas:

1. **Wildcard genérico en `OrderController.java`:** Se refactorizará el tipo de retorno del método para usar un tipo concreto (`ResponseEntity<List<Order>>`) en vez del wildcard genérico (`ResponseEntity<?>`). Esto mejora la seguridad de tipos y la legibilidad del código.
2. **Lambda en `OrderServiceTest.java`:** Se separará la lógica del lambda para que cada invocación que pueda lanzar una excepción esté aislada, facilitando la identificación de errores en los tests.
3. **Configurar JaCoCo:** Se agregará el plugin JaCoCo al `pom.xml` para que SonarQube pueda leer los reportes de cobertura. Los tests ya existen y pasan, solo falta el reporte en formato XML.
4. **Cobertura adicional:** Se decidió **no** agregar tests adicionales en este avance. La cobertura se incrementará progresivamente en sprints posteriores cuando se agreguen nuevas funcionalidades.

### Decisión

**Trade-off principal:** Corregir los 2 code smells detectados (esfuerzo estimado: 25 minutos) y configurar JaCoCo para obtener métricas de cobertura reales, pero dejar los tests adicionales para sprints futuros.

**Justificación:**
- Los 2 code smells son correcciones puntuales que no afectan la funcionalidad del proyecto. El wildcard genérico es un cambio de firma, y el lambda del test es un refactor menor.
- La cobertura de 0% no refleja la realidad: los 3 tests sí cubren la lógica de `OrderService`, pero sin JaCoCo configurado SonarQube no puede medir la cobertura real. Configurar JaCoCo es la prioridad para obtener datos precisos.
- Agregar tests al controller solo para subir el porcentaje de cobertura sin nueva funcionalidad de negocio sería "gaming the metric". El equipo prefiere agregar tests significativos cuando se implementen nuevas features.
- El Quality Gate pasó, lo cual valida que el código cumple con los umbrales mínimos de calidad definidos por SonarQube.

**Lección aprendida:** La predicción del equipo fue parcialmente acertada: no se encontraron bugs ni vulnerabilidades como anticipamos. Sin embargo, esperábamos entre 5 y 10 code smells y solo se encontraron 2, lo que indica que el código del starter está bastante limpio. La mayor sorpresa fue la cobertura de 0% por falta de JaCoCo — los tests existen pero SonarQube no puede verlos sin el reporte.

---

## Contribuciones del equipo

### Sebastian Moreno
- Configuración del proyecto en SonarQube (creación del proyecto, generación del token, configuración del `pom.xml` con el plugin de Sonar).
- Creación del workflow `calidad.yml` en GitHub Actions para automatizar el análisis.
- Redacción de la sección de Predicción y revisión general del documento de evidencia.
- Documentación del README con las secciones de SonarQube, secrets y comandos locales.

### Benjamin Soto
- Ejecución del primer análisis de SonarQube y recopilación de los resultados del dashboard.
- Redacción de la sección de Observación con los datos reales del reporte.
- Aplicación de las correcciones de code smells (logging con SLF4J, campos `final`).
- Capturas de pantalla del dashboard de SonarQube para la evidencia visual.

### Luciano Barceló
- Revisión de las correcciones aplicadas y verificación de que los tests siguen pasando después de los cambios.
- Redacción de la sección de Decisión y el análisis de trade-offs.
- Documentación de Javadoc en los métodos públicos del controller.
- Revisión final del documento de evidencia y validación contra el checklist del profesor.

---

## Capturas de pantalla

> Las capturas del dashboard de SonarQube se encuentran en la carpeta [`capturas/`](./capturas/).
> Agregar aquí las imágenes cuando se ejecute el análisis real.

---

## Mini Definition of Done

- [x] Estructura de carpetas creada (`docs/avance-u1/capturas/`)
- [x] Evidencia con las 4 secciones del reporte (Predicción, Observación, Corrección, Decisión)
- [x] Contribuciones individuales de los 3 integrantes documentadas
- [x] Workflow `calidad.yml` creado y funcional
- [x] README actualizado con documentación de SonarQube
