# NOVAPAY â€” PrÃ³ximos pasos e Plan de Tests
> Documento ejecutable por cualquier modelo IA. Cada paso es independiente y contiene toda la informaciÃ³n necesaria.
> **Stack:** Spring Boot 3.5, Java 21, Mockito 5, JUnit 5, Swagger/SpringDoc OpenAPI 3

---

## Estado actual âœ…

| MÃ³dulo | Estado |
|--------|--------|
| Arquitectura hexagonal completa | âœ… |
| God Services â†’ 5 servicios individuales | âœ… |
| GlobalExceptionHandler (RFC 7807) | âœ… |
| JWT Authentication (api_clients) | âœ… |
| Firma digital XMLDSig (Apache Santuario) | âœ… |
| Audit log (sentXml / responseXml) | âœ… |
| application.yml + .env configurados | âœ… |

---

## PASO 1 â€” Swagger / OpenAPI Documentation

### 1.1 AÃ±adir dependencia SpringDoc OpenAPI

**Archivo:** [pom.xml](pom.xml)

```xml
<!-- SpringDoc OpenAPI â€” Swagger UI -->
<dependency>
    <groupId>org.springdoc</groupId>
    <artifactId>springdoc-openapi-starter-webmvc-ui</artifactId>
    <version>2.8.6</version>
</dependency>
```

### 1.2 Crear configuraciÃ³n OpenAPI

**Archivo nuevo:** `src/main/java/levelup42/novapay_backend_hex/infrastructure/config/OpenApiConfig.java`

```java
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI novapayOpenAPI() {
        return new OpenAPI()
            .info(new Info()
                .title("NovaPay Fiscal API")
                .description("API REST para gestiÃ³n de facturas electrÃ³nicas VERIFACTU / TicketBAI")
                .version("1.0.0")
                .contact(new Contact().name("LevelUp42").email("dev@levelup42.es")))
            .addSecurityItem(new SecurityRequirement().addList("Bearer Auth"))
            .components(new Components()
                .addSecuritySchemes("Bearer Auth",
                    new SecurityScheme()
                        .type(SecurityScheme.Type.HTTP)
                        .scheme("bearer")
                        .bearerFormat("JWT")
                        .description("ObtÃ©n el token con POST /api/v1/auth/token")));
    }
}
```

### 1.3 AÃ±adir anotaciones @Operation a los controladores

**Archivo:** [InvoiceController.java](src/main/java/com/novapay/backend/api/controller/InvoiceController.java) â€” AÃ±adir en cada mÃ©todo:
```java
@Operation(summary = "Emitir factura", 
           description = "Crea y registra una factura en VERIFACTU",
           security = @SecurityRequirement(name = "Bearer Auth"))
@ApiResponse(responseCode = "201", description = "Factura creada")
@ApiResponse(responseCode = "400", description = "Datos invÃ¡lidos")
@ApiResponse(responseCode = "401", description = "Token invÃ¡lido o expirado")
```

**Archivo:** [AuthController.java](src/main/java/levelup42/novapay_backend_hex/infrastructure/adapter/in/rest/AuthController.java) â€” Sin seguridad en el endpoint de token:
```java
@Operation(summary = "Obtener JWT", description = "Autentica un cliente API y devuelve un token JWT")
@ApiResponse(responseCode = "200", description = "Token generado")
@ApiResponse(responseCode = "401", description = "Credenciales incorrectas")
```

**Archivo:** [FiscalController.java](src/main/java/com/novapay/backend/api/controller/FiscalController.java) â€” Para los endpoints de estado y reintento.

### 1.4 Configurar Swagger en SecurityConfig

AÃ±adir a la cadena `authorizeHttpRequests`:
```java
.requestMatchers(
    "/swagger-ui/**",
    "/swagger-ui.html",
    "/v3/api-docs/**"
).permitAll()
```

### 1.5 VerificaciÃ³n

```
mvnw spring-boot:run
# Abrir: http://localhost:8080/swagger-ui.html
# Debe mostrar los 3 controladores con el candado JWT
```

---

## PASO 2 â€” Suite de Tests Unitarios con Mockito

> AÃ±adir Mockito estÃ¡ incluido en `spring-boot-starter-test`. No requiere dependencies adicionales.

---

### 2.1 Test: `JwtTokenProviderTest`

**Archivo nuevo:** `src/test/java/.../infrastructure/security/jwt/JwtTokenProviderTest.java`

