package levelup42.novapay_backend_hex.domain.exception;

/**
 * Excepción lanzada cuando se intenta autenticar con un cliente inactivo.
 */
public class ClientNotActiveException extends RuntimeException {

    public ClientNotActiveException(String message) {
        super(message);
    }

    public ClientNotActiveException(String message, Throwable cause) {
        super(message, cause);
    }

    public ClientNotActiveException() {
        super("El cliente no está activo.");
    }
}
