# Guía de defensa — Demostración en vivo (Avance Unidad 1)

> Este documento prepara al equipo para la demostración en vivo del avance. Cada integrante debe poder explicar cualquier punto y modificar código en vivo.

---

## Secuencia de demostración

### 1. Mostrar el workflow y explicar evento, runner y steps

**Abrir:** `.github/workflows/calidad.yml`

**Puntos clave:**
- **Evento:** Se dispara con `push` a `main` y con `pull_request` hacia `main`. Esto significa que cada vez que alguien hace push directo a main o abre/actualiza un PR, el workflow se ejecuta automáticamente.
- **Runner:** `ubuntu-latest` — máquina virtual de GitHub con Ubuntu, proporcionada por GitHub Actions. No necesitamos infraestructura propia para correr el pipeline.
- **Steps explicados:**
  1. `actions/checkout@v4` — Clona el repositorio en el runner. `fetch-depth: 0` trae todo el historial para que Sonar pueda hacer análisis incremental (comparar código nuevo vs existente).
  2. `actions/setup-java@v4` — Instala Java 21 (Temurin) y configura cache de Maven para que no descargue dependencias cada vez.
  3. `actions/cache@v4` — Cachea los datos de SonarQube (`~/.sonar/cache`) para acelerar análisis subsecuentes.
  4. `mvn -B clean verify` — Compila el código, ejecuta los tests unitarios y genera el reporte de cobertura con JaCoCo (el plugin está configurado en el `pom.xml`). `-B` es modo batch (sin interacción).
  5. `mvn -B sonar:sonar` — Invoca el SonarScanner for Maven. Envía el código fuente y los reportes de JaCoCo al servidor SonarQube para el análisis estático.

**Preguntas frecuentes:**
- *¿Qué es `uses`?* — Referencia a una GitHub Action reutilizable publicada en el marketplace. `actions/checkout@v4` es la versión 4 de la action oficial de checkout.
- *¿Qué es `run`?* — Ejecuta un comando de shell directamente en el runner.
- *¿Qué pasa si el build falla?* — El step de SonarQube no se ejecuta porque los steps son secuenciales y un fallo detiene la ejecución.
- *¿Qué es `working-directory`?* — Cambia el directorio de trabajo al subdirectorio `orderflow-starter` porque el proyecto Maven está ahí, no en la raíz del repo.

---

### 2. Mostrar el baseline de main y un PR del equipo

**Acciones:**
1. Ir a la pestaña **Actions** del repositorio en GitHub.
2. Mostrar las ejecuciones del workflow en `main` (push events).
3. Mostrar el PR #21 como ejemplo de PR con ejecución asociada.
4. Señalar que el workflow se ejecutó tanto en el push como en el PR.

**Punto clave:** Cuando se hace un PR, GitHub Actions crea un commit temporal de merge entre la rama del PR y `main`. El análisis se ejecuta sobre ese merge temporal. Por eso el commit hash en el run puede diferir del último commit de la rama.

---

### 3. Presentar un fallo controlado e identificar su causa

**Opciones de fallo para demostrar:**

**Opción A — Test que falla:**
```java
// En OrderServiceTest.java, agregar temporalmente:
@Test
void failsOnPurpose() {
    var s = new OrderService();
    var o = s.create("test", new BigDecimal("100"));
    assertEquals(OrderStatus.SHIPPED, o.status()); // Falla: espera SHIPPED pero es CREATED
}
```
- El step "Build, tests y cobertura" falla.
- El log muestra: `AssertionFailedError: expected SHIPPED but was CREATED`.
- El step de SonarQube NO se ejecuta porque el build falló primero.

**Opción B — Code smell que rompe el gate (si el gate tiene umbral estricto):**
- Agregar código con un code smell intencional y verificar si el Quality Gate cambia.
- Nota: No todos los issues provocan un gate rojo. Depende de los umbrales configurados.

**Opción C — Error de configuración:**
- Cambiar el `SONAR_HOST_URL` a una URL incorrecta y mostrar el error de conexión en el log del step de SonarQube.

---

### 4. Mostrar la corrección, nuevo commit y nueva ejecución

**Acciones:**
1. Mostrar el código antes de la corrección (commit `e36a396`):
   - `OrderController.java` con `ResponseEntity<?>` (line 27)
   - `OrderServiceTest.java` con lambda compuesto (line 20)
2. Mostrar el código después de la corrección (nuevo commit):
   - `ResponseEntity<Object>` — elimina wildcard genérico
   - Lambda separado — solo una invocación que puede lanzar excepción
3. Mostrar el nuevo run del workflow con las correcciones.
4. Verificar que los tests siguen pasando.

**Razón técnica de cada corrección:**
- **Wildcard genérico:** Java permite `ResponseEntity<?>` pero SonarQube lo marca como code smell porque los consumidores del API no pueden hacer cast seguro del body. `Object` es explícito y permite que Spring serialice correctamente tanto `Order` como `String` (mensaje de error).
- **Lambda compuesto:** Si el lambda tiene `new OrderService()` + `s.create(...)`, y `new OrderService()` falla por alguna razón, `assertThrows` captura esa excepción y el test pasa incorrectamente. Separando la creación, solo `s.create(...)` está dentro del `assertThrows`.

---

### 5. Abrir el resultado Sonar y explicar el estado del gate

**Acciones:**
1. Abrir el dashboard de SonarQube en el navegador.
2. Navegar al proyecto `orderflow-starter`.
3. Mostrar el Quality Gate y su estado (Passed/Failed).