```java
@ExtendWith(MockitoExtension.class)
class JwtTokenProviderTest {

    private JwtTokenProvider provider;
    private static final String SECRET = "test-secret-must-be-32-chars-long!";

    @BeforeEach
    void setUp() {
        provider = new JwtTokenProvider(SECRET, 60L);
    }

    @Test
    void generate_devuelveTokenNoNulo() {
        String token = provider.generate("client-1", List.of("ROLE_INVOICER"));
        assertNotNull(token);
        assertTrue(token.startsWith("ey")); // JWT vÃ¡lido
    }

    @Test
    void validateAndGetClaims_tokenValido_devuelveClaims() {
        String token = provider.generate("client-42", List.of("ROLE_FISCAL"));
        Claims claims = provider.validateAndGetClaims(token);
        assertEquals("client-42", claims.getSubject());
    }

    @Test
    void validateAndGetClaims_tokenManipulado_lanzaJwtException() {
        String token = provider.generate("client-1", List.of()) + "MANIPULADO";
        assertThrows(JwtException.class, () -> provider.validateAndGetClaims(token));
    }

    @Test
    void validateAndGetClaims_tokenExpirado_lanzaJwtException() {
        // TTL de 0 minutos â†’ expira inmediatamente
        JwtTokenProvider shortProvider = new JwtTokenProvider(SECRET, 0L);
        String token = shortProvider.generate("client-1", List.of());
        assertThrows(JwtException.class, () -> shortProvider.validateAndGetClaims(token));
    }

    @Test
    void generate_conRolesMultiples_rolesEnClaims() {
        List<String> roles = List.of("ROLE_INVOICER", "ROLE_FISCAL", "ROLE_ADMIN");
        String token = provider.generate("admin", roles);
        Claims claims = provider.validateAndGetClaims(token);
        List<?> rolesEnToken = claims.get("roles", List.class);
        assertEquals(3, rolesEnToken.size());
    }
}
```

---

### 2.2 Test: `AuthControllerTest`

```java
@ExtendWith(MockitoExtension.class)
class AuthControllerTest {

    @Mock ApiClientRepositoryPort clientRepository;
    @Mock JwtTokenProvider tokenProvider;
    @Mock PasswordEncoder passwordEncoder;
    @InjectMocks AuthController controller;

    @Test
    void getToken_credencialesValidas_retorna200ConToken() {
        ApiClient client = new ApiClient("id", "c1", "hashed", List.of("ROLE_INVOICER"), true);
        when(clientRepository.findByClientId("c1")).thenReturn(Optional.of(client));
        when(passwordEncoder.matches("secret123", "hashed")).thenReturn(true);
        when(tokenProvider.generate("c1", client.getRoles())).thenReturn("jwt.token.aqui");
        when(tokenProvider.getExpirationMs()).thenReturn(3600000L);

        ResponseEntity<AuthResponse> resp = controller.getToken(new AuthRequest("c1", "secret123"));

        assertEquals(200, resp.getStatusCode().value());
        assertEquals("jwt.token.aqui", resp.getBody().accessToken());
    }

    @Test
    void getToken_clienteNoExiste_retorna401() {
        when(clientRepository.findByClientId("noexiste")).thenReturn(Optional.empty());
        ResponseEntity<AuthResponse> resp = controller.getToken(new AuthRequest("noexiste", "x"));
        assertEquals(401, resp.getStatusCode().value());
    }

    @Test
    void getToken_passwordIncorrecto_retorna401() {
        ApiClient client = new ApiClient("id", "c1", "hashed", List.of(), true);
        when(clientRepository.findByClientId("c1")).thenReturn(Optional.of(client));
        when(passwordEncoder.matches("wrong", "hashed")).thenReturn(false);
        ResponseEntity<AuthResponse> resp = controller.getToken(new AuthRequest("c1", "wrong"));
        assertEquals(401, resp.getStatusCode().value());
    }

    @Test
    void getToken_clienteInactivo_retorna401() {
        ApiClient client = new ApiClient("id", "c1", "hashed", List.of(), false); // active=false
        when(clientRepository.findByClientId("c1")).thenReturn(Optional.of(client));
        ResponseEntity<AuthResponse> resp = controller.getToken(new AuthRequest("c1", "x"));
        assertEquals(401, resp.getStatusCode().value());
    }
}
```

---

### 2.3 Test: `EmitInvoiceServiceTest`

```java
@ExtendWith(MockitoExtension.class)
class EmitInvoiceServiceTest {

    @Mock CompanyRepositoryPort companyRepository;
    @Mock InvoiceRepositoryPort invoiceRepository;
    @Mock FiscalEvidenceService fiscalEvidenceService;
    @Mock FiscalAgencyPort fiscalAgencyPort;
    @Mock TaxCalculationService taxCalculationService;
    @InjectMocks EmitInvoiceService service;

    @Test
    void emit_empresaNoExiste_lanzaCompanyNotFoundException() {
        when(companyRepository.findById(any())).thenReturn(Optional.empty());
        InvoiceCreateCommand cmd = buildCommand();
        assertThrows(CompanyNotFoundException.class, () -> service.emit(cmd));
    }

    @Test
    void emit_facturaValida_guardaYDevuelveResultado() {
        Company company = buildCompany();
        when(companyRepository.findById(any())).thenReturn(Optional.of(company));
        when(invoiceRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(fiscalEvidenceService.prepareFiscalEvidence(any())).thenReturn(buildFiscalRecord());
        when(fiscalAgencyPort.getAgency()).thenReturn(TaxAgency.AEAT);

        InvoiceResult result = service.emit(buildCommand());

        assertNotNull(result);
        verify(invoiceRepository, times(1)).save(any());
        verify(fiscalEvidenceService, times(1)).prepareFiscalEvidence(any());
    }
}
```

