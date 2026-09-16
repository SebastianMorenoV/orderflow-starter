# OrderFlow — Student Starter

OrderFlow funciona localmente, pero deliberadamente NO contiene el delivery system del curso.

## ¿De qué trata el sistema?

OrderFlow simula el procesamiento de pedidos de una empresa. Su API permite registrar pedidos asociados con un cliente, consultar los pedidos existentes y verificar el estado de salud del servicio. También incluye un componente de notificaciones que, más adelante, se preparará para ejecutarse como una función AWS Lambda.

La funcionalidad de negocio inicial es deliberadamente pequeña porque el propósito del proyecto no es construir una tienda completa. Durante el semestre, el equipo transformará la manera en que OrderFlow se integra, prueba, empaqueta, entrega, despliega, aprovisiona y observa mediante prácticas DevOps reproducibles.

## Requisitos

- Java 21
- Maven 3.9+
- Git

Más adelante: Docker, AWS CLI, Terraform, Minikube, kubectl y Kompose.

## Bootstrap del repositorio del equipo

Cada equipo crea en la Sesión 1 su propio repositorio GitHub, por ejemplo `orderflow-equipo-03`. Ese repositorio será la **source of truth** durante todo el semestre.

Importen el starter y creen el baseline:

```bash
git init
git add .
git commit -m "chore: import OrderFlow baseline"
git branch -M main
git remote add origin <URL>
git push -u origin main
```

Otro integrante verifica la reproducibilidad desde un fresh clone:

```bash
git clone <URL>
cd <repo>
git log --oneline -1
mvn clean test
mvn package
```

Branching formal inicia en Semana 2. Pull Requests, Code Review y quality gates inician en Semana 3.

## Baseline

```bash
mvn clean test
mvn package
mvn -pl orders-api spring-boot:run
```

Prueba:

```bash
curl http://localhost:8080/actuator/health
Invoke-RestMethod -Uri "http://localhost:8080/api/orders" -Method Post -ContentType "application/json" -Body '{"customerId":"team-demo","total":150.00}'
curl http://localhost:8080/api/orders
```

## Evidencia acumulativa

No sobrescriban evidencias anteriores. Cada Sprint conserva su propio archivo:

```text
docs/evidence/
├── sprint-00.md
├── sprint-01.md
├── sprint-02.md
├── sprint-03.md
├── sprint-04.md
├── sprint-05.md
├── sprint-06.md
└── sprint-07.md
```

Usen PR, pipeline, deployment o infraestructura sólo cuando ya correspondan al Sprint. Antes de eso registren: `N/A — todavía no corresponde a este Sprint.`

## Análisis de calidad con SonarQube

El proyecto utiliza **SonarQube Community Edition 9.9.8 LTS** (self-hosted en AWS EC2) como herramienta de análisis estático de código. SonarQube inspecciona el código fuente en busca de:

- **Code Smells** — malas prácticas y deuda técnica
- **Bugs** — errores potenciales detectados por análisis estático
- **Vulnerabilidades** — posibles problemas de seguridad
- **Cobertura de tests** — porcentaje del código cubierto por pruebas unitarias (requiere JaCoCo)
- **Duplicaciones** — bloques de código repetidos

El análisis se ejecuta automáticamente en cada push a `main` y en cada Pull Request mediante el workflow [`calidad.yml`](.github/workflows/calidad.yml).

### Cobertura con JaCoCo

El proyecto usa **JaCoCo** (Java Code Coverage, versión 0.8.12) para generar los reportes de cobertura que SonarQube importa. JaCoCo está configurado en el `pom.xml` de cada módulo con dos goals:

- `prepare-agent` — instrumenta los tests para rastrear líneas ejecutadas.
- `report` — genera el archivo `target/site/jacoco/jacoco.xml` durante la fase `verify`.

La cobertura se genera automáticamente al ejecutar `mvn clean verify`.

### Secrets requeridos en GitHub

Para que el workflow de calidad funcione, se deben configurar los siguientes secrets en **Settings → Secrets and variables → Actions** del repositorio:

| Secret | Descripción |
|--------|-------------|
| `SONAR_TOKEN` | Token de autenticación generado en SonarQube (Project → Administration → Security) |
| `SONAR_HOST_URL` | URL del servidor SonarQube (ejemplo: `http://sonarqube.example.com:9000`) |

> **Nota:** No incluir valores de tokens en el código ni en documentación pública.

### Comandos de análisis local

Para ejecutar el análisis de SonarQube desde la línea de comandos local:

```bash
# 1. Build, tests y cobertura
mvn clean verify

# 2. Análisis de Sonar (reemplazar los valores)
mvn sonar:sonar \
  -Dsonar.projectKey=orderflow-starter \
  -Dsonar.host.url=<URL_DEL_SERVIDOR> \
  -Dsonar.token=<TU_TOKEN>
```

### Limitaciones de la edición Community

- No soporta análisis de ramas ni de Pull Requests (solo rama principal).
- No tiene integración con GitHub Checks para bloqueo automático de merge.
- Para estas funcionalidades se requiere Developer Edition o superior.

## Regla del semestre

No implementen por adelantado `.github/workflows`, `delivery`, `infra`, `k8s` u `observability`. Esas carpetas se desarrollan progresivamente como evidencia de aprendizaje.

