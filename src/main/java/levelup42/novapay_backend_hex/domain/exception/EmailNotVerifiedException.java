package levelup42.novapay_backend_hex.domain.exception;

/**
 * Excepción lanzada cuando se intenta autenticar con un usuario cuyo email no está verificado.
 */
public class EmailNotVerifiedException extends RuntimeException {

    public EmailNotVerifiedException(String message) {
        super(message);
    }

    public EmailNotVerifiedException(String message, Throwable cause) {
        super(message, cause);
    }

    public EmailNotVerifiedException() {
        super("El email no ha sido verificado. Por favor verifica tu email primero.");
    }
}
