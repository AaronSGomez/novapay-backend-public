package levelup42.novapay_backend_hex.domain.exception;

/**
 * Excepción lanzada cuando no se encuentra el cliente/usuario por el email o identificador.
 */
public class ClientNotFoundException extends RuntimeException {

    public ClientNotFoundException(String message) {
        super(message);
    }

    public ClientNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }

    public ClientNotFoundException() {
        super("El cliente no fue encontrado.");
    }
}