---

### 2.4 Test: `FiscalEvidenceServiceTest` â­ CRÃTICO para VERIFACTU

```java
@ExtendWith(MockitoExtension.class)
class FiscalEvidenceServiceTest {

    @Mock InvoiceRepositoryPort invoiceRepository;
    @Mock FiscalRecordRepositoryPort fiscalRecordRepository;
    @Mock HashServicePort hashService;
    @InjectMocks FiscalEvidenceService service;

    @Test
    void prepareFiscalEvidence_primerFactura_primeraHuellaNull() {
        Invoice invoice = buildInvoice();
        when(invoiceRepository.findLastByCompanyIdForHash(any())).thenReturn(Optional.empty());
        when(hashService.calculateChainedHash(invoice, null)).thenReturn("HASH_INICIAL");
        when(fiscalRecordRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        FiscalRecord record = service.prepareFiscalEvidence(invoice);

        assertNull(record.getPreviousHash());
        assertEquals("HASH_INICIAL", record.getCurrentHash());
        assertEquals(FiscalStatus.PENDIENTE_ENVIO, record.getStatus());
    }

    @Test
    void prepareFiscalEvidence_facturaSecundaria_encadenaHashCorrectamente() {
        Invoice prevInvoice = buildInvoice();
        Invoice invoice = buildInvoice();
        FiscalRecord prevRecord = buildFiscalRecord("HASH_ANTERIOR");

        when(invoiceRepository.findLastByCompanyIdForHash(any())).thenReturn(Optional.of(prevInvoice));
        when(fiscalRecordRepository.findByInvoiceId(any())).thenReturn(Optional.of(prevRecord));
        when(hashService.calculateChainedHash(invoice, "HASH_ANTERIOR")).thenReturn("HASH_NUEVO");
        when(fiscalRecordRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        FiscalRecord record = service.prepareFiscalEvidence(invoice);

        assertEquals("HASH_ANTERIOR", record.getPreviousHash()); // encadenamiento correcto
        assertEquals("HASH_NUEVO", record.getCurrentHash());
    }

    @Test
    void prepareFiscalEvidence_tipoEsALTA() {
        // El primer registro siempre debe ser tipo ALTA
        Invoice invoice = buildInvoice();
        when(invoiceRepository.findLastByCompanyIdForHash(any())).thenReturn(Optional.empty());
        when(hashService.calculateChainedHash(any(), any())).thenReturn("HASH");
        when(fiscalRecordRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        FiscalRecord record = service.prepareFiscalEvidence(invoice);
        assertEquals(FiscalRecordType.ALTA, record.getType());
    }
}
```

---

### 2.5 Test: `VerifactuAdapterTest` â­ CRÃTICO para VERIFACTU

