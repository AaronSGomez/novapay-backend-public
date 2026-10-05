package levelup42.novapay_backend_hex.domain.exception;

import java.util.UUID;

public class PosTerminalNotFoundException extends RuntimeException {
    public PosTerminalNotFoundException(UUID id) {
        super("POS Terminal not found with id: " + id);
    }
}
