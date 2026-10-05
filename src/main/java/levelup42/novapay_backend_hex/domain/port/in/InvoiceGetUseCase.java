package levelup42.novapay_backend_hex.domain.port.in;

import java.util.UUID;

public interface InvoiceGetUseCase {
    InvoiceResult getById(UUID invoiceId);
}