```java
@ExtendWith(MockitoExtension.class)
class VerifactuAdapterTest {

    @Mock VerifactuXmlBuilder xmlBuilder;
    @Mock VerifactuSoapClient soapClient;
    @Mock VerifactuResponseParser responseParser;
    @Mock FiscalRecordRepositoryPort fiscalRecordRepository;
    @Mock XmlSignerPort xmlSigner;
    @InjectMocks VerifactuAdapter adapter;

    // â”€â”€ ALTA â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

    @Test
    void submit_ALTA_flujoCompleto_guardaAuditYRetornaAgencyResponse() throws Exception {
        FiscalRecord record = buildRecord(FiscalRecordType.ALTA);
        when(xmlBuilder.buildAltaEnvelope(any(), any())).thenReturn("<soap>xml</soap>");
        when(xmlSigner.sign("<soap>xml</soap>")).thenReturn("<soap>xml_firmado</soap>");
        when(soapClient.send(any(), any())).thenReturn("<respuesta>OK</respuesta>");
        when(responseParser.parse(any())).thenReturn(new ParseResult(true, "Correcto", null, null, "CSV123", "<respuesta>OK</respuesta>"));
        when(fiscalRecordRepository.findByCurrentHash(any())).thenReturn(Optional.empty());
        when(fiscalRecordRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        AgencyResponse response = adapter.submit(record);

        // Audit verificado
        assertNotNull(record.getSentXml());       // XML firmado guardado
        assertNotNull(record.getSentAt());         // timestamp envÃ­o
        assertNotNull(record.getResponseXml());    // respuesta guardada
        assertNotNull(record.getRespondedAt());    // timestamp respuesta
        assertEquals(FiscalStatus.ACEPTADO, record.getStatus());

        // Respuesta correcta
        assertTrue(response.successful());
        assertEquals("CSV123", response.secureVerificationCode());

        // Verificar firma fue llamada
        verify(xmlSigner, times(1)).sign(any());
        verify(soapClient, times(1)).send("<soap>xml_firmado</soap>", "RegFactuSistemaFacturacion");
    }

    @Test
    void submit_ALTA_aeatRechaza_estadoRECHAZADO() throws Exception {
        FiscalRecord record = buildRecord(FiscalRecordType.ALTA);
        when(xmlBuilder.buildAltaEnvelope(any(), any())).thenReturn("<soap/>");
        when(xmlSigner.sign(any())).thenReturn("<soap_firmado/>");
        when(soapClient.send(any(), any())).thenReturn("<error/>");
        when(responseParser.parse(any())).thenReturn(new ParseResult(false, "Rechazado", "ERR001", "NIF invÃ¡lido", null, "<error/>"));
        when(fiscalRecordRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        AgencyResponse response = adapter.submit(record);

        assertFalse(response.successful());
        assertEquals(FiscalStatus.RECHAZADO, record.getStatus());
        assertEquals("ERR001", response.responseCode());
    }

    @Test
    void submit_ALTA_firmadoFalla_lanzaFiscalAgencyException() throws Exception {
        FiscalRecord record = buildRecord(FiscalRecordType.ALTA);
        when(xmlBuilder.buildAltaEnvelope(any(), any())).thenReturn("<soap/>");
        when(xmlSigner.sign(any())).thenThrow(new XmlSignerPort.XmlSigningException("Certificado no encontrado", null));

        assertThrows(FiscalAgencyPort.FiscalAgencyException.class, () -> adapter.submit(record));
        verify(soapClient, never()).send(any(), any()); // nunca debe enviarse si la firma falla
    }

    @Test
    void submit_ALTA_errorComunicacionAEAT_lanzaFiscalAgencyException() throws Exception {
        FiscalRecord record = buildRecord(FiscalRecordType.ALTA);
        when(xmlBuilder.buildAltaEnvelope(any(), any())).thenReturn("<soap/>");
        when(xmlSigner.sign(any())).thenReturn("<soap_firmado/>");
        when(soapClient.send(any(), any())).thenThrow(new VerifactuCommunicationException("Timeout", true));

        FiscalAgencyPort.FiscalAgencyException ex =
            assertThrows(FiscalAgencyPort.FiscalAgencyException.class, () -> adapter.submit(record));
        assertTrue(ex.isRetryable());
        assertEquals("COMM_ERROR", ex.getCode());
    }

    @Test
    void submit_ALTA_primerFactura_sinEncadenamiendoAnterior() throws Exception {
        FiscalRecord record = buildRecord(FiscalRecordType.ALTA);
        record.setPreviousHash(null); // Primera factura

        when(xmlBuilder.buildAltaEnvelope(record, null)).thenReturn("<soap/>");
        when(xmlSigner.sign(any())).thenReturn("<soap_firmado/>");
        when(soapClient.send(any(), any())).thenReturn("<ok/>");
        when(responseParser.parse(any())).thenReturn(successResult());
        when(fiscalRecordRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        adapter.submit(record);

        // Verifica que se pasÃ³ null como previousRecord al xmlBuilder
        verify(xmlBuilder).buildAltaEnvelope(record, null);
    }

    // â”€â”€ ANULACIÃ“N â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

    @Test
    void cancel_ANULACION_flujoCompleto_estadoANULADO() throws Exception {
        FiscalRecord record = buildRecord(FiscalRecordType.BAJA);
        when(xmlBuilder.buildAnulacionEnvelope(any())).thenReturn("<anulacion/>");
        when(xmlSigner.sign(any())).thenReturn("<anulacion_firmada/>");
        when(soapClient.send(any(), any())).thenReturn("<ok/>");
        when(responseParser.parse(any())).thenReturn(successResult());
        when(fiscalRecordRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        AgencyResponse response = adapter.cancel(record);

        assertTrue(response.successful());
        assertEquals(FiscalStatus.ANULADO, record.getStatus());
        assertNotNull(record.getSentXml());    // audit
        assertNotNull(record.getResponseXml()); // audit
    }

    @Test
    void cancel_ANULACION_aeatRechaza_estadoRECHAZADO() throws Exception {
        FiscalRecord record = buildRecord(FiscalRecordType.BAJA);
        when(xmlBuilder.buildAnulacionEnvelope(any())).thenReturn("<anulacion/>");
        when(xmlSigner.sign(any())).thenReturn("<anulacion_firmada/>");
        when(soapClient.send(any(), any())).thenReturn("<error/>");
        when(responseParser.parse(any())).thenReturn(failResult("ERR002", "Factura ya anulada"));
        when(fiscalRecordRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        AgencyResponse response = adapter.cancel(record);

        assertFalse(response.successful());
        assertEquals(FiscalStatus.RECHAZADO, record.getStatus());
    }

    // â”€â”€ AUDIT LOG â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

    @Test
    void submit_guardaXmlFirmadoEnBBDD_antesDeEnviar() throws Exception {
        List<FiscalRecord> saved = new ArrayList<>();
        FiscalRecord record = buildRecord(FiscalRecordType.ALTA);

        when(xmlBuilder.buildAltaEnvelope(any(), any())).thenReturn("<soap/>");
        when(xmlSigner.sign(any())).thenReturn("<soap_firmado/>");
        when(soapClient.send(any(), any())).thenReturn("<ok/>");
        when(responseParser.parse(any())).thenReturn(successResult());
        when(fiscalRecordRepository.save(any())).thenAnswer(inv -> {
            saved.add(inv.getArgument(0));
            return inv.getArgument(0);
        });

        adapter.submit(record);

        // El primer save debe tener sentXml y sentAt pero aÃºn NO responseXml
        FiscalRecord firstSave = saved.get(0);
        assertNotNull(firstSave.getSentXml());
        assertNotNull(firstSave.getSentAt());

        // El segundo save debe tener todo
        FiscalRecord secondSave = saved.get(1);
        assertNotNull(secondSave.getResponseXml());
        assertNotNull(secondSave.getRespondedAt());
    }
}
```

