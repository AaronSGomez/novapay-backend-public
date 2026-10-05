package levelup42.novapay_backend_hex.domain.port.in;

import java.util.UUID;

public interface FiscalRetrySubmitUseCase {
    void retry(UUID invoiceId);
}
