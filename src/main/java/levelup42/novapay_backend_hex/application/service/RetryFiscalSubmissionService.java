package levelup42.novapay_backend_hex.application.service;

import levelup42.novapay_backend_hex.domain.exception.FiscalRecordNotFoundException;
import levelup42.novapay_backend_hex.domain.exception.InvoiceNotFoundException;
import levelup42.novapay_backend_hex.domain.exception.InvalidInvoiceStateException;
import levelup42.novapay_backend_hex.domain.model.FiscalRecord;
import levelup42.novapay_backend_hex.domain.model.Invoice;
import levelup42.novapay_backend_hex.domain.model.enums.FiscalStatus;
import levelup42.novapay_backend_hex.domain.port.in.FiscalRetrySubmitUseCase;
import levelup42.novapay_backend_hex.domain.port.out.FiscalAgencyPort;
import levelup42.novapay_backend_hex.domain.port.out.FiscalRecordRepositoryPort;
import levelup42.novapay_backend_hex.domain.port.out.InvoiceRepositoryPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class RetryFiscalSubmissionService implements FiscalRetrySubmitUseCase {

    private final InvoiceRepositoryPort invoiceRepository;
    private final FiscalRecordRepositoryPort fiscalRecordRepository;
    private final FiscalAgencyPort fiscalAgencyPort;
    private final TaxCalculationService taxCalculationService;

    @Override
    @Transactional
    public void retry(UUID invoiceId) {
        log.info("Reintentando envío fiscal para factura {}", invoiceId);

        Invoice invoice = invoiceRepository.findById(invoiceId)
                .orElseThrow(() -> new InvoiceNotFoundException(invoiceId));

        // En reintentos, algunas facturas recuperadas de persistencia pueden no traer breakdowns mapeados.
        if ((invoice.getBreakdowns() == null || invoice.getBreakdowns().isEmpty())
            && invoice.getLines() != null
            && !invoice.getLines().isEmpty()) {
            invoice.setBreakdowns(taxCalculationService.calculateTaxBreakdown(invoice.getLines(), invoice));
            log.info("Reintento {}: breakdowns reconstruidos desde líneas ({})", invoiceId, invoice.getBreakdowns().size());
        }

        FiscalRecord record = fiscalRecordRepository.findByInvoiceId(invoice.getId())
                .orElseThrow(() -> new FiscalRecordNotFoundException(invoiceId));

        if (record.getStatus() == FiscalStatus.ACEPTADO) {
            log.warn("La factura {} ya está aceptada por la agencia fiscal. No se reintenta.", invoiceId);
            return;
        }

        String rebuiltPreviousHash = resolveLatestAcceptedHash(invoice);
        if (rebuiltPreviousHash != null && !rebuiltPreviousHash.isBlank()) {
            record.setPreviousHash(rebuiltPreviousHash);
            log.info("Reintento {}: previousHash reconstruido desde último ACEPTADO", invoiceId);
        } else if (record.getPreviousHash() == null || record.getPreviousHash().isBlank()) {
            if (hasPreviousRecordsInCompany(invoice, invoiceId)) {
                throw new InvalidInvoiceStateException(
                        "No se puede reintentar la firma: existe historial fiscal previo pero la cadena hash está vacía"
                );
            }
            log.warn("Reintento {}: no se pudo reconstruir previousHash; se mantiene flujo como primer registro", invoiceId);
        }

        // Limpiamos restos del intento anterior para forzar una reconstrucción completa del XML.
        record.setCurrentHash(null);
        record.setSentXml(null);
        record.setResponseXml(null);
        record.setSentAt(null);
        record.setRespondedAt(null);

        record.setRetryCount(record.getRetryCount() + 1);
        record.setStatus(FiscalStatus.REINTENTO);
        FiscalRecord savedRecord = fiscalRecordRepository.save(record);

        try {
            fiscalAgencyPort.submit(savedRecord);

            FiscalRecord latestRecord = fiscalRecordRepository.findByInvoiceId(invoiceId).orElse(savedRecord);
            if (latestRecord.getStatus() == FiscalStatus.RECHAZADO) {
                String rejectionDetail = buildRejectionDetail(latestRecord.getResponseXml());
                throw new InvalidInvoiceStateException(rejectionDetail);
            }

            log.info("Reintento completado para factura {} con estado {}", invoiceId, latestRecord.getStatus());
        } catch (FiscalAgencyPort.FiscalAgencyException e) {
            log.error("Error en reintento fiscal para factura {}: {}", invoiceId, e.getMessage());

            FiscalRecord failedRecord = fiscalRecordRepository.findByInvoiceId(invoiceId).orElse(savedRecord);
            if (e.isRetryable()) {
                failedRecord.setStatus(FiscalStatus.REINTENTO);
            } else {
                failedRecord.setStatus(FiscalStatus.RECHAZADO);
            }

            fiscalRecordRepository.save(failedRecord);
        }
    }

    private String buildRejectionDetail(String responseXml) {
        String code = extractTagValue(responseXml, "CodigoErrorRegistro");
        String description = extractTagValue(responseXml, "DescripcionErrorRegistro");

        if ("3000".equals(code)) {
            return "Reintento rechazado por AEAT [3000]: Registro de facturación duplicado. "
                    + "No procede reenvío; usa subsanación (anulación y emisión de rectificativa).";
        }

        if (code != null && description != null) {
            return "Reintento rechazado por AEAT [" + code + "]: " + description;
        }
        if (description != null) {
            return "Reintento rechazado por AEAT: " + description;
        }
        return "Reintento rechazado por AEAT sin detalle adicional.";
    }

    private String extractTagValue(String xml, String tagName) {
        if (xml == null || xml.isBlank()) {
            return null;
        }
        Pattern pattern = Pattern.compile("<(?:\\w+:)?" + tagName + ">([^<]+)</(?:\\w+:)?" + tagName + ">", Pattern.CASE_INSENSITIVE);
        Matcher matcher = pattern.matcher(xml);
        if (!matcher.find()) {
            return null;
        }
        String value = matcher.group(1);
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

    private String resolveLatestAcceptedHash(Invoice invoice) {
        return fiscalRecordRepository.findLatestByCompanyId(invoice.getCompany().getId(), 200)
                .stream()
                .filter(r -> r.getInvoice() != null && !invoice.getId().equals(r.getInvoice().getId()))
                .filter(r -> r.getStatus() == FiscalStatus.ACEPTADO)
                .map(FiscalRecord::getCurrentHash)
                .filter(hash -> hash != null && !hash.isBlank())
                .findFirst()
                .orElse(null);
    }

    private boolean hasPreviousRecordsInCompany(Invoice invoice, UUID currentInvoiceId) {
        return fiscalRecordRepository.findLatestByCompanyId(invoice.getCompany().getId(), 200)
                .stream()
                .anyMatch(r -> r.getInvoice() != null && !currentInvoiceId.equals(r.getInvoice().getId()));
    }
}