---

### 2.6 Test: `HashServiceImplTest` â­ CRÃTICO â€” Integridad de la cadena fiscal

```java
class HashServiceImplTest {

    private HashServiceImpl hashService = new HashServiceImpl();

    @Test
    void calculateChainedHash_primerRegistro_hashNoNulo() {
        Invoice invoice = buildInvoice("NIF001", "2024-01-01", "F1", "100.00");
        String hash = hashService.calculateChainedHash(invoice, null);
        assertNotNull(hash);
        assertEquals(64, hash.length()); // SHA-256 = 64 hex chars
    }

    @Test
    void calculateChainedHash_mismaEntrada_mismoHash() {
        Invoice invoice = buildInvoice("NIF001", "2024-01-01", "F1", "100.00");
        String hash1 = hashService.calculateChainedHash(invoice, null);
        String hash2 = hashService.calculateChainedHash(invoice, null);
        assertEquals(hash1, hash2); // determinista
    }

    @Test
    void calculateChainedHash_conHashAnterior_diferenteAsinPrevio() {
        Invoice invoice = buildInvoice("NIF001", "2024-01-01", "F1", "100.00");
        String hashSinPrevio = hashService.calculateChainedHash(invoice, null);
        String hashConPrevio = hashService.calculateChainedHash(invoice, "HASH_ANTERIOR");
        assertNotEquals(hashSinPrevio, hashConPrevio); // el encadenamiento cambia el hash
    }

    @Test
    void calculateChainedHash_diferenteImporte_diferenteHash() {
        Invoice inv1 = buildInvoice("NIF001", "2024-01-01", "F1", "100.00");
        Invoice inv2 = buildInvoice("NIF001", "2024-01-01", "F1", "200.00");
        String hash1 = hashService.calculateChainedHash(inv1, null);
        String hash2 = hashService.calculateChainedHash(inv2, null);
        assertNotEquals(hash1, hash2);
    }

    @Test
    void calculateChainedHash_diferenteNIF_diferenteHash() {
        Invoice inv1 = buildInvoice("NIF001", "2024-01-01", "F1", "100.00");
        Invoice inv2 = buildInvoice("NIF002", "2024-01-01", "F1", "100.00");
        String hash1 = hashService.calculateChainedHash(inv1, null);
        String hash2 = hashService.calculateChainedHash(inv2, null);
        assertNotEquals(hash1, hash2);
    }
}
```

---

### 2.7 Test: `RetryFiscalSubmissionServiceTest`

```java
@ExtendWith(MockitoExtension.class)
class RetryFiscalSubmissionServiceTest {

    @Mock InvoiceRepositoryPort invoiceRepository;
    @Mock FiscalRecordRepositoryPort fiscalRecordRepository;
    @Mock FiscalAgencyPort fiscalAgencyPort;
    @InjectMocks RetryFiscalSubmissionService service;

    @Test
    void retry_facturaYaAceptada_noReintenta() {
        UUID id = UUID.randomUUID();
        Invoice invoice = buildInvoice(id);
        FiscalRecord record = buildRecord(FiscalStatus.ACEPTADO, 0);

        when(invoiceRepository.findById(id)).thenReturn(Optional.of(invoice));
        when(fiscalRecordRepository.findByInvoiceId(id)).thenReturn(Optional.of(record));

        service.retry(id);

        verify(fiscalAgencyPort, never()).submit(any()); // no debe reenviar
    }

    @Test
    void retry_facturaRechazada_incrementaRetryCountYEnvia() throws Exception {
        UUID id = UUID.randomUUID();
        Invoice invoice = buildInvoice(id);
        FiscalRecord record = buildRecord(FiscalStatus.RECHAZADO, 1);

        when(invoiceRepository.findById(id)).thenReturn(Optional.of(invoice));
        when(fiscalRecordRepository.findByInvoiceId(id)).thenReturn(Optional.of(record));
        when(fiscalRecordRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        service.retry(id);

        assertEquals(2, record.getRetryCount()); // incrementado
        assertEquals(FiscalStatus.PENDIENTE_ENVIO, record.getStatus());
        verify(fiscalAgencyPort, times(1)).submit(any());
    }

    @Test
    void retry_facturaNoExiste_lanzaInvoiceNotFoundException() {
        UUID id = UUID.randomUUID();
        when(invoiceRepository.findById(id)).thenReturn(Optional.empty());
        assertThrows(InvoiceNotFoundException.class, () -> service.retry(id));
    }

    @Test
    void retry_registroFiscalNoExiste_lanzaFiscalRecordNotFoundException() {
        UUID id = UUID.randomUUID();
        Invoice invoice = buildInvoice(id);
        when(invoiceRepository.findById(id)).thenReturn(Optional.of(invoice));
        when(fiscalRecordRepository.findByInvoiceId(any())).thenReturn(Optional.empty());
        assertThrows(FiscalRecordNotFoundException.class, () -> service.retry(id));
    }
}
```

