package levelup42.novapay_backend_hex.domain.exception;

/**
 * Excepción lanzada cuando la contraseña proporcionada es inválida en el contexto de cambio de contraseña.
 */
public class InvalidPasswordException extends RuntimeException {

    public InvalidPasswordException(String message) {
        super(message);
    }

    public InvalidPasswordException(String message, Throwable cause) {
        super(message, cause);
    }

    public InvalidPasswordException() {
        super("La contraseña es inválida.");
    }
}
