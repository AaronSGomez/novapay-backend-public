package levelup42.novapay_backend_hex.application.service;

import levelup42.novapay_backend_hex.domain.model.ApiClient;
import levelup42.novapay_backend_hex.domain.exception.*;
import levelup42.novapay_backend_hex.domain.port.out.ApiClientRepositoryPort;
import levelup42.novapay_backend_hex.domain.port.out.email.EmailServicePort;
import levelup42.novapay_backend_hex.domain.service.TokenGenerationService;
import levelup42.novapay_backend_hex.infrastructure.adapter.in.rest.dto.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Servicio de aplicación para autenticación.
 * Orquesta los flujos de:
 * - Registro con verificación de email obligatoria
 * - Login con email + contraseña
 * - Verificación de email
 * - Recuperación de contraseña (temporal)
 * - Cambio de contraseña
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthenticationService {

    private final ApiClientRepositoryPort clientRepo;
    private final EmailServicePort emailService;
    private final PasswordEncoder passwordEncoder;

    private static final long EMAIL_VERIFICATION_TOKEN_EXPIRY_MINUTES = 24 * 60; // 24 horas
    private static final long PASSWORD_RESET_TOKEN_EXPIRY_MINUTES = 30; // 30 minutos

    /**
     * Registra un nuevo cliente.
     * Valida que el email no exista, encripta la contraseña,
     * genera token de verificación y lo envía por email.
     * El cliente no puede hacer login: hasta verificar el email.
     *
     * @param request datos del registro
     * @throws ClientAlreadyExistsException si el email ya está registrado
     */
    @Transactional
    public void registro(RegistroRequest request) {
        // Verificar que el email no existe
        if (clientRepo.findByEmail(request.getEmail()).isPresent()) {
            throw new ClientAlreadyExistsException("El email " + request.getEmail() + " ya está registrado");
        }

        // Generar token de verificación (raw)
        String verificationTokenRaw = TokenGenerationService.generateRandomToken();
        String verificationTokenHashed = TokenGenerationService.hashToken(verificationTokenRaw);

        // Crear cliente
        ApiClient client = new ApiClient(
            null,                                                  // id (lo genera JPA)
                UUID.randomUUID().toString(),                          // clientId único
                request.getEmail(),                                    // email
                passwordEncoder.encode(request.getPassword()),         // hashedSecret (BCrypt)
                List.of("ROLE_INVOICER"),                             // roles por defecto
                true,                                                  // active
                false,                                                 // emailVerified = false (debe verificarse)
                verificationTokenHashed,                               // emailVerificationToken (hasheado)
                OffsetDateTime.now().plusMinutes(EMAIL_VERIFICATION_TOKEN_EXPIRY_MINUTES), // expiry
                null,                                                  // passwordResetToken
                null,                                                  // passwordResetTokenExpires
                null,                                                  // resetPasswordTemporary
                request.getClientSigningPreviousHash(),               // fiscales opcionales
                request.getCompanyId(),
                OffsetDateTime.now(),                                  // createdAt
                null                                                   // lastLogin
        );

        // Guardar cliente
        clientRepo.save(client);

        // Enviar email con token raw (sin hashear)
        emailService.sendVerificationEmail(request.getEmail(), verificationTokenRaw);

        log.info("✅ Client registered: {}", request.getEmail());
    }

    /**
     * Autentica un cliente con email + contraseña.
     * Valida que exista el cliente, contraseña sea correcta,
     * email esté verificado y cliente esté activo.
     *
     * @param request email + password
     * @return response con JWT token
     * @throws ClientNotFoundException si no existe el email
     * @throws AuthenticationException si la contraseña es incorrecta
     * @throws EmailNotVerifiedException si el email no está verificado
     * @throws ClientNotActiveException si el cliente no está activo
     */
    @Transactional
    public AuthResponse login(LoginRequest request) {
        // Buscar cliente por email
        ApiClient client = clientRepo.findByEmail(request.getEmail())
                .orElseThrow(() -> new ClientNotFoundException("Cliente no encontrado: " + request.getEmail()));

        // Verificar cliente está activo
        if (!client.isActive()) {
            throw new ClientNotActiveException("El cliente no está activo: " + request.getEmail());
        }

        // Verificar contraseña
        if (!passwordEncoder.matches(request.getPassword(), client.getHashedSecret())) {
            throw new AuthenticationException("Email o contraseña incorrecta");
        }

        // Verificar email está verificado
        if (!client.isEmailVerified()) {
            throw new EmailNotVerifiedException("El email " + request.getEmail() + " no ha sido verificado");
        }

        // Actualizar lastLogin
        client.setLastLogin(OffsetDateTime.now());
        clientRepo.save(client);

        log.info("✅ Client logged in: {}", request.getEmail());

        // Generar JWT (se delega a JwtTokenProvider)
        // NOTA: Este método debería retornar el token JWT generado por JwtTokenProvider
        // Por ahora retornamos respuesta básica. Se integrará con JwtTokenProvider existente.
        return AuthResponse.completeWithClient(
            "jwt-token-placeholder", // Se reemplazará con jwtTokenProvider.generateToken(client)
            3600, // 1 hora
            client.getClientId(),
            client.getEmail(),
            client.isEmailVerified(),
            "Login exitoso"
        );
    }

    /**
     * Verifica el email del cliente usando el token.
     * Busca el cliente por token hasheado, valida expiración,
     * marca email como verificado y limpia tokens.
     *
     * @param verificationTokenRaw token raw recibido en la request
     * @throws InvalidTokenException si el token no es válido o expiró
     */
    @Transactional
    public void verificarEmail(String verificationTokenRaw) {
        String verificationTokenHashed = TokenGenerationService.hashToken(verificationTokenRaw);

        // Buscar cliente por token hasheado
        ApiClient client = clientRepo.findByEmailVerificationToken(verificationTokenHashed)
                .orElseThrow(() -> new InvalidTokenException("Token de verificación inválido"));

        // Verificar que el token no está expirado
        if (!client.isEmailVerificationTokenValid()) {
            throw new TokenExpiredException("El token de verificación ha expirado");
        }

        // Marcar email como verificado y limpiar tokens
        client.setEmailVerified(true);
        client.clearEmailVerification();
        clientRepo.save(client);

        log.info("✅ Email verified: {}", client.getEmail());
    }

    /**
     * Inicia recuperación de contraseña.
     * Genera contraseña temporal, la envía por email,
     * y almacena el token reseteo en la BD.
     *
     * @param email del cliente
     * @throws ClientNotFoundException si no existe el email
     */
    @Transactional
    public void forgotPassword(String email) {
        ApiClient client = clientRepo.findByEmail(email)
                .orElseThrow(() -> new ClientNotFoundException("Cliente no encontrado: " + email));

        // Generar contraseña temporal (12 chars cumple requisitos)
        String temporalPassword = TokenGenerationService.generateTemporaryPassword();

        // Generar token reset
        String resetTokenRaw = TokenGenerationService.generateRandomToken();
        String resetTokenHashed = TokenGenerationService.hashToken(resetTokenRaw);

        // Guardar cambios en cliente
        client.setResetPasswordTemporary(temporalPassword);
        client.setPasswordResetToken(resetTokenHashed);
        client.setPasswordResetTokenExpires(
                OffsetDateTime.now().plusMinutes(PASSWORD_RESET_TOKEN_EXPIRY_MINUTES)
        );
        clientRepo.save(client);

        // Enviar email con contraseña temporal
        emailService.sendTemporaryPasswordEmail(email, temporalPassword);

        log.info("✅ Temporary password sent to: {}", email);
    }

    /**
     * Cambia la contraseña del cliente.
     * Soporta dos flujos:
     * 1. Post-recuperación: verifica temporalPassword
     * 2. Autenticado: verifica currentPassword
     *
     * @param clientId del cliente
     * @param request con nueva contraseña y verificación del flujo
     * @throws ClientNotFoundException si no existe el cliente
     * @throws invalidPasswordException si la contraseña actual/temporal es incorrecta
     */
    @Transactional
    public void changePassword(String clientId, ChangePasswordRequest request) {
        ApiClient client = clientRepo.findByClientId(clientId)
                .orElseThrow(() -> new ClientNotFoundException("Cliente no encontrado: " + clientId));

        // Flujo 1: Cambio post-recuperación
        if (request.getTemporalPassword() != null) {
            if (!request.getTemporalPassword().equals(client.getResetPasswordTemporary())) {
                throw new InvalidPasswordException("Contraseña temporal inválida");
            }
            // Válida: cambiar contraseña
        }
        // Flujo 2: Cambio autenticado
        else if (request.getCurrentPassword() != null) {
            if (!passwordEncoder.matches(request.getCurrentPassword(), client.getHashedSecret())) {
                throw new InvalidPasswordException("Contraseña actual incorrecta");
            }
            // Válida: cambiar contraseña
        }

        // Encriptar nueva contraseña con BCrypt
        String newHashedPassword = passwordEncoder.encode(request.getNewPassword());
        client.setHashedSecret(newHashedPassword);

        // Limpiar tokens de reset
        client.clearPasswordReset();

        // Guardar cambios
        clientRepo.save(client);

        // Enviar confirmación por email
        emailService.sendPasswordChangedConfirmationEmail(client.getEmail());

        log.info("✅ Password changed: {}", client.getEmail());
    }
}
