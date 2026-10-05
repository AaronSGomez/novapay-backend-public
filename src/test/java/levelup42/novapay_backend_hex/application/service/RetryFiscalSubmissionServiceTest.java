package levelup42.novapay_backend_hex.application.service;

import levelup42.novapay_backend_hex.domain.model.FiscalRecord;
import levelup42.novapay_backend_hex.domain.model.Company;
import levelup42.novapay_backend_hex.domain.model.enums.FiscalStatus;
import levelup42.novapay_backend_hex.domain.model.Invoice;
import levelup42.novapay_backend_hex.domain.port.out.FiscalAgencyPort;
import levelup42.novapay_backend_hex.domain.port.out.FiscalRecordRepositoryPort;
import levelup42.novapay_backend_hex.domain.port.out.InvoiceRepositoryPort;
import levelup42.novapay_backend_hex.domain.exception.FiscalRecordNotFoundException;
import levelup42.novapay_backend_hex.domain.exception.InvoiceNotFoundException;
import levelup42.novapay_backend_hex.domain.exception.InvalidInvoiceStateException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RetryFiscalSubmissionServiceTest {

    @Mock InvoiceRepositoryPort invoiceRepository;
    @Mock FiscalRecordRepositoryPort fiscalRecordRepository;
    @Mock FiscalAgencyPort fiscalAgencyPort;
    @InjectMocks RetryFiscalSubmissionService service;

    @Test
    void retry_facturaYaAceptada_noReintenta() throws Exception {
        UUID id = UUID.randomUUID();
        Invoice invoice = mock(Invoice.class);
        FiscalRecord record = mock(FiscalRecord.class);
        when(record.getStatus()).thenReturn(FiscalStatus.ACEPTADO);

        when(invoice.getId()).thenReturn(id);

        when(invoiceRepository.findById(id)).thenReturn(Optional.of(invoice));
        when(fiscalRecordRepository.findByInvoiceId(id)).thenReturn(Optional.of(record));

        service.retry(id);

        verify(fiscalAgencyPort, never()).submit(any());
    }

    @Test
    void retry_facturaNoExiste_lanzaInvoiceNotFoundException() throws Exception {
        UUID id = UUID.randomUUID();
        when(invoiceRepository.findById(id)).thenReturn(Optional.empty());
        assertThrows(InvoiceNotFoundException.class, () -> service.retry(id));
    }

    @Test
    void retry_registroFiscalNoExiste_lanzaFiscalRecordNotFoundException() throws Exception {
        UUID id = UUID.randomUUID();
        Invoice invoice = mock(Invoice.class);
        when(invoice.getId()).thenReturn(id);
        when(invoiceRepository.findById(id)).thenReturn(Optional.of(invoice));
        when(fiscalRecordRepository.findByInvoiceId(any())).thenReturn(Optional.empty());
        assertThrows(FiscalRecordNotFoundException.class, () -> service.retry(id));
    }

    @Test
    void retry_conCadenaVaciaYHistorialPrevio_lanzaInvalidInvoiceStateException() throws Exception {
        UUID id = UUID.randomUUID();
        UUID companyId = UUID.randomUUID();

        Invoice invoice = mock(Invoice.class);
        Company company = mock(Company.class);
        when(invoice.getId()).thenReturn(id);
        when(invoice.getCompany()).thenReturn(company);
        when(company.getId()).thenReturn(companyId);

        FiscalRecord record = mock(FiscalRecord.class);
        when(record.getStatus()).thenReturn(FiscalStatus.RECHAZADO);
        when(record.getPreviousHash()).thenReturn(" ");

        Invoice anotherInvoice = mock(Invoice.class);
        when(anotherInvoice.getId()).thenReturn(UUID.randomUUID());
        FiscalRecord previousCompanyRecord = mock(FiscalRecord.class);
        when(previousCompanyRecord.getInvoice()).thenReturn(anotherInvoice);

        when(invoiceRepository.findById(id)).thenReturn(Optional.of(invoice));
        when(fiscalRecordRepository.findByInvoiceId(id)).thenReturn(Optional.of(record));
        when(fiscalRecordRepository.findLatestByCompanyId(companyId, 200)).thenReturn(List.of(previousCompanyRecord));

        assertThrows(InvalidInvoiceStateException.class, () -> service.retry(id));

        verify(fiscalAgencyPort, never()).submit(any());
        verify(fiscalRecordRepository, never()).save(any());
    }

    @Test
    void retry_primerRegistroSinHistorialPermiteReintento() throws Exception {
        UUID id = UUID.randomUUID();
        UUID companyId = UUID.randomUUID();

        Invoice invoice = mock(Invoice.class);
        Company company = mock(Company.class);
        when(invoice.getId()).thenReturn(id);
        when(invoice.getCompany()).thenReturn(company);
        when(company.getId()).thenReturn(companyId);

        FiscalRecord record = mock(FiscalRecord.class);
        when(record.getStatus()).thenReturn(FiscalStatus.RECHAZADO);
        when(record.getPreviousHash()).thenReturn(" ");
        when(record.getRetryCount()).thenReturn(0);

        when(invoiceRepository.findById(id)).thenReturn(Optional.of(invoice));
    when(fiscalRecordRepository.findByInvoiceId(id)).thenReturn(Optional.of(record), Optional.of(record));
        when(fiscalRecordRepository.findLatestByCompanyId(companyId, 200)).thenReturn(List.of());
        when(fiscalRecordRepository.save(record)).thenReturn(record);
    when(record.getStatus()).thenReturn(FiscalStatus.RECHAZADO, FiscalStatus.ACEPTADO);

        service.retry(id);

        verify(record).setRetryCount(1);
        verify(record).setStatus(FiscalStatus.REINTENTO);
        verify(fiscalAgencyPort, times(1)).submit(record);
    }

    @Test
    void retry_cuandoAeatRechaza3000_lanzaInvalidInvoiceStateException() throws Exception {
        UUID id = UUID.randomUUID();
        UUID companyId = UUID.randomUUID();

        Invoice invoice = mock(Invoice.class);
        Company company = mock(Company.class);
        when(invoice.getId()).thenReturn(id);
        when(invoice.getCompany()).thenReturn(company);
        when(company.getId()).thenReturn(companyId);
        when(invoice.getLines()).thenReturn(List.of());

        FiscalRecord record = mock(FiscalRecord.class);
        when(record.getStatus()).thenReturn(FiscalStatus.RECHAZADO, FiscalStatus.RECHAZADO);
        when(record.getPreviousHash()).thenReturn(" ");
        when(record.getRetryCount()).thenReturn(0);
        when(record.getResponseXml()).thenReturn("<CodigoErrorRegistro>3000</CodigoErrorRegistro><DescripcionErrorRegistro>Registro de facturación duplicado.</DescripcionErrorRegistro>");

        when(invoiceRepository.findById(id)).thenReturn(Optional.of(invoice));
        when(fiscalRecordRepository.findByInvoiceId(id)).thenReturn(Optional.of(record), Optional.of(record));
        when(fiscalRecordRepository.findLatestByCompanyId(companyId, 200)).thenReturn(List.of());
        when(fiscalRecordRepository.save(record)).thenReturn(record);

        InvalidInvoiceStateException ex = assertThrows(InvalidInvoiceStateException.class, () -> service.retry(id));
        assertTrue(ex.getMessage().contains("[3000]"));
        verify(fiscalAgencyPort, times(1)).submit(record);
    }
}
