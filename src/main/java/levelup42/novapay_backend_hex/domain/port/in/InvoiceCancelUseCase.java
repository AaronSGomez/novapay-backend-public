package levelup42.novapay_backend_hex.domain.port.in;

public interface InvoiceCancelUseCase {
    InvoiceResult cancel(InvoiceCancelCommand command);
}
