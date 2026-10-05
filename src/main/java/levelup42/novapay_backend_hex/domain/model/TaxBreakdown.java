package levelup42.novapay_backend_hex.domain.model;

import levelup42.novapay_backend_hex.domain.model.enums.TaxType;
import levelup42.novapay_backend_hex.domain.model.valueObject.Money;

import java.math.BigDecimal;
import java.util.UUID;

public class TaxBreakdown {
    private UUID id;
    private Invoice invoice;
    private TaxType taxType;
    private BigDecimal percentage;
    private Money baseAmount;
    private Money taxAmount;

    // constructor
    public TaxBreakdown(UUID id, Invoice invoice, TaxType taxType, BigDecimal percentage, Money baseAmount, Money taxAmount) {
        this.id = id;
        this.invoice = invoice;
        this.taxType = taxType;
        this.percentage = percentage;
        this.baseAmount = baseAmount;
        this.taxAmount = taxAmount;
    }

    // getter
    public UUID getId() {return id;}
    public Invoice getInvoice() {return invoice;}
    public TaxType getTaxType() {return taxType;}
    public BigDecimal getPercentage() {return percentage;}
    public Money getBaseAmount() {return baseAmount;}
    public Money getTaxAmount() {return taxAmount;}

    // setter
    public void setId(UUID id) {this.id = id;}
    public void setInvoice(Invoice invoice) {this.invoice = invoice;}
    public void setTaxType(TaxType taxType) {this.taxType = taxType;}
    public void setPercentage(BigDecimal percentage) {this.percentage = percentage;}
    public void setBaseAmount(Money baseAmount) {this.baseAmount = baseAmount;}
    public void setTaxAmount(Money taxAmount) {this.taxAmount = taxAmount;}
}
