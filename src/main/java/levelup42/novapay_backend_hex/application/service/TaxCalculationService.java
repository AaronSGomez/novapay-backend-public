package levelup42.novapay_backend_hex.application.service;

import levelup42.novapay_backend_hex.domain.model.Invoice;
import levelup42.novapay_backend_hex.domain.model.InvoiceLine;
import levelup42.novapay_backend_hex.domain.model.TaxBreakdown;
import levelup42.novapay_backend_hex.domain.model.enums.TaxType;
import levelup42.novapay_backend_hex.domain.model.valueObject.Money;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class TaxCalculationService {

    public List<TaxBreakdown> calculateTaxBreakdown(List<InvoiceLine> lines, Invoice invoice) {
        Map<TaxType, List<InvoiceLine>> linesByTaxType = lines.stream()
                .collect(Collectors.groupingBy(InvoiceLine::getTaxType));

        return linesByTaxType.entrySet().stream()
                .map(entry -> createTaxBreakdown(entry, invoice))
                .toList();
    }

    private TaxBreakdown createTaxBreakdown(Map.Entry<TaxType, List<InvoiceLine>> entry, Invoice invoice) {
        TaxType taxType = entry.getKey();
        List<InvoiceLine> lines = entry.getValue();

        Money totalBaseAmount = lines.stream()
                .map(InvoiceLine::getTaxBase)
                .reduce(Money.ZERO, Money::add);

        Money totalTaxAmount = lines.stream()
                .map(InvoiceLine::getTaxAmount)
                .reduce(Money.ZERO, Money::add);

        return new TaxBreakdown(
                null, // id to be generated
                invoice,
                taxType,
                taxType.getRate(),
                totalBaseAmount,
                totalTaxAmount
        );
    }

    public Money calculateInvoiceTotal(List<InvoiceLine> lines) {
        return calculateTotalTaxBase(lines).add(calculateTotalVat(lines));
    }

    public Money calculateTotalTaxBase(List<InvoiceLine> lines) {
        return lines.stream()
                .map(InvoiceLine::getTaxBase)
                .reduce(Money.ZERO, Money::add);
    }

    public Money calculateTotalVat(List<InvoiceLine> lines) {
        return lines.stream()
                .map(InvoiceLine::getTaxAmount)
                .reduce(Money.ZERO, Money::add);
    }

    public BigDecimal roundFiscal(BigDecimal amount) {
        return amount.setScale(2, RoundingMode.HALF_UP);
    }
}
