# Sprint 0 Evidence

## Sprint Goal
Configurar el repositorio del proyecto, verificar que se pueda clonar y ejecutar sin errores, y documentar el flujo actual (AS-IS) y el propuesto (TO-BE).

## Repository baseline
- Repo: https://github.com/SebastianMorenoV/orderflow-starter
- Initial commit: `a5b378e` — "Initial commit"
- Fresh clone verified by: Integrantes del equipo (Sebastian Moreno, Benjamin Soto, Luciano Barcelo)

## Baseline execution
- Tests: `mvn clean test` pasó correctamente — 3 tests, 0 errores.
- Artifact: `mvn package` generó los JARs sin problemas (`orders-api-0.1.0-SNAPSHOT.jar`, `notifications-lambda-0.1.0-SNAPSHOT.jar`).
- Endpoint: `http://localhost:8080/actuator/health` respondió con `{"status":"UP"}`. Se probaron los endpoints de pedidos con Postman y funcionaron bien.

## Value Stream
- AS-IS: `docs/value-stream/as-is.md`
- TO-BE: `docs/value-stream/to-be.md`

## Incremento demostrable
El repositorio quedó configurado en GitHub con acceso para todos los integrantes. Se verificó que al clonarlo desde cero, el proyecto compila y las pruebas pasan sin errores. La API funciona correctamente en local.

## PR / pipeline / deployment / infraestructura
N/A — todavía no corresponde a este Sprint.

## Decisión y trade-off
Se decidió usar el almacenamiento en memoria que ya trae el proyecto en vez de conectar una base de datos. Esto hace que los pedidos se pierdan al reiniciar la app, pero a cambio cualquier integrante puede correr el proyecto sin instalar nada extra. Para Sprint 0 es lo mejor porque el objetivo era que todo funcionara fácil desde un clone limpio.

## Demo mínima reproducible
Desde una terminal limpia se puede clonar el repo, correr `mvn clean test` para ver que las 3 pruebas pasan, luego `mvn package` para generar los JARs, y finalmente `mvn -pl orders-api spring-boot:run` para levantar la API. Se verifica que funciona abriendo `http://localhost:8080/actuator/health` en el navegador o creando un pedido con Postman en `POST /api/orders`.

## Contribuciones del equipo

- **SebastianMorenoV** — Creación del repositorio, import del baseline, formateo del código fuente, documentación del proyecto, verificación de builds y configuración inicial.
- **Benjaminsc** — Verificación de reproducibilidad (fresh clone), pruebas de API, documentación del Value Stream y Working Agreement.
- **Luciano Barceló** — Comprobación de que el código corre bien desde cero, revisión de la API y apoyo en la redacción de los documentos del equipo

## Mini Definition of Done
- [x] Repo y commit baseline identificables
- [x] Fresh clone verificado
- [x] Build/tests, artifact y endpoint reproducibles
- [x] AS-IS, TO-BE y Working Agreement completos
- [x] Decisión/trade-off defendible 

## Retro: Keep / Change / Next experiment
- **Keep:** El equipo se organizó rápido para configurar el repositorio y repartir tareas.
- **Change:** Revisar que todos tengan Git y Java instalados antes de empezar, para no perder tiempo.
- **Next:** Buscar una forma de que el formato del código sea igual para todos.

## Uso de IA
- **Herramienta:** Asistente de IA 
- **Prompts relevantes:** 
  1. Revisión de gramática y checklist contra las instrucciones del profesor.
  2. Consulta sobre la mejor estrategia de almacenamiento para priorizar la reproducibilidad en el Sprint 0.
- **Qué verificamos/cambiamos:** 
  - Aplicamos las correcciones ortográficas y de estilo, confirmando que el archivo incluye todos los puntos (hash, evidencias, etc.).
  - Tomamos la recomendación de la IA para nuestro Trade-off: confirmamos que usar la memoria interna es la mejor decisión para este sprint. Aunque los datos no persistan, nos asegura cumplir con el objetivo principal de que los comandos `mvn clean test` y `mvn package` funcionen a la primera en cualquier computadora sin instalaciones extra.