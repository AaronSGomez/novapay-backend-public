# Pruebas Realizadas y Ejecución de Tests

Este documento contiene la lista de comprobaciones que hemos realizado en el sistema, junto con los comandos necesarios para ejecutar los distintos tests del proyecto Spring Boot.

## 🛠️ Comandos para ejecutar los Tests

El proyecto utiliza Maven Wrapper (`mvnw`). Para ejecutar los tests de forma local, abre una terminal en la raíz del proyecto y utiliza los siguientes comandos:

### 1. Ejecutar todos los tests (Unitarios y de Integración)
```powershell
.\mvnw.cmd clean test
```

### 2. Ejecutar una clase de test en específico
```powershell
# Ejemplo: Ejecutar solo el AuthFlowIntegrationTest
.\mvnw.cmd test -Dtest=AuthFlowIntegrationTest

# Ejemplo: Ejecutar solo los tests unitarios de un controlador
.\mvnw.cmd test -Dtest=AuthControllerTest
```

### 3. Ejecutar tests con logs detallados (DEBUG)
Si necesitas ver qué está enviando / recibiendo exactamente la aplicación, puedes aumentar el nivel de log durante el test:
```powershell
.\mvnw.cmd test -Dlogging.level.levelup42.novapay_backend_hex=DEBUG
```

---

## ✅ Lista de Comprobaciones Realizadas

Durante nuestras sesiones de depuración y desarrollo, hemos verificado y estabilizado los siguientes componentes clave del flujo del servidor:

### 1. Resolución del Arranque de Spring Boot y Flyway
- **Problema encontrado:** La base de datos de Docker contenía tablas creadas manualmente (`api_clients`, `api_client_roles`) que provocaban un fallo al intentar ejecutar los scripts iniciales de migración de Flyway (`V2__create_api_clients.sql`).
- **Solución Aplicada:** Se realizó una limpieza de las tablas residuales en PostgreSQL para permitir que Flyway construya el esquema correctamente desde cero.
- **Resultado:** La aplicación ahora arranca con éxito en el puerto 8080 sin conflictos de esquema inicial.

### 2. Carga automática de Variables de Entorno (`.env`)
- **Mejora implementada:** Se ha configurado `application.yml` con la propiedad `spring.config.import: optional:file:.env[.properties]`.
- **Resultado:** Las variables globales del sistema, conexión a base de datos y la clave `JWT_SECRET` se leen automáticamente del fichero `.env` al iniciar la app desde el IDE (p. ej. usando Tomcat embedido), evitando errores intermitentes por falta de configuración de parámetros de entorno. Se ha dejado un token JWT de respaldo (`fallback`) en el `.yml` en caso de que alguien inicie el proyecto de cero sin `.env`.

### 3. Verificación de la Capa de Persistencia (Base de Datos)
- **Componente probado:** `ApiClientRepositoryIntegrationTest`
- **Comprobaciones:**
  - Verificada la conexión de la aplicación con la base de datos PostgreSQL real desplegada en el contenedor Docker.
  - Comprobado que Hibernate y Spring Data JPA mapean correctamente la entidad `ApiClientEntity`.
  - Acceso, guardado y lectura de variables de base de datos desde el entorno de contexto completo de Spring (`@SpringBootTest`).

### 4. Verificación del Flujo de Autenticación y Seguridad (JWT)
- **Componente probado:** `AuthFlowIntegrationTest`
- **Comprobaciones e iteraciones:**
  - **Mapeo JSON (Jackson):** Inclusión de anotaciones `@JsonProperty` en los DTOs `AuthRequest` y `AuthResponse` para asegurar su serialización / deserialización correcta, parcheando un error HTTP 401 oculto debido a records Java crudos.
  - **Endpoints:** Ajuste del endpoint mapeado en la prueba hacia la ruta protegida real: `/api/v1/auth/token`.
  - **BCrypt:** Simulación de una contraseña pre-hasheada durante la inyección en base de datos para garantizar la consistencia en el test, evitando problemas con los datos estáticos inyectados por Flyway.
  - **Verificación de Filtro:** Se valida que Spring Security e interceptor de JWT actúen adecuadamente.
- **Resultado:** El endpoint recibe las credenciales, devuelve código HTTP 200 OK y entrega un token de acceso (`accessToken`) válido con la firma HMAC correcta y los Claims apropiados para el usuario subyacente.

---
**🏆 Estado Actual:** El 100% de la suite de tests automatizados (Unitarios + Integración) pasa correctamente.
