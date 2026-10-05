package levelup42.novapay_backend_hex.application.service;

import levelup42.novapay_backend_hex.domain.model.FiscalRecord;
import levelup42.novapay_backend_hex.domain.model.Invoice;
import levelup42.novapay_backend_hex.domain.exception.InvalidInvoiceStateException;
import levelup42.novapay_backend_hex.domain.model.enums.FiscalRecordType;
import levelup42.novapay_backend_hex.domain.model.enums.FiscalStatus;
import levelup42.novapay_backend_hex.domain.port.out.ApiClientRepositoryPort;
import levelup42.novapay_backend_hex.domain.port.out.FiscalRecordRepositoryPort;
import levelup42.novapay_backend_hex.domain.port.out.HashServicePort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Servicio de aplicación que orquesta la preparación de la evidencia fiscal.
 * El encadenamiento de hashes es un requisito legal (TicketBAI / VERIFACTU),
 * por tanto esta lógica pertenece a la capa de aplicación, no a infraestructura.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FiscalEvidenceService {

    private final FiscalRecordRepositoryPort fiscalRecordRepository;
    private final HashServicePort hashService;
    private final ApiClientRepositoryPort apiClientRepository;

    /**
     * Calcula el hash encadenado y crea el registro fiscal en estado PENDIENTE_ENVIO.
     * Debe ejecutarse justo después de persistir la factura y antes del envío a Hacienda.
     */
    public FiscalRecord prepareFiscalEvidence(Invoice invoice) {
        log.debug("Preparando evidencia fiscal para factura: {}", invoice.getId());

        java.util.List<FiscalRecord> latestRecords = fiscalRecordRepository.findLatestByCompanyId(invoice.getCompany().getId(), 20);

        // Obtener el hash del último registro fiscal ya persistido de la empresa.
        // Esto evita que la "última factura" sea la factura recién creada (sin fiscal_record aún),
        // lo que provocaba enviar PrimerRegistro=S indebidamente y errores AEAT 2007.
        String previousHash = latestRecords.stream()
            .filter(r -> r.getCurrentHash() != null && !r.getCurrentHash().isBlank())
            .findFirst()
            .map(FiscalRecord::getCurrentHash)
                .orElse(null);

        if (previousHash == null || previousHash.isBlank()) {
            previousHash = resolvePreviousHashByTaxId(invoice);
        }

        if (previousHash == null || previousHash.isBlank()) {
            previousHash = resolveChainSeed(invoice);
        }

        if ((previousHash == null || previousHash.isBlank()) && hasAnyPreviousRecord(latestRecords)) {
            throw new InvalidInvoiceStateException(
                    "No se puede firmar la factura: la cadena fiscal previa está vacía y existen registros anteriores pendientes o inconsistentes"
            );
        }

        log.info("Cadena fiscal previa para companyId={}: {}",
            invoice.getCompany().getId(),
            previousHash != null ? "ENCONTRADA" : "NO ENCONTRADA (PrimerRegistro)");

        String currentHash = hashService.calculateChainedHash(invoice, previousHash);

        FiscalRecord record = new FiscalRecord(
                null,
                invoice,
                invoice.getCompany().getTaxAgency(),
                previousHash,
                currentHash,
                null,   // sentXml — se llenará al enviar
                null,   // responseXml — se llenará al recibir
                FiscalStatus.PENDIENTE_ENVIO,
                FiscalRecordType.ALTA,
                null,   // sentAt
                0,      // retryCount
                null    // respondedAt
        );

        return fiscalRecordRepository.save(record);
    }

    private boolean hasAnyPreviousRecord(java.util.List<FiscalRecord> latestRecords) {
        return latestRecords != null && !latestRecords.isEmpty();
    }

    private String resolveChainSeed(Invoice invoice) {
        String companySeed = apiClientRepository.findByLinkedCompanyId(invoice.getCompany().getId())
                .map(client -> client.getClientSigningPreviousHash())
                .filter(seed -> seed != null && !seed.isBlank())
                .map(String::trim)
                .orElse(null);

        if (companySeed != null && !companySeed.isBlank()) {
            log.info("Cadena fiscal seed recuperada desde ApiClient.linkedCompanyId para companyId={}",
                    invoice.getCompany().getId());
            return companySeed;
        }

        String envSeed = System.getenv("FISCAL_CHAIN_SEED_HASH");
        if (envSeed == null || envSeed.isBlank()) {
            envSeed = System.getProperty("FISCAL_CHAIN_SEED_HASH");
        }
        if (envSeed != null && !envSeed.isBlank()) {
            log.warn("Usando fallback global FISCAL_CHAIN_SEED_HASH para companyId={}",
                    invoice.getCompany().getId());
            return envSeed.trim();
        }

        return null;
    }

    private String resolvePreviousHashByTaxId(Invoice invoice) {
        String taxId = invoice.getCompany() != null && invoice.getCompany().getTaxId() != null
                ? invoice.getCompany().getTaxId().getValue()
                : null;
        if (taxId == null || taxId.isBlank()) {
            return null;
        }

        return fiscalRecordRepository.findLatestByCompanyTaxId(taxId, 200).stream()
                .filter(r -> r.getCurrentHash() != null && !r.getCurrentHash().isBlank())
                .filter(r -> r.getInvoice() == null || !invoice.getId().equals(r.getInvoice().getId()))
                .map(FiscalRecord::getCurrentHash)
                .findFirst()
                .orElse(null);
    }
}