**Explicación del Quality Gate:**
- **Estado:** Passed — el código cumple todos los umbrales configurados.
- **Condiciones del gate (default en SonarQube 9.9):**
  - Cobertura en código nuevo ≥ 80%
  - Duplicaciones en código nuevo ≤ 3%
  - Rating de Reliability en código nuevo = A (0 bugs)
  - Rating de Security en código nuevo = A (0 vulnerabilidades)
  - Rating de Maintainability en código nuevo = A
- **Alcance:** El Quality Gate por defecto evalúa **New Code** (código nuevo desde la línea base definida, normalmente la versión anterior). El **Overall Code** muestra las métricas acumuladas de todo el proyecto pero no determina si el gate pasa o falla.

**¿Qué bloquea la integración?**
- Si cualquier condición del gate no se cumple (por ejemplo, cobertura < 80% en código nuevo), el gate cambia a **Failed**. En un flujo con branch protection, esto impediría el merge del PR.
- **Limitación:** En Community Edition no podemos configurar el check de SonarQube como requisito en las branch protection rules de GitHub. Esto requiere Developer Edition.

**¿Qué NO bloquea el gate?**
- Code smells existentes en Overall Code (solo aplica a New Code).
- Deuda técnica acumulada (solo se evalúa el incremento).
- Corrección funcional del código (el gate solo mide métricas estáticas).

---

### 6. Defender la decisión de integración y demostrar el bloqueo

**Decisión:** Integrar las correcciones a `main` porque:
1. El Quality Gate pasó.
2. Los 2 code smells fueron corregidos.
3. JaCoCo fue configurado para obtener cobertura real.
4. Los tests existentes siguen pasando sin errores.

**Sobre el bloqueo:**
- En SonarQube Community Edition 9.9.8, **no es posible** configurar bloqueo automático de merge basado en el Quality Gate. Esta funcionalidad requiere la integración con GitHub Checks que solo está disponible en Developer Edition.
- Lo que sí se puede hacer: revisar manualmente el estado del Quality Gate antes de aprobar el PR.
- **No afirmamos que el merge está bloqueado.** Documentamos la limitación y explicamos qué configuración faltaría (Developer Edition + GitHub App de SonarQube + branch protection rule con required status check).

---

## Preguntas frecuentes por integrante

### Para Sebastian Moreno (Configuración de Sonar y workflow)
- *¿Cómo configuraste el proyecto en SonarQube?* — En el dashboard de SonarQube: Projects → Create project manually → project key: `orderflow-starter`. Luego generar token en Administration → Security.
- *¿Qué hace `fetch-depth: 0`?* — Clona todo el historial de git. Sin esto, Sonar no puede comparar el código nuevo contra el existente para el análisis incremental.
- *¿Por qué se usa `sonar.token` y no `sonar.login`?* — `sonar.login` está deprecado desde SonarQube 9.x. `sonar.token` es el parámetro recomendado.
- *¿Cómo cambiarías el step de build si quisieras saltar los tests?* — Cambiar `mvn -B clean verify` a `mvn -B clean verify -DskipTests`. Pero no lo haríamos porque sin tests, JaCoCo no genera cobertura.

### Para Benjamin Soto (Análisis y correcciones)
- *¿Por qué la cobertura era 0% si los tests pasaban?* — Porque SonarQube no genera cobertura por sí solo. Necesita un reporte XML generado por una herramienta externa (JaCoCo para Java). Sin el plugin JaCoCo configurado, no había reporte que leer.
- *¿Qué es JaCoCo?* — Java Code Coverage. Es un agente que se instrumenta durante la ejecución de tests para rastrear qué líneas de código se ejecutaron. Genera un XML que SonarQube importa.
- *¿Por qué `ResponseEntity<Object>` y no `ResponseEntity<Order>`?* — Porque el método puede devolver tanto un `Order` (caso exitoso) como un `String` con el mensaje de error (caso de excepción). `Object` es el supertipo común.
- *¿Cómo agregarías un test para el controller?* — Usando `@WebMvcTest` de Spring con `MockMvc` para simular peticiones HTTP sin levantar el servidor completo.

### Para Luciano Barceló (Revisión y decisiones)
- *¿Qué diferencia hay entre New Code y Overall Code?* — New Code es el código agregado desde la línea base (definida por la configuración del proyecto). Overall Code es todo el código del proyecto. El Quality Gate evalúa New Code para no penalizar código legacy.
- *¿El Quality Gate garantiza que el código funciona?* — No. Solo valida métricas estáticas (code smells, bugs potenciales, cobertura, duplicaciones). La validación funcional requiere tests de integración, revisión manual y pruebas de aceptación.
- *¿Qué harían si el gate falla por cobertura?* — Analizar qué código nuevo no tiene tests y decidir: agregar tests para las líneas no cubiertas (si son lógica crítica) o ajustar el umbral del gate si la línea base no es apropiada. No eliminar controles sin justificación.
- *¿Qué limitaciones tiene SonarQube Community?* — No soporta análisis de ramas ni de PRs (solo la rama principal), no tiene integración con GitHub Checks para bloqueo automático, no soporta OWASP/SANS security reports. Para eso se necesita Developer o Enterprise Edition.

---

## Checklist pre-demostración

- [ ] El repositorio es accesible para el profesor
- [ ] El workflow ejecuta correctamente (verificar en Actions)
- [ ] Las capturas no exponen tokens ni secretos
- [ ] Cada integrante puede explicar `uses`, `run`, `runner` y Quality Gate
- [ ] Se puede mostrar un diff antes/después de la corrección
- [ ] Se tiene preparado un fallo controlado para demostrar
- [ ] Los pendientes están declarados (no se presentan como completados)
- [ ] El documento de evidencia tiene todos los campos requeridos
