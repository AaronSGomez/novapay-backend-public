package levelup42.novapay_backend_hex.infrastructure.adapter.in.rest.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * DTO para respuesta de autenticación exitosa.
 * Contiene el JWT token, información del cliente (email, estado de verificación) y expiración.
 * Utiliza record de Java 16+ para inmutabilidad automática.
 */
public record AuthResponse(
    @JsonProperty("access_token") String accessToken,
    @JsonProperty("token_type") String tokenType,
    @JsonProperty("expires_in") long expiresIn,
    @JsonProperty("clientId") String clientId,
    @JsonProperty("email") String email,
    @JsonProperty("email_verified") boolean emailVerified,
    @JsonProperty("message") String message
) {
    /**
     * Constructor compacto para respuesta mínima (compatibilidad con código legado).
     */
    public static AuthResponse minimal(String accessToken, long expiresIn) {
        return new AuthResponse(accessToken, "Bearer", expiresIn, null, null, false, null);
    }

    /**
     * Constructor completo con todos los detalles.
     */
    public static AuthResponse complete(String accessToken, long expiresIn, String email, boolean emailVerified, String message) {
        return new AuthResponse(accessToken, "Bearer", expiresIn, null, email, emailVerified, message);
    }

    public static AuthResponse completeWithClient(
            String accessToken,
            long expiresIn,
            String clientId,
            String email,
            boolean emailVerified,
            String message
    ) {
        return new AuthResponse(accessToken, "Bearer", expiresIn, clientId, email, emailVerified, message);
    }
}
