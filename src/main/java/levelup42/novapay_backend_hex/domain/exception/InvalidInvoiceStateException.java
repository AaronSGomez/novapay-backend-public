package levelup42.novapay_backend_hex.domain.exception;

public class InvalidInvoiceStateException extends RuntimeException {
    public InvalidInvoiceStateException(String message) {
        super(message);
    }
}
