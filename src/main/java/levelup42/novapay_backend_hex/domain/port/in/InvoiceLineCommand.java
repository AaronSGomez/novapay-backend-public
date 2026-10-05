package levelup42.novapay_backend_hex.domain.port.in;

import levelup42.novapay_backend_hex.domain.model.enums.TaxType;
import levelup42.novapay_backend_hex.domain.model.valueObject.Money;

import java.math.BigDecimal;

public record InvoiceLineCommand(
        String description,
        BigDecimal quantity,
        Money unitPrice,
        TaxType taxType
) {}
