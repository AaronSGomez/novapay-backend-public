package levelup42.novapay_backend_hex.domain.exception;

/**
 * Excepción lanzada cuando el token proporcionado es inválido o no existe.
 */
public class InvalidTokenException extends RuntimeException {

    public InvalidTokenException(String message) {
        super(message);
    }

    public InvalidTokenException(String message, Throwable cause) {
        super(message, cause);
    }

    public InvalidTokenException() {
        super("El token proporcionado es inválido.");
    }
}
