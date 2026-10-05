package levelup42.novapay_backend_hex.application.service;

import levelup42.novapay_backend_hex.domain.exception.InvoiceNotFoundException;
import levelup42.novapay_backend_hex.domain.model.Invoice;
import levelup42.novapay_backend_hex.domain.port.in.InvoiceGetUseCase;
import levelup42.novapay_backend_hex.domain.port.in.InvoiceResult;
import levelup42.novapay_backend_hex.domain.port.out.InvoiceRepositoryPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class GetInvoiceService implements InvoiceGetUseCase {

    private final InvoiceRepositoryPort invoiceRepository;

    @Override
    @Transactional(readOnly = true)
    public InvoiceResult getById(UUID invoiceId) {
        log.info("Obteniendo factura {}", invoiceId);
        return invoiceRepository.findById(invoiceId)
                .map(this::toResult)
                .orElseThrow(() -> new InvoiceNotFoundException(invoiceId));
    }

    private InvoiceResult toResult(Invoice invoice) {
        return new InvoiceResult(
                invoice.getId(),
                invoice.getInvoiceNumber(),
                invoice.getType(),
                invoice.getStatus(),
                invoice.getIssueDate(),
                invoice.getTotalAmount()
        );
    }
}
