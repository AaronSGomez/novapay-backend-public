package levelup42.novapay_backend_hex.domain.exception;

/**
 * Excepción lanzada cuando se intenta registrar un cliente con un email que ya existe.
 */
public class ClientAlreadyExistsException extends RuntimeException {

    public ClientAlreadyExistsException(String message) {
        super(message);
    }

    public ClientAlreadyExistsException(String message, Throwable cause) {
        super(message, cause);
    }

    public ClientAlreadyExistsException() {
        super("El email ya está registrado.");
    }
}
