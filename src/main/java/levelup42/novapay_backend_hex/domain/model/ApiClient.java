package levelup42.novapay_backend_hex.domain.model;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Modelo de dominio para clientes de la API NOVAPAY.
 * Soporta:
 * - Autenticación por email + password (nuevo)
 * - Verificación de email (nuevo)
 * - Recuperación de contraseña (nuevo)
 * - Migración de firma fiscal con hash anterior (nuevo)
 */
public class ApiClient {

    private String id;
    private String clientId;              // Identificador único (legado o generado)
    private String email;                 // Email único para login
    private String hashedSecret;          // BCrypt hash de la contraseña
    private List<String> roles;           // Ej: ["ROLE_INVOICER", "ROLE_FISCAL"]
    private boolean active;
    
    // Verificación de email
    private boolean emailVerified;
    private String emailVerificationToken;
    private OffsetDateTime emailVerificationTokenExpires;
    
    // Recuperación de contraseña
    private String passwordResetToken;
    private OffsetDateTime passwordResetTokenExpires;
    private String resetPasswordTemporary;
    
    // Migración fiscal
    private String clientSigningPreviousHash;
    private UUID linkedCompanyId;
    
    // Auditoría
    private OffsetDateTime createdAt;
    private OffsetDateTime lastLogin;

    // Constructor completo
    public ApiClient(String id, String clientId, String email, String hashedSecret, 
                     List<String> roles, boolean active, boolean emailVerified,
                     String emailVerificationToken, OffsetDateTime emailVerificationTokenExpires,
                     String passwordResetToken, OffsetDateTime passwordResetTokenExpires,
                     String resetPasswordTemporary, String clientSigningPreviousHash,
                     UUID linkedCompanyId, OffsetDateTime createdAt, OffsetDateTime lastLogin) {
        this.id = id;
        this.clientId = clientId;
        this.email = email;
        this.hashedSecret = hashedSecret;
        this.roles = roles;
        this.active = active;
        this.emailVerified = emailVerified;
        this.emailVerificationToken = emailVerificationToken;
        this.emailVerificationTokenExpires = emailVerificationTokenExpires;
        this.passwordResetToken = passwordResetToken;
        this.passwordResetTokenExpires = passwordResetTokenExpires;
        this.resetPasswordTemporary = resetPasswordTemporary;
        this.clientSigningPreviousHash = clientSigningPreviousHash;
        this.linkedCompanyId = linkedCompanyId;
        this.createdAt = createdAt;
        this.lastLogin = lastLogin;
    }

    // Getters
    public String getId()                           { return id; }
    public String getClientId()                     { return clientId; }
    public String getEmail()                        { return email; }
    public String getHashedSecret()                 { return hashedSecret; }
    public List<String> getRoles()                  { return roles; }
    public boolean isActive()                       { return active; }
    public boolean isEmailVerified()                { return emailVerified; }
    public String getEmailVerificationToken()       { return emailVerificationToken; }
    public OffsetDateTime getEmailVerificationTokenExpires() { return emailVerificationTokenExpires; }
    public String getPasswordResetToken()           { return passwordResetToken; }
    public OffsetDateTime getPasswordResetTokenExpires() { return passwordResetTokenExpires; }
    public String getResetPasswordTemporary()       { return resetPasswordTemporary; }
    public String getClientSigningPreviousHash()    { return clientSigningPreviousHash; }
    public UUID getLinkedCompanyId()                { return linkedCompanyId; }
    public OffsetDateTime getCreatedAt()            { return createdAt; }
    public OffsetDateTime getLastLogin()            { return lastLogin; }

    // Setters
    public void setEmail(String email)                                       { this.email = email; }
    public void setHashedSecret(String hashedSecret)                         { this.hashedSecret = hashedSecret; }
    public void setActive(boolean active)                                    { this.active = active; }
    public void setEmailVerified(boolean emailVerified)                      { this.emailVerified = emailVerified; }
    public void setEmailVerificationToken(String token)                      { this.emailVerificationToken = token; }
    public void setEmailVerificationTokenExpires(OffsetDateTime expires)     { this.emailVerificationTokenExpires = expires; }
    public void setPasswordResetToken(String token)                          { this.passwordResetToken = token; }
    public void setPasswordResetTokenExpires(OffsetDateTime expires)         { this.passwordResetTokenExpires = expires; }
    public void setResetPasswordTemporary(String temp)                       { this.resetPasswordTemporary = temp; }
    public void setClientSigningPreviousHash(String hash)                    { this.clientSigningPreviousHash = hash; }
    public void setLinkedCompanyId(UUID companyId)                           { this.linkedCompanyId = companyId; }
    public void setLastLogin(OffsetDateTime lastLogin)                       { this.lastLogin = lastLogin; }

    // Métodos de negocio
    /**
     * Verifica si el token de verificación de email es válido (no expirado)
     */
    public boolean isEmailVerificationTokenValid() {
        return emailVerificationToken != null && 
               emailVerificationTokenExpires != null &&
               emailVerificationTokenExpires.isAfter(OffsetDateTime.now());
    }

    /**
     * Verifica si el token de reset de contraseña es válido (no expirado)
     */
    public boolean isPasswordResetTokenValid() {
        return passwordResetToken != null && 
               passwordResetTokenExpires != null &&
               passwordResetTokenExpires.isAfter(OffsetDateTime.now());
    }

    /**
     * Limpia los tokens de verificación de email
     */
    public void clearEmailVerification() {
        this.emailVerificationToken = null;
        this.emailVerificationTokenExpires = null;
    }

    /**
     * Limpia los tokens y datos de reset de contraseña
     */
    public void clearPasswordReset() {
        this.passwordResetToken = null;
        this.passwordResetTokenExpires = null;
        this.resetPasswordTemporary = null;
    }
}
