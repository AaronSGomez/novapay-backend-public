package levelup42.novapay_backend_hex.domain.exception;

import java.util.UUID;

public class CompanyNotFoundException extends RuntimeException {
    public CompanyNotFoundException(String message) {
        super(message);
    }
    public CompanyNotFoundException(UUID id) {
        super("Company not found with ID: " + id);
    }
}
