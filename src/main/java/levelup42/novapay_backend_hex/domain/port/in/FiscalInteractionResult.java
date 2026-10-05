package levelup42.novapay_backend_hex.domain.port.in;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

public record FiscalInteractionResult(
        UUID invoiceId,
        String invoiceSeries,
        int invoiceNumber,
        LocalDate issueDate,
        BigDecimal totalAmount,
        String status,
        int retryCount,
        OffsetDateTime sentAt,
        OffsetDateTime respondedAt,
        String secureVerificationCode,
        String verificationUrl,
        String responseCode,
        String responseDescription
) {}