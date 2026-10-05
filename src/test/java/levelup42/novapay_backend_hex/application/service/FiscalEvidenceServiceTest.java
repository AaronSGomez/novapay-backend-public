package levelup42.novapay_backend_hex.application.service;

import levelup42.novapay_backend_hex.domain.model.Company;
import levelup42.novapay_backend_hex.domain.model.FiscalRecord;
import levelup42.novapay_backend_hex.domain.exception.InvalidInvoiceStateException;
import levelup42.novapay_backend_hex.domain.model.enums.FiscalStatus;
import levelup42.novapay_backend_hex.domain.model.enums.FiscalRecordType;
import levelup42.novapay_backend_hex.domain.model.enums.TaxAgency;
import levelup42.novapay_backend_hex.domain.model.Invoice;
import levelup42.novapay_backend_hex.domain.port.out.ApiClientRepositoryPort;
import levelup42.novapay_backend_hex.domain.port.out.FiscalRecordRepositoryPort;
import levelup42.novapay_backend_hex.infrastructure.adapter.out.fiscal.HashServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FiscalEvidenceServiceTest {

    @Mock FiscalRecordRepositoryPort fiscalRecordRepository;
    @Mock ApiClientRepositoryPort apiClientRepository;
    @Mock HashServiceImpl hashService;
    @InjectMocks FiscalEvidenceService service;

    @Test
    void prepareFiscalEvidence_guardaAltaConHash() {
        Invoice invoice = mock(Invoice.class);
        Company company = mock(Company.class);
        UUID companyId = UUID.randomUUID();
        when(invoice.getCompany()).thenReturn(company);
        when(company.getId()).thenReturn(companyId);
        when(company.getTaxAgency()).thenReturn(TaxAgency.AEAT);

        when(fiscalRecordRepository.findLatestByCompanyId(companyId, 20)).thenReturn(List.of());
        when(apiClientRepository.findByLinkedCompanyId(companyId)).thenReturn(Optional.empty());
        when(hashService.calculateChainedHash(any(), eq(null))).thenReturn("HASH_NUEVO");

        when(fiscalRecordRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        FiscalRecord record = service.prepareFiscalEvidence(invoice);

        assertEquals("HASH_NUEVO", record.getCurrentHash());
        assertNull(record.getPreviousHash());
        assertEquals(FiscalRecordType.ALTA, record.getType());
        assertEquals(FiscalStatus.PENDIENTE_ENVIO, record.getStatus());
        verify(fiscalRecordRepository, times(1)).save(record);
    }

    @Test
    void prepareFiscalEvidence_conFacturaAnterior_guardaConPreviousHash() {
        Invoice invoice = mock(Invoice.class);
        Company company = mock(Company.class);
        UUID companyId = UUID.randomUUID();
        when(invoice.getCompany()).thenReturn(company);
        when(company.getId()).thenReturn(companyId);
        when(company.getTaxAgency()).thenReturn(TaxAgency.AEAT);

        FiscalRecord previous = mock(FiscalRecord.class);
        when(previous.getCurrentHash()).thenReturn("HASH_VIEJO");
        when(fiscalRecordRepository.findLatestByCompanyId(companyId, 20)).thenReturn(List.of(previous));

        when(hashService.calculateChainedHash(any(), eq("HASH_VIEJO"))).thenReturn("HASH_NUEVO");
        when(fiscalRecordRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        FiscalRecord record = service.prepareFiscalEvidence(invoice);

        assertEquals("HASH_NUEVO", record.getCurrentHash());
        assertEquals("HASH_VIEJO", record.getPreviousHash());
        verify(fiscalRecordRepository, times(1)).save(record);
        verify(apiClientRepository, never()).findByLinkedCompanyId(any());
    }

    @Test
    void prepareFiscalEvidence_conHistorialPeroSinHashPrevio_lanzaInvalidInvoiceStateException() {
        Invoice invoice = mock(Invoice.class);
        Company company = mock(Company.class);
        UUID companyId = UUID.randomUUID();

        when(invoice.getCompany()).thenReturn(company);
        when(company.getId()).thenReturn(companyId);

        FiscalRecord brokenRecord = mock(FiscalRecord.class);
        when(brokenRecord.getCurrentHash()).thenReturn(null);

        when(fiscalRecordRepository.findLatestByCompanyId(companyId, 20)).thenReturn(List.of(brokenRecord));
        when(apiClientRepository.findByLinkedCompanyId(companyId)).thenReturn(Optional.empty());

        assertThrows(InvalidInvoiceStateException.class, () -> service.prepareFiscalEvidence(invoice));

        verify(hashService, never()).calculateChainedHash(any(), any());
        verify(fiscalRecordRepository, never()).save(any());
    }
}
