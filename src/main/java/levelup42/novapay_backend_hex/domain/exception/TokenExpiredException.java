package levelup42.novapay_backend_hex.domain.exception;

/**
 * Excepción lanzada cuando el token ha expirado.
 */
public class TokenExpiredException extends RuntimeException {

    public TokenExpiredException(String message) {
        super(message);
    }

    public TokenExpiredException(String message, Throwable cause) {
        super(message, cause);
    }

    public TokenExpiredException() {
        super("El token ha expirado.");
    }
}
