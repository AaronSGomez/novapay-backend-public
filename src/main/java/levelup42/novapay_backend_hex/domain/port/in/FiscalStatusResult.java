package levelup42.novapay_backend_hex.domain.port.in;

import java.time.OffsetDateTime;
import java.util.UUID;

public record FiscalStatusResult(
        UUID invoiceId,
        String status,
        int retryCount,
        OffsetDateTime sentAt,
        OffsetDateTime respondedAt,
        String responseCode,
        String responseDescription,
        String submissionId,
        String secureVerificationCode,
        String verificationUrl
) {}
