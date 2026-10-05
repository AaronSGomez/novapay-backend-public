package levelup42.novapay_backend_hex.infrastructure.adapter.in.rest;

import jakarta.validation.Valid;
import io.swagger.v3.oas.annotations.tags.Tag;
import levelup42.novapay_backend_hex.application.service.AuthenticationService;
import levelup42.novapay_backend_hex.domain.exception.AuthenticationException;
import levelup42.novapay_backend_hex.domain.exception.ClientAlreadyExistsException;
import levelup42.novapay_backend_hex.domain.exception.ClientNotFoundException;
import levelup42.novapay_backend_hex.domain.exception.EmailNotVerifiedException;
import levelup42.novapay_backend_hex.domain.exception.InvalidPasswordException;
import levelup42.novapay_backend_hex.domain.exception.InvalidTokenException;
import levelup42.novapay_backend_hex.domain.exception.TokenExpiredException;
import levelup42.novapay_backend_hex.domain.model.ApiClient;
import levelup42.novapay_backend_hex.domain.port.out.ApiClientRepositoryPort;
import levelup42.novapay_backend_hex.infrastructure.adapter.in.rest.dto.AuthRequest;
import levelup42.novapay_backend_hex.infrastructure.adapter.in.rest.dto.AuthResponse;
import levelup42.novapay_backend_hex.infrastructure.adapter.in.rest.dto.ChangePasswordRequest;
import levelup42.novapay_backend_hex.infrastructure.adapter.in.rest.dto.ForgotPasswordRequest;
import levelup42.novapay_backend_hex.infrastructure.adapter.in.rest.dto.LoginRequest;
import levelup42.novapay_backend_hex.infrastructure.adapter.in.rest.dto.RegistroRequest;
import levelup42.novapay_backend_hex.infrastructure.adapter.in.rest.dto.VerifyEmailRequest;
import levelup42.novapay_backend_hex.infrastructure.security.jwt.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Controlador de autenticación.
 * Mantiene endpoint legacy por compatibilidad y añade el flujo nuevo email/password.
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Tag(name = "Autenticacion", description = "Endpoints de autenticacion legacy y email/password")
public class AuthController {

    private final ApiClientRepositoryPort clientRepository;
    private final JwtTokenProvider tokenProvider;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationService authenticationService;

    @Operation(summary = "Obtener JWT (Legacy)", description = "Autentica con clientId/clientSecret")
    @ApiResponse(responseCode = "200", description = "Token generado")
    @ApiResponse(responseCode = "401", description = "Credenciales incorrectas")
    @PostMapping("/token")
    public ResponseEntity<AuthResponse> getToken(@RequestBody AuthRequest request) {
        log.info("Intento de login para clientId: {}", request.clientId());

        ApiClient client = clientRepository.findByClientId(request.clientId())
                .filter(ApiClient::isActive)
                .orElse(null);

        if (client == null || !passwordEncoder.matches(request.clientSecret(), client.getHashedSecret())) {
            log.warn("Login fallido para clientId: {}", request.clientId());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        String token = tokenProvider.generate(client.getClientId(), client.getRoles());
        long expiresIn = tokenProvider.getExpirationMs() / 1000L;

        log.info("Login exitoso para clientId: {}", client.getClientId());
        return ResponseEntity.ok(AuthResponse.minimal(token, expiresIn));
    }

    @Operation(summary = "Registro", description = "Registra usuario con email/password y envia verificacion")
    @PostMapping("/registro")
    public ResponseEntity<Map<String, String>> registro(@Valid @RequestBody RegistroRequest request) {
        try {
            authenticationService.registro(request);
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(Map.of("message", "Usuario registrado. Verifica tu email"));
        } catch (ClientAlreadyExistsException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("error", e.getMessage()));
        }
    }

    @Operation(summary = "Login", description = "Autentica con email/password")
    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequest request) {
        try {
            return ResponseEntity.ok(authenticationService.login(request));
        } catch (ClientNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", e.getMessage()));
        } catch (EmailNotVerifiedException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", e.getMessage()));
        } catch (AuthenticationException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", e.getMessage()));
        }
    }

    @Operation(summary = "Verificar email", description = "Valida token de verificacion")
    @PostMapping("/verify-email")
    public ResponseEntity<Map<String, String>> verifyEmail(@Valid @RequestBody VerifyEmailRequest request) {
        try {
            authenticationService.verificarEmail(request.getVerificationToken());
            return ResponseEntity.ok(Map.of("message", "Email verificado exitosamente"));
        } catch (InvalidTokenException | TokenExpiredException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", e.getMessage()));
        }
    }

    @Operation(summary = "Forgot password", description = "Genera y envia password temporal")
    @PostMapping("/forgot-password")
    public ResponseEntity<Map<String, String>> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        try {
            authenticationService.forgotPassword(request.getEmail());
            return ResponseEntity.ok(Map.of("message", "Contrasena temporal enviada"));
        } catch (ClientNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", e.getMessage()));
        }
    }

    @Operation(summary = "Change password", description = "Cambia password usando temporal o current")
    @PostMapping("/change-password")
    public ResponseEntity<Map<String, String>> changePassword(
            @RequestHeader(name = "X-Client-Id") String clientId,
            @Valid @RequestBody ChangePasswordRequest request) {
        try {
            authenticationService.changePassword(clientId, request);
            return ResponseEntity.ok(Map.of("message", "Contrasena cambiada exitosamente"));
        } catch (ClientNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", e.getMessage()));
        } catch (InvalidPasswordException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", e.getMessage()));
        }
    }
}
