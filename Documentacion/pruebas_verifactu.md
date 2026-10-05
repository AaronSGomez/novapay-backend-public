# Plan de Pruebas de Integración Completa: Verifactu (AEAT)

Este documento detalla los pasos, requisitos y comprobaciones necesarias para validar el flujo completo de envío de facturas al sistema Verifactu de la Agencia Tributaria (AEAT), incluyendo la respuesta oficial, antes de proceder con el despliegue del backend en el servidor de producción.

## 1. Requisitos Previos para la Prueba

Antes de ejecutar la prueba de integración contra el entorno de la AEAT, es imprescindible confirmar que los siguientes componentes técnicos están configurados correctamente:

- **Certificado Digital (mTLS):**
  - Disponer de un certificado digital válido (formato `.p12` o `.pfx`).
  - Configurar las variables de entorno asociadas en el archivo `.env`:
    - `CERT_P12_BASE64`: El contenido del certificado codificado en Base64.
    - `CERT_PASSWORD`: La contraseña del certificado.
    - `CERT_ALIAS`: El alias del certificado.
  - Implementación completa del `Contexto SSL/TLS` en el cliente HTTP/SOAP de Spring Boot (`VerifactuSoapClient.java`) para soportar *Mutual TLS* (mTLS), que es mandatorio por la AEAT.

- **Firma XML (XMLDSig):**
  - Asegurar que el Payload XML (Alta de Factura) lleva la firma en formato XAdES exigida por normativa.

- **Conectividad de Red:**
  - Asegurar que el endpoint de pruebas de la AEAT (`https://prewww1.aeat.es/...`) es accesible y no está bloqueado por firewalls locales o políticas corporativas.

## 2. Descripción del Flujo de Prueba (End-to-End)

La prueba consistirá en simular el ciclo de vida completo de un envío desde el punto de vista del usuario final invocando la API del backend, hasta recibir la conformidad de la Hacienda española.

### Paso 2.1: Generación del Payload XML
1. Invocar internamente al servicio de dominio/infraestructura encargado de mapear la "Factura" de nuestro dominio al formato oficial `RegistroAltaFacturas`.
2. **Validación:** Comprobar que el XML generado es válido estructuralmente (XSD) y que no le faltan campos obligatorios.

### Paso 2.2: Firma Digital del XML
1. El sistema coge el XML en claro y le aplica la firma digital usando el certificado PKCS#12 proporcionado.
2. **Validación:** Comprobar que el tag `<ds:Signature>` aparece en el documento y es criptográficamente válido.

### Paso 2.3: Configuración del Canal Seguro (mTLS) e Invocación SOAP
1. El `VerifactuSoapClient` establece un "Handshake TLS" con la AEAT presentando el certificado cliente, construyendo la petición HTTP POST con el `Envelope` SOAP correspondiente.
2. **Validación:** Comprobar que no hay errores de "Handshake Exception" o "SSLPeerUnverifiedException".

### Paso 2.4: Recepción y Parseo de la Respuesta Oficial (AEAT)
1. Recibir la respuesta síncrona de la AEAT (Típicamente un archivo XML firmado por ellos).
2. Procesar el XML de respuesta para detectar el estado del envío:
   - `Aceptada` (Correcto).
   - `Aceptada con Errores` (Funcional pero con advertencias).
   - `Rechazada` (Fallo de validación de negocio).
3. **Validación:** Registrar la respuesta en logs y en la base de datos vinculada a la factura de pruebas.

## 3. Preparación Inicial del Entorno y Código

Revisión en código pendiente para posibilitar el test:

- [ ] Revisión del `VerifactuSoapClient.java` para asegurar la configuración correcta de la inyección de PKCS#12 en el `SSLContext`.
- [ ] Revisión del `VerifactuXmlBuilder.java` y `VerifactuSigner.java`.
- [ ] Creación de un Test de Integración `VerifactuIntegrationTest.java` (o uso de Postman invocando el endpoint REST del Controller, según se prefiera para E2E).

## 4. Ejecución del Test

*Pendiente de detallar los comandos y resultados logrados una vez el código de mTLS esté finalizado.*

---
*Estado actual: Fase de diseño y análisis de requisitos de código antes del envío.*
