package levelup42.novapay_backend_hex.domain.port.in;

public interface InvoiceCreateUseCase {
    InvoiceResult emit(InvoiceCreateCommand command);
}