# Campos a Modificar para Producción — Verifactu

> Documento técnico. Recoge todos los valores hardcodeados o dependientes del entorno que deben cambiarse al pasar de pruebas/desarrollo a producción real.

---

## Resumen

Durante las pruebas se firma con el **certificado de pruebas** (`12345678Z` / `EMPRESA DE PRUEBAS S.L.`). En producción, el emisor será una empresa autorizada con su propio NIF y certificado digital de representación o sello electrónico.

El bloque `SistemaInformatico` del XML Verifactu identifica el **software garante** (NovaPay). Sus datos deben corresponder al NIF real del operador fiscal.

---

## 1. `VerifactuXmlBuilder.java` — `buildSistemaInformatico()`

Archivo: `src/main/java/.../verifactu/VerifactuXmlBuilder.java`

| Campo XML | Valor actual (pruebas) | Valor en producción | Tipo XSD |
|---|---|---|---|
| `NombreRazon` | Nombre del emisor (`EMPRESA DE PRUEBAS S.L.`) | **Nombre de empresa** registrado en AEAT | TextMax120 |
| `NIF` | NIF del emisor (`12345678Z`) | **NIF de NOVAPAY SL** (dado de alta en AEAT Verifactu) | TextMax9 |
| `NombreSistemaInformatico` | `NovaPay` | `NovaPay` ✅ (sin cambio) | TextMax30 |
| `IdSistemaInformatico` | `01` | `01` ✅ (sin cambio) | TextMax2 |
| `Version` | `1.0` | `1.0` ✅ (sin cambio) | TextMax5 |
| `NumeroInstalacion` | `NOVAPAY-001` | `NOVAPAY-001` ✅ (sin cambio) | TextMax30 |
| `TipoUsoPosibleSoloVerifactu` | `S` | `S` ✅ (sin cambio) | S/N |
| `TipoUsoPosibleMultiOT` | `N` | `N` ✅ (sin cambio) | S/N |
| `IndicadorMultiplesOT` | `N` | `N` ✅ (sin cambio) | S/N |

> ⚠️ **Acción requerida**: Cuando la empresa tenga NIF propio dado de alta en AEAT para Verifactu, actualizar `NombreRazon` y `NIF` del `SistemaInformatico`. Esto se puede externalizar a `application.yml` o a la configuración de empresa en BD.

---

## 2. Variables de Entorno (`.env`)

| Variable | Valor actual (pruebas) | Valor en producción |
|---|---|---|
| `CERT_ALIAS` | `test-alias` | Alias del **certificado de empresa** FNMT |
| `CERT_P12_BASE64` | Certificado dummy auto-firmado | **Certificado PKCS#12 de empresa** en Base64 |
| `CERT_PASSWORD` | `changeit` | Password del cert de empresa |
| `CERT_SIGNING_ENABLED` | `true` | `true` ✅ |
| `VERIFACTU_PRODUCTION` | `false` (entorno homologación) | **`true`** (para endpoint real de producción) |
| `VERIFACTU_ENDPOINT_URL` | URL de homologación AEAT | **URL de producción AEAT** |

---

## 3. Datos de Empresa en Base de Datos

En pruebas el test E2E fuerza los datos de empresa. En producción, el registro en BD debe contener:

| Campo | Valor pruebas | Valor producción |
|---|---|---|
| `companies.tax_id` | `12345678Z` | NIF real de NOVAPAY SL |
| `companies.name` | `EMPRESA DE PRUEBAS S.L.` | `NOVAPAY SL` (exactamente como en el censo AEAT) |
| `companies.tax_agency` | `AEAT` | `AEAT` ✅ |

> ⚠️ El `name` de la empresa en BD se usa directamente en el XML (`NombreRazonEmisor` y `SistemaInformatico.NombreRazon`). Debe coincidir exactamente con el nombre registrado en el censo de la AEAT.

---

## 4. Endpoint AEAT — URL de Producción vs Homologación

| Entorno | URL |
|---|---|
| **Homologación (pruebas)** | `https://prewww1.aeat.es/wlpl/TIKE-CONT/ws/SistemaFacturacion/VerifactuSOAP` |
| **Producción** | `https://www1.aeat.es/wlpl/TIKE-CONT/ws/SistemaFacturacion/VerifactuSOAP` |

El switch se controla con `VERIFACTU_PRODUCTION=true/false` en `.env`.

---

## 5. `ImporteTotal` vs Cálculo Real

Actualmente el test envía líneas con:
- `Café de prueba` 1.50€ + IVA 10% → base 1.50, cuota 0.15
- `Tostada` 2.50€ + IVA 10% → base 2.50, cuota 0.25

En producción, el `ImporteTotal` debe ser **base + cuota**, y `CuotaTotal` la suma de todas las cuotas. El cálculo es automático en `TaxCalculationService` y `buildCuotaTotal()` — no requiere cambios.

---

## 6. Checklist de Go-Live

- [ ] Obtener certificado FNMT de **empresa** (NOVAPAY SL) o de representante autorizado
- [ ] Actualizar `.env` con el nuevo `CERT_ALIAS`, `CERT_P12_BASE64`, `CERT_PASSWORD`
- [ ] Dar de alta el NIF de empresa en el censo AEAT para Verifactu
- [ ] Cambiar `VERIFACTU_PRODUCTION=true` y la `VERIFACTU_ENDPOINT_URL` a producción
- [ ] Actualizar `NIF` y `NombreRazon` en `buildSistemaInformatico()` (o externalizarlo a config)
- [ ] Insertar el registro de empresa en BD con `tax_id` y `name` correctos
- [ ] Verificar que el primer envío real genera un registro con `status=ACEPTADO` y CSV válido
