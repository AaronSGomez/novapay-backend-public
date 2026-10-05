package levelup42.novapay_backend_hex.domain.port.in;

import levelup42.novapay_backend_hex.domain.model.enums.InvoiceType;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record InvoiceCreateCommand(
        String series,
        Integer number,
        InvoiceType type,
        UUID companyId,
        UUID terminalId,
        LocalDate issueDate,
        List<InvoiceLineCommand> lines,
        UUID rectifiedInvoiceId
) {}
