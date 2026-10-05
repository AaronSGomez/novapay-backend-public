package levelup42.novapay_backend_hex.application.service;

import levelup42.novapay_backend_hex.domain.model.FiscalRecord;
import levelup42.novapay_backend_hex.domain.port.in.FiscalInteractionResult;
import levelup42.novapay_backend_hex.domain.port.in.FiscalListInteractionsUseCase;
import levelup42.novapay_backend_hex.domain.port.out.ApiClientRepositoryPort;
import levelup42.novapay_backend_hex.domain.port.out.FiscalRecordRepositoryPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ListFiscalInteractionsService implements FiscalListInteractionsUseCase {

    private final FiscalRecordRepositoryPort fiscalRecordRepository;
    private final ApiClientRepositoryPort apiClientRepository;
    private final FiscalXmlDataExtractor xmlDataExtractor;

    @Override
    @Transactional(readOnly = true)
    public List<FiscalInteractionResult> listInteractions(int limit) {
        int safeLimit = Math.min(Math.max(limit, 1), 500);
        log.info("Listado de interacciones fiscales. limit={}", safeLimit);

        return fiscalRecordRepository.findLatest(safeLimit).stream()
                .map(this::toResult)
                .toList();
    }

        @Override
        @Transactional(readOnly = true)
        public List<FiscalInteractionResult> listInteractionsForClient(String clientId, int limit) {
        int safeLimit = Math.min(Math.max(limit, 1), 500);
        log.info("Listado de interacciones fiscales por cliente. clientId={} limit={}", clientId, safeLimit);

        return apiClientRepository.findByClientId(clientId)
            .map(apiClient -> apiClient.getLinkedCompanyId())
            .map(companyId -> fiscalRecordRepository.findLatestByCompanyId(companyId, safeLimit).stream()
                .map(this::toResult)
                .toList())
            .orElseGet(List::of);
        }

    private FiscalInteractionResult toResult(FiscalRecord record) {
        FiscalXmlDataExtractor.ExtractedFiscalData extracted = xmlDataExtractor.extract(record.getResponseXml());
        String fallbackDescription = buildUserFacingDescription(record, extracted);
        String fallbackCode = (extracted.responseCode() == null || extracted.responseCode().isBlank())
            ? (record.getStatus().name().startsWith("ERROR") ? "SIGN_OR_SUBMIT_ERROR" : null)
            : extracted.responseCode();
        String verificationUrl = extracted.csv() == null
                ? null
                : "https://www2.agenciatributaria.gob.es/wlpl/inwinv/es/es.aeat.dit.adu.einv.qr.QRWidget?csv=" + extracted.csv();

        return new FiscalInteractionResult(
                record.getInvoice().getId(),
                record.getInvoice().getInvoiceNumber().getSeries(),
                record.getInvoice().getInvoiceNumber().getNumber(),
                record.getInvoice().getIssueDate(),
                record.getInvoice().getTotalAmount() != null ? record.getInvoice().getTotalAmount().getValue() : null,
                record.getStatus().name(),
                record.getRetryCount(),
                record.getSentAt(),
                record.getRespondedAt(),
                extracted.csv(),
                verificationUrl,
                fallbackCode,
                fallbackDescription
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