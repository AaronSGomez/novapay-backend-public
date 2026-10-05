package levelup42.novapay_backend_hex.domain.model;

import levelup42.novapay_backend_hex.domain.model.enums.TaxType;
import levelup42.novapay_backend_hex.domain.model.valueObject.Money;

import java.math.BigDecimal;
import java.util.UUID;

public class InvoiceLine {
    private UUID id;
    private Invoice invoice;
    private String description;
    private BigDecimal quantity;
    private Money unitPrice;
    private TaxType taxType;
    private Money lineTotal;
    private Money taxBase;
    private Money taxAmount;

    // contructor
    public InvoiceLine(UUID id, Invoice invoice, String description, BigDecimal quantity, Money unitPrice,
                       TaxType taxType, Money lineTotal, Money taxBase, Money taxAmount) {
        this.id = id;
        this.invoice = invoice;
        this.description = description;
        this.quantity = quantity;
        this.unitPrice = unitPrice;
        this.taxType = taxType;
        this.lineTotal = lineTotal;
        this.taxBase = taxBase;
        this.taxAmount = taxAmount;
    }

    // getter
    public UUID getId() {return id;}
    public Invoice getInvoice() {return invoice;}
    public String getDescription() {return description;}
    public BigDecimal getQuantity() {return quantity;}
    public Money getUnitPrice() {return unitPrice;}
    public TaxType getTaxType() {return taxType;}
    public Money getLineTotal() {return lineTotal;}
    public Money getTaxBase() {return taxBase;}
    public Money getTaxAmount() {return taxAmount;}

    // setter
    public void setId(UUID id) {this.id = id;}
    public void setInvoice(Invoice invoice) {this.invoice = invoice;}
    public void setDescription(String description) {this.description = description;}
    public void setQuantity(BigDecimal quantity) {this.quantity = quantity;}
    public void setUnitPrice(Money unitPrice) {this.unitPrice = unitPrice;}
    public void setTaxType(TaxType taxType) {this.taxType = taxType;}
    public void setLineTotal(Money lineTotal) {this.lineTotal = lineTotal;}
    public void setTaxBase(Money taxBase) {this.taxBase = taxBase;}
    public void setTaxAmount(Money taxAmount) {this.taxAmount = taxAmount;}

}
