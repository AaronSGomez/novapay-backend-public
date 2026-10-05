package levelup42.novapay_backend_hex.domain.port.in;

import levelup42.novapay_backend_hex.domain.model.enums.InvoiceStatus;
import levelup42.novapay_backend_hex.domain.model.enums.InvoiceType;
import levelup42.novapay_backend_hex.domain.model.valueObject.InvoiceNumber;
import levelup42.novapay_backend_hex.domain.model.valueObject.Money;

import java.time.LocalDate;
import java.util.UUID;

public record InvoiceResult(
        UUID id,
        InvoiceNumber invoiceNumber,
        InvoiceType type,
        InvoiceStatus status,
        LocalDate issueDate,
        Money totalAmount
) {}
