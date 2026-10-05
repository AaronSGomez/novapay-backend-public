package levelup42.novapay_backend_hex.domain.exception;

import java.util.UUID;

public class FiscalRecordNotFoundException extends RuntimeException {
    public FiscalRecordNotFoundException(UUID id) {
        super("Fiscal Record not found with id: " + id);
    }
}
