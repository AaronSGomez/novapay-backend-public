# Comandos PowerShell — Tests de Verifactu

> Ejecutar siempre desde el directorio raíz del proyecto backend.

---

## Variables de Entorno Requeridas

Antes de cada test, carga las variables del certificado en la sesión de PowerShell.  
Guarda este bloque como función o ejecuta siempre al inicio:

```powershell
# Cargar variables de entorno del certificado de prueba
$env:JWT_SECRET     = "dGhpc2lzYWZha2VzZWNyZXRrZXlmb3JkZXZlbG9wbWVudG9ubHkxMjM0NTY3ODkw"
$env:CERT_ALIAS     = "test-alias"
$env:CERT_PASSWORD  = "changeit"
$env:CERT_SIGNING_ENABLED = "true"
# Leer el certificado Base64 del .env o archivo local si está configurado
$env:CERT_P12_BASE64 = (Get-Content .env -ErrorAction SilentlyContinue | Select-String '^CERT_P12_BASE64=(.+)' | ForEach-Object { $_.Matches.Groups[1].Value })
```

---

## Test 1 — Envío Real E2E a AEAT

**Clase:** `VerifactuRealE2ETest`

Crea una factura de prueba, la envía firmada a AEAT y muestra el resultado (status, CSV, XML enviado y recibido).

```powershell
.\mvnw.cmd test -Dtest=VerifactuRealE2ETest
```

### ¿Qué muestra en consola?
- `EMPRESA ENCONTRADA Y ACTUALIZADA EN BD: <uuid>` — empresa cargada
- `EMITIENDO FACTURA AL ENDPOINT DE VERIFACTU...` — inicio del envío
- `Status: ACEPTADO / RECHAZADO` — resultado final
- `Agency Invoice ID (CSV): A-XXXXX` — el CSV que devuelve AEAT
- El XML firmado enviado a AEAT (SOAP Envelope completo)
- El XML de respuesta recibido de AEAT (con `EstadoEnvio`, `RespuestaLinea`, etc.)

### Ver solo errores/resultado
```powershell
.\mvnw.cmd test -Dtest=VerifactuRealE2ETest 2>&1 | Select-String "EMITIENDO|estadoEnvio|RESULTADO|Status:|rechaz|csv|WARN|ERROR" | Select-Object -Last 30
```

---

## Test 2 — Diagnóstico: Ver XMLs del Último Registro en BD

**Clase:** `VerifactuDiagnosticoTest`

Lee el último registro fiscal con `response_xml` no nulo y **escribe los XMLs a disco** (`target/last_sent.xml` y `target/last_response.xml`) para poder inspeccionarlos.

```powershell
$env:JWT_SECRET = "dGhpc2lzYWZha2VzZWNyZXRrZXlmb3JkZXZlbG9wbWVudG9ubHkxMjM0NTY3ODkw"
.\mvnw.cmd test -Dtest=VerifactuDiagnosticoTest
```

### Después del test, abrir los ficheros:
```powershell
# XML que enviamos a AEAT (firmado, SOAP Envelope completo)
Get-Content target/last_sent.xml

# XML que nos devolvió AEAT (RespuestaRegFactuSistemaFacturacion)
Get-Content target/last_response.xml
```

> 💡 Para ver con formato XML legible, usa VS Code: en `target/last_sent.xml` pulsa `Alt+Shift+F` (Format Document).

### ¿Qué contienen?

| Fichero | Contenido |
|---|---|
| `target/last_sent.xml` | SOAP Envelope firmado con `ds:Signature`. Incluye `Cabecera`, `RegistroAlta` con `Desglose`, `CuotaTotal`, `ImporteTotal`, `Encadenamiento`, `SistemaInformatico` |
| `target/last_response.xml` | Respuesta SOAP de AEAT. Incluye `EstadoEnvio`, `CSV`, `RespuestaLinea` con `EstadoRegistro`, `CodigoErrorRegistro`, `DescripcionErrorRegistro` |

---

## Test 3 — Solo Tests Unitarios (Builder XML)

**Clase:** `VerifactuXmlBuilderTest`

Verifica la generación correcta del XML sin llamar a AEAT. No requiere certificado.

```powershell
.\mvnw.cmd test -Dtest=VerifactuXmlBuilderTest
```

---

## Test 4 — Todos los Tests del Proyecto

```powershell
# Sin llamar a AEAT (excluye E2E reales)
.\mvnw.cmd test -Dexcludes="**/*E2E*,**/*Real*"

# Solo compilar (verifica que no hay errores de compilación)
.\mvnw.cmd compile -q
```

---

## Dónde Ver los Mensajes XML Detallados

### Opción A — Ficheros en disco (recomendado)
Ejecuta `VerifactuDiagnosticoTest` y abre:
- `target/last_sent.xml` → XML enviado a AEAT
- `target/last_response.xml` → XML recibido de AEAT

### Opción B — Consola Maven con filtro
```powershell
.\mvnw.cmd test -Dtest=VerifactuRealE2ETest 2>&1 | Select-String "REQUEST BODY|RESPONSE BODY|---"
```

### Opción C — Base de Datos directamente
Si tienes acceso a pgAdmin / DBeaver, conecta a la BD y ejecuta:
```sql
SELECT 
  status,
  current_hash,
  LEFT(sent_xml, 500)     as xml_enviado_preview,
  LEFT(response_xml, 500) as xml_respuesta_preview
FROM fiscal_records
WHERE response_xml IS NOT NULL
ORDER BY id DESC
LIMIT 5;
```

---

## Interpretación de Respuesta AEAT

| Campo en `target/last_response.xml` | Significado |
|---|---|
| `EstadoEnvio` | `Correcto` / `ParcialmenteCorrecto` / `Incorrecto` |
| `CSV` | Código Seguro de Verificación (si es aceptada) |
| `EstadoRegistro` | `Correcto` / `AceptadoConErrores` / `Incorrecto` |
| `CodigoErrorRegistro` | Código numérico del error (4102, 1100, etc.) |
| `DescripcionErrorRegistro` | Descripción legible del error |

### Códigos de error comunes

| Código | Descripción | Solución |
|---|---|---|
| 4102 | Campo obligatorio faltante | Revisar el XSD y añadir el campo |
| 4118 | NIF no encaja con el certificado | El NombreRazon del cert no coincide con AEAT |
| 1100 | Valor o tipo incorrecto | Revisar maxLength del campo en el XSD |
| Huella incorrecta | Hash de encadenamiento erróneo | Esperado en pruebas con múltiples envíos |

---

## Test 5 — Ejecución Unificada con Certificado de Prueba

```powershell
$env:JWT_SECRET="dGhpc2lzYWZha2VzZWNyZXRrZXlmb3JkZXZlbG9wbWVudG9ubHkxMjM0NTY3ODkw"; $env:CERT_ALIAS="test-alias"; $env:CERT_PASSWORD="changeit"; $env:CERT_SIGNING_ENABLED="true"; .\mvnw.cmd test -Dtest=VerifactuRealE2ETest 2>&1 | Select-String "EMITIENDO|estadoEnvio|WARN.*(rechaz|Error)|rechaz|RESULTADO|Status:" | Select-Object -Last 20
```