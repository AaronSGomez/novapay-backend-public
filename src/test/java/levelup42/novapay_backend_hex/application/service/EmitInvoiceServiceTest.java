package levelup42.novapay_backend_hex.application.service;

import levelup42.novapay_backend_hex.domain.model.Company;
import levelup42.novapay_backend_hex.domain.model.FiscalRecord;
import levelup42.novapay_backend_hex.domain.model.Invoice;
import levelup42.novapay_backend_hex.domain.model.PosTerminal;
import levelup42.novapay_backend_hex.domain.model.enums.InvoiceType;
import levelup42.novapay_backend_hex.domain.model.enums.TaxAgency;
import levelup42.novapay_backend_hex.domain.exception.InvalidInvoiceStateException;
import levelup42.novapay_backend_hex.domain.port.in.InvoiceCreateCommand;
import levelup42.novapay_backend_hex.domain.port.in.InvoiceResult;
import levelup42.novapay_backend_hex.domain.port.out.CompanyRepositoryPort;
import levelup42.novapay_backend_hex.domain.port.out.FiscalAgencyPort;
import levelup42.novapay_backend_hex.domain.port.out.InvoiceRepositoryPort;
import levelup42.novapay_backend_hex.domain.exception.CompanyNotFoundException;
import levelup42.novapay_backend_hex.domain.port.out.PosTerminalRepositoryPort;
import levelup42.novapay_backend_hex.domain.port.out.FiscalRecordRepositoryPort;
import levelup42.novapay_backend_hex.domain.port.out.HashServicePort;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EmitInvoiceServiceTest {

    @Mock CompanyRepositoryPort companyRepositoryPort;
    @Mock InvoiceRepositoryPort invoiceRepositoryPort;
    @Mock PosTerminalRepositoryPort posTerminalRepositoryPort;
    @Mock FiscalRecordRepositoryPort fiscalRecordRepositoryPort;
    @Mock HashServicePort hashServicePort;
    @Mock FiscalAgencyPort fiscalAgencyPort;
    @Mock TaxCalculationService taxCalculationService;
    @Mock FiscalEvidenceService fiscalEvidenceService;

    @InjectMocks EmitInvoiceService emitInvoiceService;

    @Test
    void emitInvoice_companyNoExiste_lanzaCompanyNotFoundException() {
        UUID companyId = UUID.randomUUID();
        InvoiceCreateCommand command = mock(InvoiceCreateCommand.class);
        when(command.companyId()).thenReturn(companyId);

        when(companyRepositoryPort.findById(companyId)).thenReturn(Optional.empty());

        assertThrows(CompanyNotFoundException.class, () -> emitInvoiceService.emit(command));
    }

    @Test
    void emitInvoice_rectificativaSinRectifiedInvoiceId_lanzaInvalidInvoiceStateException() {
        UUID companyId = UUID.randomUUID();
        UUID terminalId = UUID.randomUUID();

        InvoiceCreateCommand command = mock(InvoiceCreateCommand.class);
        when(command.companyId()).thenReturn(companyId);
        when(command.terminalId()).thenReturn(terminalId);
        when(command.type()).thenReturn(InvoiceType.RECTIFICATIVA);
        when(command.rectifiedInvoiceId()).thenReturn(null);

        when(companyRepositoryPort.findById(companyId)).thenReturn(Optional.of(mock(Company.class)));
        when(posTerminalRepositoryPort.findById(terminalId)).thenReturn(Optional.of(mock(PosTerminal.class)));

        assertThrows(InvalidInvoiceStateException.class, () -> emitInvoiceService.emit(command));
    }

    @Test
    void emitInvoice_noRectificativaConRectifiedInvoiceId_lanzaInvalidInvoiceStateException() {
        UUID companyId = UUID.randomUUID();
        UUID terminalId = UUID.randomUUID();

        InvoiceCreateCommand command = mock(InvoiceCreateCommand.class);
        when(command.companyId()).thenReturn(companyId);
        when(command.terminalId()).thenReturn(terminalId);
        when(command.type()).thenReturn(InvoiceType.SIMPLIFICADA);
        when(command.rectifiedInvoiceId()).thenReturn(UUID.randomUUID());

        when(companyRepositoryPort.findById(companyId)).thenReturn(Optional.of(mock(Company.class)));
        when(posTerminalRepositoryPort.findById(terminalId)).thenReturn(Optional.of(mock(PosTerminal.class)));

        assertThrows(InvalidInvoiceStateException.class, () -> emitInvoiceService.emit(command));
    }
}
