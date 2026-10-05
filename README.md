# 💳 NovaPay Backend — Hexagonal Architecture & Verifactu AEAT Integration

🇺🇸 [Read in English](README.en.md) | 🇪🇸 **Español**

[![Java Version](https://img.shields.io/badge/Java-21-orange.svg)](https://www.oracle.com/java/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.4.3-brightgreen.svg)](https://spring.io/projects/spring-boot)
[![Architecture](https://img.shields.io/badge/Architecture-Hexagonal%20%2F%20Ports%20%26%20Adapters-blue.svg)](#arquitectura-del-sistema)
[![License](https://img.shields.io/badge/License-MIT-green.svg)](LICENSE)

> **Caso de Estudio / Portafolio Técnico**: Backend enterprise en **Java 21** y **Spring Boot 3** diseñado bajo **Arquitectura Hexagonal (Puertos y Adaptadores)** y **Domain-Driven Design (DDD)**. Implementa la normativa de facturación electrónica **Verifactu (AEAT)** con encadenamiento criptográfico de registros (SHA-256), firma digital XMLDSig con certificados PKCS#12 y autenticación JWT.

---

## 📌 Resumen Ejecutivo

**NovaPay Backend** es el motor central de procesamiento fiscal y facturación electrónica simplificada y completa diseñado para integrarse con terminales TPV (ej. Flutter/Mobile) y sistemas de gestión comercial.

El sistema garantiza cumplimiento normativo estricto con el **Real Decreto 1007/2023 (Reglamento Verifactu / AEAT)**:
1. **Inmutabilidad y Cadenas de Huellas**: Generación de hashes SHA-256 encadenados por emisor/empresa.
2. **Firma Digital Avanzada**: Firma de mensajes SOAP/XML mediante estándar XAdES/XMLDSig (RSA-SHA256) usando almacén de claves PKCS#12.
3. **Comunicación Directa con la AEAT**: Cliente SOAP mTLS contra los webservices de la Agencia Tributaria.
4. **Verificación mediante QR**: Código Seguro de Verificación (CSV) y generación de URLs de cotejo fiscal.

---

## 🏛️ Arquitectura del Sistema (Puertos y Adaptadores)

El proyecto sigue una estricta separación de responsabilidades en tres capas fundamentales:

```
                          ┌──────────────────────────────────────────────────────────┐
                          │                      CAPA INBOUND                        │
                          │   (REST Controllers / OpenAPI / DTOs / Security JWT)    │
                          └────────────────────────────┬─────────────────────────────┘
                                                       │
                                                       ▼  (Driving Ports)
┌─────────────────────────────────────────────────────────────────────────────────────────────────────────┐
│                                             CAPA DE DOMINIO                                             │
│                                                                                                         │
│  ┌────────────────────────┐         ┌────────────────────────┐         ┌─────────────────────────────┐  │
│  │    Modelos & Value     │         │   Casos de Uso / App   │         │    Enums / Excepciones de   │  │
│  │    Objects (Money,     │ <------ │   Services (Emit,      │ <------ │    Dominio (InvoiceType,     │  │
│  │    Invoice, Record)    │         │   Retry, Verification) │         │    TaxType, Status)         │  │
│  └────────────────────────┘         └────────────────────────┘         └─────────────────────────────┘  │
└──────────────────────────────────────────────────────┬──────────────────────────────────────────────────┘
                                                       │  (Driven Ports)
                                                       ▼
                          ┌──────────────────────────────────────────────────────────┐
                          │                      CAPA OUTBOUND                       │
                          │  - Adaptador Verifactu SOAP (AEAT WebService Client)     │
                          │  - Adaptador de Firma XML (Apache Santuario / PKCS#12)   │
                          │  - Adaptador de Persistencia JPA (PostgreSQL + Flyway)   │
                          │  - Adaptador de Encriptación / Hashing (SHA-256)        │
                          └──────────────────────────────────────────────────────────┘
```

### Estructura de Directorios

```
src/main/java/levelup42/novapay_backend_hex/
├── domain/                               # Núcleo de negocio (sin dependencias de frameworks)
│   ├── model/                            # Entidades de dominio y Value Objects (Money, Invoice, FiscalRecord)
│   ├── port/
│   │   ├── in/                           # Puertos primarios (Casos de uso: InvoiceCreateUseCase, etc.)
│   │   └── out/                          # Puertos secundarios (FiscalAgencyPort, XmlSignerPort, Repositories)
│   └── service/                          # Lógica de dominio pura y servicios de cálculo
├── application/                          # Orquestación de casos de uso (EmitInvoiceService, RetryService)
└── infrastructure/                       # Detalles técnicos y adaptadores
    ├── adapter/
    │   ├── in/rest/                      # Controladores REST, DTOs y Mappers (MapStruct/Jackson)
    │   └── out/
    │       ├── fiscal/verifactu/         # Cliente SOAP AEAT, XML Builder y Parser
    │       ├── persistence/              # Repositorios Spring Data JPA y Entidades
    │       └── signing/                  # Firma digital XMLDSig / PKCS#12
    ├── config/                           # SecurityConfig, GlobalExceptionHandler (RFC 7807), OpenApiConfig
    └── security/jwt/                     # Proveedor de tokens JWT y Filtros de Autorización
```

---

## 🔒 Sanitización y DevSecOps (Versión Pública)

Esta versión ha sido completamente sanitizada conforme a estándares corporativos de DevSecOps:
- 🛡️ **Cero Secretos Hardcodeados**: Todas las claves privadas, credenciales de base de datos y secretos JWT utilizan inyección por variables de entorno con fallbacks genéricos para desarrollo local.
- 🔑 **Gestión de Certificados**: Sin certificados reales en repositorio. Se incluyen scripts para generar keystores PKCS#12 auto-firmados de pruebas (`test-keystore.p12`).
- 📄 **Datos Sensibles**: Todos los NIFs, razones sociales y URLs de producción han sido sustituidos por placeholders RFC oficiales (ej. `12345678Z`, `EMPRESA DE PRUEBAS S.L.`, `https://api.example.com`).
- 🚫 **.gitignore Robusto**: Bloqueo estricto de archivos `.p12`, `.key`, `.env`, logs y artefactos temporales.

---

## 🚀 Guía de Inicio Rápido (Desarrollo Local)

### Requisitos Previos

- **Java 21 JDK** u homogéneo.
- **Docker Desktop** (para la base de datos PostgreSQL local).
- **Maven 3.9+** (o el script `./mvnw` incluido).

### 1. Clonar el Repositorio
```bash
git clone https://github.com/tu-usuario/novapay-backend-public.git
cd novapay-backend-public
```

### 2. Configurar Variables de Entorno
Copia la plantilla de entorno:
```bash
cp .env.example .env
```

### 3. Generar Certificado PKCS#12 Dummy de Pruebas
Ejecuta el script de generación de keystore de pruebas:

**En Linux / macOS (Bash):**
```bash
chmod +x scripts/generate-dummy-cert.sh
./scripts/generate-dummy-cert.sh
```

**En Windows (PowerShell):**
```powershell
.\scripts\generate-dummy-cert.ps1
```

### 4. Levantar la Base de Datos PostgreSQL con Docker
```bash
docker compose up -d
```
> Flyway ejecutará automáticamente todas las migraciones SQL al arrancar la aplicación (`V2` a `V7`).

### 5. Compilar y Ejecutar el Backend
```bash
./mvnw spring-boot:run
```

El servidor iniciará en `http://localhost:8080`.

---

## 📖 Documentación de la API (OpenAPI / Swagger)

Una vez iniciada la aplicación, la documentación interactiva Swagger UI está disponible en:
- **Swagger UI**: [http://localhost:8080/swagger-ui.html](http://localhost:8080/swagger-ui.html)
- **OpenAPI JSON Spec**: `http://localhost:8080/v3/api-docs`

### Principales Endpoints REST

| Método | Endpoint | Descripción | Autenticación |
|---|---|---|---|
| `POST` | `/api/v1/auth/token` | Generación de Token JWT para clientes TPV | No (Pública con Client ID/Secret) |
| `POST` | `/api/v1/invoices` | Emisión y envío de factura (Simplificada/Completa) | `Bearer JWT` |
| `POST` | `/api/v1/invoices/{id}/cancel` | Anulación de factura emitida | `Bearer JWT` |
| `GET` | `/api/v1/fiscal/status/{invoiceId}` | Consulta del estado fiscal AEAT (CSV, QR URL) | `Bearer JWT` |
| `POST` | `/api/v1/fiscal/retry/{invoiceId}` | Reintento manual de envío pendiente/fallido | `Bearer JWT` |

---

## 🧪 Pruebas Automatizadas

El proyecto cuenta con una suite completa de pruebas unitarias e integración con **JUnit 5** y **Mockito**:

```bash
# Ejecutar todas las pruebas unitarias
./mvnw test "-Dtest=VerifactuXmlBuilderTest,XmlSignerImplTest,GlobalExceptionHandlerTest,JwtTokenProviderTest,EmitInvoiceServiceTest,FiscalEvidenceServiceTest,RetryFiscalSubmissionServiceTest,HashServiceImplTest"

# Ejecutar test de envío E2E a entorno de homologación AEAT (Requiere cert de prueba)
./mvnw test -Dtest=VerifactuRealE2ETest
```

---

## 🐳 Despliegue en Producción (Docker Server)

En el directorio `/deploy` se incluyen las plantillas preparadas para entornos de servidor:
- `docker-compose.server.yml`: Pila de producción con Nginx Proxy y PostgreSQL.
- `nginx.conf`: Configuración de proxy inverso con cabeceras `X-Forwarded-*`.
- `.env.server.example`: Plantilla de producción aislada.
- `DEPLOY_SERVER_STEP_BY_STEP.md`: Guía de despliegue paso a paso.

---

## 📜 Licencia

Este proyecto está bajo la Licencia MIT. Consulta el archivo [LICENSE](LICENSE) para más detalles.
