package levelup42.novapay_backend_hex.infrastructure.adapter.in.rest.dto;

import lombok.Data;
import java.util.UUID;

@Data
public class InvoiceCancelRequestDTO {
    private UUID invoiceId;
    private String reason;
}