---

### 2.8 Test: `XmlSignerImplTest`

```java
class XmlSignerImplTest {

    @Test
    void sign_signingDisabled_devuelveXmlSinFirmar() throws Exception {
        CertificateStoreProperties props = new CertificateStoreProperties();
        props.setSigningEnabled(false);
        XmlSignerImpl signer = new XmlSignerImpl(props);

        String xml = "<root><data>test</data></root>";
        String result = signer.sign(xml);

        assertEquals(xml, result); // pass-through sin firma
        assertFalse(result.contains("ds:Signature")); // sin bloque de firma
    }

    @Test
    void sign_signingEnabled_sinCertificado_lanzaXmlSigningException() {
        CertificateStoreProperties props = new CertificateStoreProperties();
        props.setSigningEnabled(true);
        props.setP12Base64(null);
        XmlSignerImpl signer = new XmlSignerImpl(props);

        assertThrows(XmlSignerPort.XmlSigningException.class,
            () -> signer.sign("<root/>"));
    }

    @Test
    void sign_signingEnabled_conCertificadoFalso_lanzaXmlSigningException() {
        CertificateStoreProperties props = new CertificateStoreProperties();
        props.setSigningEnabled(true);
        props.setP12Base64("DATOS_BASE64_INVALIDOS");
        props.setPassword("password");
        props.setAlias("alias");
        XmlSignerImpl signer = new XmlSignerImpl(props);

        assertThrows(XmlSignerPort.XmlSigningException.class,
            () -> signer.sign("<root/>"));
    }
}
```

---

### 2.9 Test: `VerifactuXmlBuilderTest` â­ CRÃTICO â€” Formato XML legal

```java
class VerifactuXmlBuilderTest {

    private VerifactuXmlBuilder builder = new VerifactuXmlBuilder();

    @Test
    void buildAltaEnvelope_contieneNIF() {
        FiscalRecord record = buildRecordCompleto("B12345678");
        String xml = builder.buildAltaEnvelope(record, null);
        assertTrue(xml.contains("B12345678"));
    }

    @Test
    void buildAltaEnvelope_primerRegistro_contieneElementoPrimerRegistro() {
        FiscalRecord record = buildRecordCompleto("B12345678");
        String xml = builder.buildAltaEnvelope(record, null);
        assertTrue(xml.contains("PrimerRegistro"));
        assertFalse(xml.contains("RegistroAnterior"));
    }

    @Test
    void buildAltaEnvelope_conRegistroAnterior_contieneEncadenamiento() {
        FiscalRecord record = buildRecordCompleto("B12345678");
        FiscalRecord previous = buildRecordAnterior("HASH_PREV");
        String xml = builder.buildAltaEnvelope(record, previous);
        assertTrue(xml.contains("RegistroAnterior"));
        assertTrue(xml.contains("HASH_PREV"));
        assertFalse(xml.contains("PrimerRegistro"));
    }

    @Test
    void buildAltaEnvelope_contieneHuellaActual() {
        FiscalRecord record = buildRecordCompleto("B12345678");
        record.setCurrentHash("MIHUELLA12345");
        String xml = builder.buildAltaEnvelope(record, null);
        assertTrue(xml.contains("MIHUELLA12345"));
    }

    @Test
    void buildAltaEnvelope_facturaCompleta_tipoF1() {
        FiscalRecord record = buildRecordConTipo(InvoiceType.COMPLETA);
        String xml = builder.buildAltaEnvelope(record, null);
        assertTrue(xml.contains("<sum:TipoFactura>F1</sum:TipoFactura>"));
    }

    @Test
    void buildAltaEnvelope_facturaSimplificada_tipoF2() {
        FiscalRecord record = buildRecordConTipo(InvoiceType.SIMPLIFICADA);
        String xml = builder.buildAltaEnvelope(record, null);
        assertTrue(xml.contains("<sum:TipoFactura>F2</sum:TipoFactura>"));
    }

    @Test
    void buildAltaEnvelope_facturaRectificativa_tipoR1() {
        FiscalRecord record = buildRecordConTipo(InvoiceType.RECTIFICATIVA);
        String xml = builder.buildAltaEnvelope(record, null);
        assertTrue(xml.contains("<sum:TipoFactura>R1</sum:TipoFactura>"));
    }

    @Test
    void buildAltaEnvelope_xmlEsValidoUTF8() {
        FiscalRecord record = buildRecordCompleto("B12345678");
        String xml = builder.buildAltaEnvelope(record, null);
        assertTrue(xml.contains("UTF-8") || xml.contains("utf-8"));
    }

    @Test
    void buildAnulacionEnvelope_contieneNIFyNumFactura() {
        FiscalRecord record = buildRecordCompleto("B12345678");
        String xml = builder.buildAnulacionEnvelope(record);
        assertTrue(xml.contains("B12345678"));
        assertFalse(xml.contains("PrimerRegistro")); // no en anulaciÃ³n
    }

    @Test
    void buildAltaEnvelope_conIVA_contieneDetalleIVA() {
        FiscalRecord record = buildRecordConIVA("21");
        String xml = builder.buildAltaEnvelope(record, null);
        assertTrue(xml.contains("DetalleIVA"));
        assertTrue(xml.contains("21"));
    }

    @Test
    void buildAltaEnvelope_conNombreSistema_contieneNovaPay() {
        FiscalRecord record = buildRecordCompleto("B12345678");
        String xml = builder.buildAltaEnvelope(record, null);
        assertTrue(xml.contains("NovaPay"));
        assertTrue(xml.contains("SistemaInformatico"));
    }
}
```

