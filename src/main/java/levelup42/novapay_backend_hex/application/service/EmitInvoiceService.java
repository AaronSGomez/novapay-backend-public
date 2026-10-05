package levelup42.novapay_backend_hex.application.service;

import levelup42.novapay_backend_hex.domain.exception.CompanyNotFoundException;
import levelup42.novapay_backend_hex.domain.exception.InvalidInvoiceStateException;
import levelup42.novapay_backend_hex.domain.exception.InvoiceNotFoundException;
import levelup42.novapay_backend_hex.domain.exception.PosTerminalNotFoundException;
import levelup42.novapay_backend_hex.domain.model.*;
import levelup42.novapay_backend_hex.domain.model.enums.FiscalStatus;
import levelup42.novapay_backend_hex.domain.model.enums.InvoiceStatus;
import levelup42.novapay_backend_hex.domain.model.enums.InvoiceType;
import levelup42.novapay_backend_hex.domain.model.valueObject.InvoiceNumber;
import levelup42.novapay_backend_hex.domain.model.valueObject.Money;
import levelup42.novapay_backend_hex.domain.port.in.InvoiceCreateCommand;
import levelup42.novapay_backend_hex.domain.port.in.InvoiceCreateUseCase;
import levelup42.novapay_backend_hex.domain.port.in.InvoiceResult;
import levelup42.novapay_backend_hex.domain.port.out.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmitInvoiceService implements InvoiceCreateUseCase {

    private final InvoiceRepositoryPort invoiceRepository;
    private final CompanyRepositoryPort companyRepository;
    private final PosTerminalRepositoryPort posTerminalRepository;
    private final FiscalRecordRepositoryPort fiscalRecordRepository;
    private final HashServicePort hashService;
    private final FiscalAgencyPort fiscalAgencyPort;
    private final TaxCalculationService taxCalculator;
    private final FiscalEvidenceService fiscalEvidenceService;

    @Override
    @Transactional
    public InvoiceResult emit(InvoiceCreateCommand command) {
        log.info("Emitiendo factura: {}-{}", command.series(), command.number());

        Company company = companyRepository.findById(command.companyId())
                .orElseThrow(() -> new CompanyNotFoundException(command.companyId()));

        PosTerminal terminal = posTerminalRepository.findById(command.terminalId())
                .orElseThrow(() -> new PosTerminalNotFoundException(command.terminalId()));

        Invoice rectifiedInvoice = resolveRectifiedInvoice(command);

        List<InvoiceLine> lines = command.lines().stream()
                .map(lineCmd -> {
                    Money taxBase = lineCmd.unitPrice().multiply(lineCmd.quantity());
                    BigDecimal rateFactor = lineCmd.taxType().getRate().divide(new BigDecimal("100"), 6, java.math.RoundingMode.HALF_UP);
                    Money taxAmount = taxBase.multiply(rateFactor);
                    Money totalAmount = taxBase.add(taxAmount);
                    return new InvoiceLine(
                        null, null,
                        lineCmd.description(),
                        lineCmd.quantity(),
                        lineCmd.unitPrice(),
                        lineCmd.taxType(),
                        totalAmount, taxBase, taxAmount
                    );
                })
                .collect(Collectors.toList());

        Invoice invoice = new Invoice(
                null,
                InvoiceNumber.of(command.series(), command.number()),
                command.type(),
                InvoiceStatus.EMITIDA,
                company,
                terminal,
                command.issueDate(),
                null, null, null, rectifiedInvoice,
                lines, null
        );

        lines.forEach(line -> line.setInvoice(invoice));

        invoice.setBreakdowns(taxCalculator.calculateTaxBreakdown(lines, invoice));
        invoice.setBaseAmount(taxCalculator.calculateTotalTaxBase(lines));
        invoice.setTaxAmount(taxCalculator.calculateTotalVat(lines));
        invoice.setTotalAmount(taxCalculator.calculateInvoiceTotal(lines));

        Invoice savedInvoice = invoiceRepository.save(invoice);

        // Preparar evidencia fiscal (hash encadenado) — lógica de negocio legal
        FiscalRecord record = fiscalEvidenceService.prepareFiscalEvidence(savedInvoice);

        // Envío a la agencia fiscal
        try {
            fiscalAgencyPort.submit(record);
        } catch (Exception e) {
            log.error("Error en envío fiscal para factura {}: {}", savedInvoice.getId(), e.getMessage());
            // Persistimos error definitivo para que el frontend deje de consultar estado pendiente.
            record.setStatus(FiscalStatus.ERROR_PERMANENTE);
            record.setRespondedAt(OffsetDateTime.now());
            record.setResponseXml("SIGN_OR_SUBMIT_ERROR: " + e.getMessage());
            fiscalRecordRepository.save(record);
        }

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

    private Invoice resolveRectifiedInvoice(InvoiceCreateCommand command) {
        boolean isRectificative = command.type() == InvoiceType.RECTIFICATIVA
                || command.type() == InvoiceType.RECTIFICATIVA_SIMPLIFICADA;

        if (command.rectifiedInvoiceId() == null) {
            if (isRectificative) {
                throw new InvalidInvoiceStateException(
                        "Las facturas rectificativas deben informar rectifiedInvoiceId"
                );
            }
            return null;
        }

        if (!isRectificative) {
            throw new InvalidInvoiceStateException(
                    "rectifiedInvoiceId solo se permite para facturas de tipo RECTIFICATIVA o RECTIFICATIVA_SIMPLIFICADA"
            );
        }

        return invoiceRepository.findById(command.rectifiedInvoiceId())
                .orElseThrow(() -> new InvoiceNotFoundException(command.rectifiedInvoiceId()));
    }
}
