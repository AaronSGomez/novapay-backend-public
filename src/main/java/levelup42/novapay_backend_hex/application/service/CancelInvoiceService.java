package levelup42.novapay_backend_hex.application.service;

import levelup42.novapay_backend_hex.domain.exception.InvalidInvoiceStateException;
import levelup42.novapay_backend_hex.domain.exception.InvoiceNotFoundException;
import levelup42.novapay_backend_hex.domain.model.Invoice;
import levelup42.novapay_backend_hex.domain.model.enums.FiscalStatus;
import levelup42.novapay_backend_hex.domain.model.enums.InvoiceStatus;
import levelup42.novapay_backend_hex.domain.port.in.InvoiceCancelCommand;
import levelup42.novapay_backend_hex.domain.port.in.InvoiceCancelUseCase;
import levelup42.novapay_backend_hex.domain.port.in.InvoiceResult;
import levelup42.novapay_backend_hex.domain.port.out.FiscalAgencyPort;
import levelup42.novapay_backend_hex.domain.port.out.FiscalRecordRepositoryPort;
import levelup42.novapay_backend_hex.domain.port.out.InvoiceRepositoryPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class CancelInvoiceService implements InvoiceCancelUseCase {

    private final InvoiceRepositoryPort invoiceRepository;
    private final FiscalRecordRepositoryPort fiscalRecordRepository;
    private final FiscalAgencyPort fiscalAgencyPort;

    @Override
    @Transactional
    public InvoiceResult cancel(InvoiceCancelCommand command) {
        log.info("Anulando factura {}: {}", command.invoiceId(), command.reason());

        Invoice invoice = invoiceRepository.findById(command.invoiceId())
                .orElseThrow(() -> new InvoiceNotFoundException(command.invoiceId()));

        if (invoice.getStatus() != InvoiceStatus.EMITIDA) {
            throw new InvalidInvoiceStateException(
                    "Solo se pueden anular facturas en estado EMITIDA. Estado actual: " + invoice.getStatus());
        }

        var record = fiscalRecordRepository.findByInvoiceId(invoice.getId())
                .orElseThrow(() -> new InvalidInvoiceStateException(
                        "No existe registro fiscal para la factura a anular"
                ));

        record.setStatus(FiscalStatus.PENDIENTE_ENVIO);
        var savedRecord = fiscalRecordRepository.save(record);

        try {
            fiscalAgencyPort.cancel(savedRecord);
        } catch (Exception e) {
            log.error("Error en anulación fiscal para factura {}: {}", invoice.getId(), e.getMessage());
            throw new InvalidInvoiceStateException(
                    "No se pudo anular en AEAT: " + e.getMessage()
            );
        }

        invoice.setStatus(InvoiceStatus.ANULADA);
        Invoice savedInvoice = invoiceRepository.save(invoice);

        return toResult(savedInvoice);
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
