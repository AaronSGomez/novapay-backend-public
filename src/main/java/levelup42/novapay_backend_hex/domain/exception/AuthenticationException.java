package levelup42.novapay_backend_hex.domain.exception;

/**
 * Excepción genérica de autenticación para credenciales inválidas o errores de login.
 */
public class AuthenticationException extends RuntimeException {

    public AuthenticationException(String message) {
        super(message);
    }

    public AuthenticationException(String message, Throwable cause) {
        super(message, cause);
    }

    public AuthenticationException() {
        super("Autenticación fallida. Verifica tus credenciales.");
    }
}
