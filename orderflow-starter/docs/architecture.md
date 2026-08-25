# Arquitectura baseline

- `orders-api`: Spring Boot REST API, almacenamiento in-memory y health endpoint.
- `notifications-lambda`: lógica Java sin integración AWS inicial.

El dominio se mantiene pequeño a propósito. La complejidad evaluada está en el delivery system.