---

### 2.10 Test: `GlobalExceptionHandlerTest`

```java
@WebMvcTest // solo prueba la capa REST
class GlobalExceptionHandlerTest {

    @Autowired MockMvc mockMvc;
    @MockBean InvoiceCreateUseCase createUseCase;
    // ... otros mocks de use cases

    @Test
    void cuandoInvoiceNotFound_devuelve404ConProblemDetail() throws Exception {
        when(createUseCase.emit(any()))
            .thenThrow(new InvoiceNotFoundException(UUID.randomUUID()));

        mockMvc.perform(post("/api/v1/invoices")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{...}"))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.status").value(404))
            .andExpect(jsonPath("$.title").value("Not Found"));
    }

    @Test
    void cuandoInvalidState_devuelve409ConProblemDetail() throws Exception {
        when(createUseCase.emit(any()))
            .thenThrow(new InvalidInvoiceStateException("Factura ya emitida"));

        mockMvc.perform(post("/api/v1/invoices")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{...}"))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.status").value(409));
    }

    @Test
    void sinToken_devuelve401() throws Exception {
        mockMvc.perform(get("/api/v1/invoices/" + UUID.randomUUID()))
            .andExpect(status().isUnauthorized());
    }
}
```

---

## PASO 3 â€” Tests de IntegraciÃ³n con BD real

> Requieren PostgreSQL activo. Usar `@SpringBootTest` + `@Transactional`.

### 3.1 `ApiClientRepositoryIntegrationTest`

```java
@SpringBootTest
@Transactional
class ApiClientRepositoryIntegrationTest {

    @Autowired JpaApiClientAdapter adapter;

    @Test
    void findByClientId_clienteExistente_devuelveApiClient() {
        // Requiere V2 Flyway aplicado (cliente 'novapay-client')
        Optional<ApiClient> client = adapter.findByClientId("novapay-client");
        assertTrue(client.isPresent());
        assertEquals("novapay-client", client.get().getClientId());
        assertTrue(client.get().isActive());
        assertTrue(client.get().getRoles().contains("ROLE_INVOICER"));
    }

    @Test
    void findByClientId_clienteNoExistente_devuelveEmpty() {
        Optional<ApiClient> client = adapter.findByClientId("no-existe-999");
        assertTrue(client.isEmpty());
    }
}
```

### 3.2 `AuthFlowIntegrationTest` (E2E con MockMvc)

```java
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class AuthFlowIntegrationTest {

    @Autowired TestRestTemplate restTemplate;

    @Test
    void loginCorrecto_devuelveJwt() {
        AuthRequest req = new AuthRequest("novapay-client", "novapay-secret-2024");
        ResponseEntity<AuthResponse> resp = restTemplate.postForEntity(
            "/api/v1/auth/token", req, AuthResponse.class);
        assertEquals(200, resp.getStatusCode().value());
        assertNotNull(resp.getBody().accessToken());
    }

    @Test
    void sinToken_accesoEndpointProtegido_401() {
        ResponseEntity<String> resp = restTemplate.getForEntity(
            "/api/v1/invoices/" + UUID.randomUUID(), String.class);
        assertEquals(401, resp.getStatusCode().value());
    }

    @Test
    void conTokenValido_accesoEndpointProtegido_noEs401() {
        String token = obtenerToken();
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        ResponseEntity<String> resp = restTemplate.exchange(
            "/api/v1/invoices/" + UUID.randomUUID(),
            HttpMethod.GET, new HttpEntity<>(headers), String.class);
        assertNotEquals(401, resp.getStatusCode().value()); // puede ser 404, pero no 401
    }
}
```

