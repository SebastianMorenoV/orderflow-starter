# Evidencia — Avance Unidad 1

## Identificación

| Campo | Valor |
|-------|-------|
| **Equipo** | OrderFlow — Equipo Tigre |
| **Repositorio** | [SebastianMorenoV/orderflow-starter](https://github.com/SebastianMorenoV/orderflow-starter) |
| **Proyecto SonarQube** | `orderflow-starter` |
| **Producto Sonar** | SonarQube Community Edition 9.9.8 LTS (self-hosted en AWS EC2) |
| **Commit de entrega** | `c3b6b93` (rama `feature/evidencia-u1`) |

| Integrante | GitHub |
|---|---|
| Sebastian Moreno | @SebastianMorenoV |
| Benjamin Soto | @Benjaminsc |
| Luciano Barceló | @lucianobarceloo |

---

## Flujo del workflow

El workflow [`calidad.yml`](../../.github/workflows/calidad.yml) se ejecuta automáticamente con los siguientes parámetros:

- **Evento:** `push` a la rama `main` y `pull_request` dirigido a `main`.
- **Runner:** `ubuntu-latest` (GitHub-hosted).
- **Steps:**
  1. **Checkout del código** — `actions/checkout@v4` con `fetch-depth: 0` para que Sonar tenga el historial completo y pueda hacer análisis incremental.
  2. **Configurar Java 21** — `actions/setup-java@v4` con distribución Temurin y cache de Maven.
  3. **Cache de SonarQube** — `actions/cache@v4` para `~/.sonar/cache`, acelera ejecuciones subsecuentes.
  4. **Build, tests y cobertura (JaCoCo)** — `mvn -B clean verify` compila, ejecuta tests y genera el reporte de cobertura XML con JaCoCo.
  5. **Análisis SonarQube** — `mvn -B sonar:sonar` invoca el SonarScanner for Maven, enviando el código y los reportes de cobertura al servidor SonarQube.

**Función de SonarQube:** Recibe el código fuente y los reportes de JaCoCo, ejecuta el análisis estático (code smells, bugs, vulnerabilidades, duplicaciones) y evalúa el Quality Gate configurado en el servidor.

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

Con base en los hallazgos del análisis, se aplicaron las siguientes correcciones:

1. **Wildcard genérico en `OrderController.java`:** Se cambió el tipo de retorno del método `create()` de `ResponseEntity<?>` a `ResponseEntity<Object>`. Esto elimina el code smell Crítico, mejora la seguridad de tipos y la legibilidad del código.
   - **Commit:** Incluido en el commit de correcciones de este avance.
   - **Comparación:** Antes `ResponseEntity<?>` → Después `ResponseEntity<Object>`.

2. **Lambda en `OrderServiceTest.java`:** Se separó la invocación dentro del lambda de `assertThrows` para que solo contenga la llamada que puede lanzar la excepción, facilitando la identificación de errores en los tests.
   - **Commit:** Incluido en el commit de correcciones de este avance.
   - **Comparación:** Antes el `new OrderService()` y `s.create(...)` estaban en el mismo lambda → Después solo `s.create(...)` está dentro del lambda.

3. **Configurar JaCoCo:** Se agregó el plugin `jacoco-maven-plugin` (versión 0.8.12) al `pom.xml` raíz y a los módulos `orders-api` y `notifications-lambda`. Con los goals `prepare-agent` (instrumenta los tests) y `report` (genera `target/site/jacoco/jacoco.xml`), SonarQube ahora puede leer la cobertura real.
   - **Commit:** Incluido en el commit de configuración de JaCoCo.
   - **Impacto:** La cobertura pasará de 0% (sin reporte) a un valor real que refleje los tests existentes.

4. **Cobertura adicional:** Se decidió **no** agregar tests adicionales en este avance. La cobertura se incrementará progresivamente en sprints posteriores cuando se agreguen nuevas funcionalidades.

### Decisión

**Trade-off principal:** Corregir los 2 code smells detectados y configurar JaCoCo para obtener métricas de cobertura reales, pero dejar los tests adicionales para sprints futuros.

**Justificación:**
- Los 2 code smells son correcciones puntuales que no afectan la funcionalidad del proyecto. El wildcard genérico es un cambio de firma, y el lambda del test es un refactor menor.
- La cobertura de 0% no refleja la realidad: los 3 tests sí cubren la lógica de `OrderService`, pero sin JaCoCo configurado SonarQube no puede medir la cobertura real. Configurar JaCoCo es la prioridad para obtener datos precisos.
- Agregar tests al controller solo para subir el porcentaje de cobertura sin nueva funcionalidad de negocio sería "gaming the metric". El equipo prefiere agregar tests significativos cuando se implementen nuevas features.
- El Quality Gate pasó, lo cual valida que el código cumple con los umbrales mínimos de calidad definidos por SonarQube.

**¿Qué bloquea la integración?** El Quality Gate de SonarQube evalúa condiciones sobre *new code* (código nuevo desde la línea base). Si la cobertura de código nuevo cae por debajo del umbral configurado (por defecto 80%), el gate falla y bloquea la integración. En la edición Community, el gate solo aplica a la rama principal.

**¿Qué NO demuestra el gate?** El Quality Gate no garantiza que el código funcione correctamente ni que cumpla requisitos de negocio. Solo valida métricas estáticas (code smells, bugs, vulnerabilidades, cobertura, duplicaciones). La validación funcional requiere tests de integración y revisión humana.

**¿Qué falta revisar?** La cobertura real (con JaCoCo configurado) para validar que los tests cubren las líneas críticas de `OrderService`. También queda pendiente agregar tests para `OrderController` en sprints futuros.

**Lección aprendida:** La predicción del equipo fue parcialmente acertada: no se encontraron bugs ni vulnerabilidades como anticipamos. Sin embargo, esperábamos entre 5 y 10 code smells y solo se encontraron 2, lo que indica que el código del starter está bastante limpio. La mayor sorpresa fue la cobertura de 0% por falta de JaCoCo — los tests existen pero SonarQube no puede verlos sin el reporte.

---

## Trazabilidad

| Elemento | Referencia |
|----------|-----------|
| **Código fuente (antes)** | Commit `e36a396` — código original con code smells |
| **PR de evidencia original** | PR #21 (`feature/evidencia-u1` → `main`) — merge commit `9111c5d` |
| **Run del workflow (antes)** | Ejecutado por el merge de PR #21 — análisis sin JaCoCo (0% cobertura) |
| **Resultado Sonar (antes)** | Quality Gate: Passed, 2 code smells, 0% cobertura, 0 bugs, 0 vulnerabilidades |
| **Código fuente (después)** | Nuevo commit en `feature/evidencia-u1` con correcciones y JaCoCo |
| **PR de correcciones** | *(se actualizará al crear el nuevo PR)* |
| **Run del workflow (después)** | *(se actualizará cuando el workflow ejecute con JaCoCo)* |
| **Resultado Sonar (después)** | *(se actualizará con los resultados del nuevo análisis)* |

> **Nota sobre commits de merge en PRs:** Cuando GitHub Actions ejecuta el workflow en un evento `pull_request`, crea un commit temporal de merge (merge commit) que combina la rama del PR con `main`. Este commit temporal no aparece en el historial de la rama y es diferente del commit real del desarrollador. El análisis de SonarQube se ejecuta sobre este merge commit temporal. Por eso, el commit analizado en SonarQube puede no coincidir exactamente con el último commit de la rama del PR.

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
- Aplicación de las correcciones de code smells (`ResponseEntity<?>` → `ResponseEntity<Object>`, refactor del lambda en tests).
- Configuración de JaCoCo para reportes de cobertura y actualización del workflow.
- Capturas de pantalla del dashboard de SonarQube para la evidencia visual.

### Luciano Barceló
- Revisión de las correcciones aplicadas y verificación de que los tests siguen pasando después de los cambios.
- Redacción de la sección de Decisión y el análisis de trade-offs.
- Documentación de Javadoc en los métodos públicos del controller.
- Revisión final del documento de evidencia y validación contra el checklist del profesor.

---

## Capturas de pantalla

Las capturas del dashboard de SonarQube se encuentran en la carpeta [`capturas/`](./capturas/).

| Captura | Descripción |
|---------|-------------|
| [01-sonar-login.jpg](./capturas/01-sonar-login.jpg) | Pantalla de login del servidor SonarQube |
| [02-crear-proyecto.jpg](./capturas/02-crear-proyecto.jpg) | Creación del proyecto en SonarQube |
| [03-sonar-token.jpg](./capturas/03-sonar-token.jpg) | Generación del token de análisis (valores ocultos) |
| [04-github-secrets.jpg](./capturas/04-github-secrets.jpg) | Configuración de secrets en GitHub Actions |
| [05-dashboard-overview.jpg](./capturas/05-dashboard-overview.jpg) | Dashboard general del proyecto — Quality Gate, métricas |
| [06-code-smells.jpg](./capturas/06-code-smells.jpg) | Detalle de los 2 code smells detectados |
| [07-coverage.jpg](./capturas/07-coverage.jpg) | Reporte de cobertura (0% antes de JaCoCo) |

---

## Limitaciones e IA

### Limitaciones del plan

- **SonarQube Community Edition 9.9.8** no soporta análisis de ramas ni de Pull Requests. Solo puede analizar la rama principal (`main`). Para decoración de PRs y análisis por rama se requiere Developer Edition o superior.
- **Bloqueo de merge por Quality Gate:** La configuración de branch protection rules en GitHub para exigir que el Quality Gate pase antes de hacer merge requiere la integración de SonarQube con GitHub Checks, que no está disponible en Community Edition. Se documenta como limitación; no se afirma que el merge está bloqueado.
- **Webhook de SonarQube:** Para usar `sonarqube-quality-gate-action` se necesita un webhook configurado en el servidor SonarQube. Esta funcionalidad queda pendiente de configuración.

### Uso de IA

- **Herramienta:** Asistente de IA (Antigravity IDE / Claude)
- **Prompts relevantes:**
  1. Revisión de gramática y checklist contra las instrucciones del profesor.
  2. Consulta sobre la mejor estrategia de almacenamiento para priorizar la reproducibilidad en el Sprint 0.
  3. Configuración de JaCoCo en proyecto multi-módulo Maven para integración con SonarQube.
  4. Corrección de code smells reportados por SonarQube (wildcard genérico y lambda compuesto).
  5. Estructura del documento de evidencia según los criterios del PDF del avance.
- **Qué verificamos/cambiamos:**
  - Confirmamos que JaCoCo genera el XML de cobertura en `target/site/jacoco/jacoco.xml` y que SonarQube lo lee correctamente.
  - Verificamos que las correcciones de code smells no rompen los tests existentes (`mvn clean verify` pasa).
  - Revisamos que el workflow tiene los parámetros correctos para enviar cobertura a SonarQube.
  - Cada integrante revisó y entiende los cambios para poder defenderlos en la demostración.

---

## Mini Definition of Done

- [x] Estructura de carpetas creada (`docs/avance-u1/capturas/`)
- [x] Evidencia con las 4 secciones del reporte (Predicción, Observación, Corrección, Decisión)
- [x] Sección de Identificación con equipo, repo, proyecto Sonar y commit
- [x] Sección de Flujo explicando evento, runner, steps y función de Sonar
- [x] Contribuciones individuales de los 3 integrantes documentadas
- [x] Workflow `calidad.yml` creado y funcional con JaCoCo
- [x] README actualizado con documentación de SonarQube
- [x] JaCoCo configurado para reportes de cobertura
- [x] Code smells corregidos (wildcard genérico + lambda)
- [x] Trazabilidad: cambio → commit → PR → run → análisis Sonar
- [x] Limitaciones e IA declaradas
- [x] Capturas de pantalla referenciadas
