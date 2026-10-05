package levelup42.novapay_backend_hex.domain.exception;

import java.util.UUID;

public class InvoiceNotFoundException extends RuntimeException {
    public InvoiceNotFoundException(String message) {
        super(message);
    }
    public InvoiceNotFoundException(UUID id) {
        super("Invoice not found with ID: " + id);
    }
}
