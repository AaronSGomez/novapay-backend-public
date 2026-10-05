package levelup42.novapay_backend_hex.infrastructure.adapter.in.rest.dto;

import levelup42.novapay_backend_hex.domain.model.enums.TaxType;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class InvoiceLineRequestDTO {
    private String description;
    private BigDecimal quantity;
    private BigDecimal unitPrice;
    private TaxType taxType;
}
