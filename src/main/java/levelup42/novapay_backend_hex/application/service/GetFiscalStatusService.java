package levelup42.novapay_backend_hex.application.service;

import levelup42.novapay_backend_hex.domain.exception.FiscalRecordNotFoundException;
import levelup42.novapay_backend_hex.domain.model.FiscalRecord;
import levelup42.novapay_backend_hex.domain.port.in.FiscalGetStatusUseCase;
import levelup42.novapay_backend_hex.domain.port.in.FiscalStatusResult;
import levelup42.novapay_backend_hex.domain.port.out.FiscalRecordRepositoryPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class GetFiscalStatusService implements FiscalGetStatusUseCase {

    private final FiscalRecordRepositoryPort fiscalRecordRepository;
    private final FiscalXmlDataExtractor xmlDataExtractor;

    @Override
    @Transactional(readOnly = true)
    public FiscalStatusResult getStatus(UUID invoiceId) {
        log.info("Consultando estado fiscal para factura {}", invoiceId);

        FiscalRecord record = fiscalRecordRepository.findByInvoiceId(invoiceId)
                .orElseThrow(() -> new FiscalRecordNotFoundException(invoiceId));

        return toResult(record);
    }

    private FiscalStatusResult toResult(FiscalRecord r) {
        FiscalXmlDataExtractor.ExtractedFiscalData extracted = xmlDataExtractor.extract(r.getResponseXml());
        String fallbackDescription = buildUserFacingDescription(r, extracted);
        String fallbackCode = (extracted.responseCode() == null || extracted.responseCode().isBlank())
            ? (r.getStatus().name().startsWith("ERROR") ? "SIGN_OR_SUBMIT_ERROR" : null)
            : extracted.responseCode();
        String verificationUrl = extracted.csv() == null
            ? null
            : "https://www2.agenciatributaria.gob.es/wlpl/inwinv/es/es.aeat.dit.adu.einv.qr.QRWidget?csv=" + extracted.csv();

        return new FiscalStatusResult(
                r.getInvoice().getId(),
                r.getStatus().name(),
                r.getRetryCount(),
                r.getSentAt(),
                r.getRespondedAt(),
            fallbackCode,
            fallbackDescription,
                null, // submissionId
            extracted.csv(),
            verificationUrl
        );
    }

    private String buildUserFacingDescription(FiscalRecord record, FiscalXmlDataExtractor.ExtractedFiscalData extracted) {
        if (extracted.responseDescription() != null && !extracted.responseDescription().isBlank()) {
            return extracted.responseDescription();
        }

        return switch (record.getStatus()) {
            case RECHAZADO -> "Rechazado por AEAT sin detalle adicional.";
            case ERROR_PERMANENTE -> "Error técnico al firmar o enviar a AEAT.";
            case REINTENTO, PENDIENTE_ENVIO, ENVIANDO -> "Pendiente de reenvío a AEAT.";
            default -> null;
        };
    }
}