---

## PASO 4 â€” Completar [VerifactuSoapClient](src/main/java/levelup42/novapay_backend_hex/infrastructure/adapter/out/fiscal/verifactu/VerifactuSoapClient.java#22-114) mTLS

> El [buildSslContext()](src/main/java/levelup42/novapay_backend_hex/infrastructure/adapter/out/fiscal/verifactu/VerifactuSoapClient.java#85-100) actual solo configura TrustManager. Falta cargar el KeyStore del cliente para mTLS con AEAT.

**Archivo:** [VerifactuSoapClient.java](src/main/java/com/novapay/backend/infrastructure/fiscal/verifactu/VerifactuSoapClient.java) â€” reemplazar [buildSslContext()](src/main/java/levelup42/novapay_backend_hex/infrastructure/adapter/out/fiscal/verifactu/VerifactuSoapClient.java#85-100):

```java
private SSLContext buildSslContext() throws Exception {
    byte[] p12 = Base64.getDecoder().decode(certProps.getP12Base64());
    KeyStore keyStore = KeyStore.getInstance("PKCS12");
    char[] password = certProps.getPassword().toCharArray();
    keyStore.load(new ByteArrayInputStream(p12), password);

    KeyManagerFactory kmf = KeyManagerFactory.getInstance(
        KeyManagerFactory.getDefaultAlgorithm());
    kmf.init(keyStore, password);

    TrustManagerFactory tmf = TrustManagerFactory.getInstance(
        TrustManagerFactory.getDefaultAlgorithm());
    tmf.init((KeyStore) null); // TrustStore del sistema (incluye CA de AEAT)

    SSLContext sslContext = SSLContext.getInstance("TLS");
    sslContext.init(kmf.getKeyManagers(), tmf.getTrustManagers(), null);
    return sslContext;
}
```

Inyectar [CertificateStoreProperties](src/main/java/levelup42/novapay_backend_hex/infrastructure/adapter/out/signing/CertificateStoreProperties.java#18-35) en el constructor de [VerifactuSoapClient](src/main/java/levelup42/novapay_backend_hex/infrastructure/adapter/out/fiscal/verifactu/VerifactuSoapClient.java#22-114).

---

## PASO 5 â€” AÃ±adir [.env](.env) a [.gitignore](.gitignore)

**Archivo:** [.gitignore](.gitignore) â€” Verificar que contiene:

```
.env
*.p12
*.jks
*.pfx
```

> âš ï¸ **El [.env](.env) NUNCA debe subirse al repositorio.** Si ya fue subido: `git rm --cached .env`

---

## Secuencia de ejecuciÃ³n recomendada

```bash
# 1. Compilar
.\mvnw.cmd compile -q

# 2. Tests unitarios (sin BD)
.\mvnw.cmd test -pl . -Dgroups="unitario" -q

# 3. Tests de integraciÃ³n (con BD activa)
$env:JWT_SECRET="test-secret-min-32-chars-long-ok"
.\mvnw.cmd test -q

# 4. Arrancar con Swagger
$env:JWT_SECRET="mi-secret-seguro-de-32-o-mas-chars"
.\mvnw.cmd spring-boot:run
# â†’ http://localhost:8080/swagger-ui.html

# 5. Prueba manual login:
curl -X POST http://localhost:8080/api/v1/auth/token \
  -H "Content-Type: application/json" \
  -d '{"clientId":"novapay-client","clientSecret":"novapay-secret-2024"}'
```

---

## Checklist final antes de producciÃ³n

- [ ] `PASO 1` Swagger documentado con @Operation en todos los endpoints
- [ ] `PASO 2` Todos los tests unitarios escritos y pasando (0 failures)
- [ ] `PASO 3` Tests de integraciÃ³n pasando con BD real
- [ ] `PASO 4` mTLS completado en [VerifactuSoapClient](src/main/java/levelup42/novapay_backend_hex/infrastructure/adapter/out/fiscal/verifactu/VerifactuSoapClient.java#22-114)
- [ ] `PASO 5` [.env](.env) en [.gitignore](.gitignore)
- [ ] Variable `CERT_P12_BASE64` configurada en el servidor con el `.p12` real de AEAT
- [ ] Variable `CERT_SIGNING_ENABLED=true` en producciÃ³n
- [ ] Variable `VERIFACTU_PRODUCTION=true` en producciÃ³n
- [ ] `JWT_SECRET` cambiado por uno seguro generado con `openssl rand -base64 32`
- [ ] Endpoint VERIFACTU cambiado a URL de producciÃ³n (no PRE)
- [ ] Tag de versiÃ³n en Git antes del primer envÃ­o real a AEAT

