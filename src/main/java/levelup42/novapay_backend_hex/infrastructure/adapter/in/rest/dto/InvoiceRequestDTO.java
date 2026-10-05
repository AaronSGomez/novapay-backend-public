package levelup42.novapay_backend_hex.infrastructure.adapter.in.rest.dto;

import levelup42.novapay_backend_hex.domain.model.enums.InvoiceType;
import lombok.Data;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Data
public class InvoiceRequestDTO {
    private String series;
    private int number;
    private InvoiceType type;
    private UUID companyId;
    private UUID terminalId;
    private LocalDate issueDate;
    private List<InvoiceLineRequestDTO> lines;
    private UUID rectifiedInvoiceId;
}
