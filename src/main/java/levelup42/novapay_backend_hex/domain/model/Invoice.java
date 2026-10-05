package levelup42.novapay_backend_hex.domain.model;

import levelup42.novapay_backend_hex.domain.model.enums.InvoiceStatus;
import levelup42.novapay_backend_hex.domain.model.enums.InvoiceType;
import levelup42.novapay_backend_hex.domain.model.valueObject.InvoiceNumber;
import levelup42.novapay_backend_hex.domain.model.valueObject.Money;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class Invoice {
    private UUID id;
    private InvoiceNumber invoiceNumber;
    private InvoiceType type;
    private InvoiceStatus status;
    private Company company;
    private PosTerminal terminal;
    private LocalDate issueDate;
    private Money baseAmount;
    private Money taxAmount;
    private Money totalAmount;
    private Invoice rectifiedInvoice;
    private List<InvoiceLine> lines = new ArrayList<>();
    private List<TaxBreakdown> breakdowns = new ArrayList<>();

    // constructor
    public Invoice(UUID id, InvoiceNumber invoiceNumber, InvoiceType type, InvoiceStatus status, Company company, PosTerminal terminal,
                   LocalDate issueDate, Money baseAmount, Money taxAmount, Money totalAmount, Invoice rectifiedInvoice,
                   List<InvoiceLine> lines, List<TaxBreakdown> breakdowns) {
        this.id = id;
        this.invoiceNumber = invoiceNumber;
        this.type = type;
        this.status = status;
        this.company = company;
        this.terminal = terminal;
        this.issueDate = issueDate;
        this.baseAmount = baseAmount;
        this.taxAmount = taxAmount;
        this.totalAmount = totalAmount;
        this.rectifiedInvoice = rectifiedInvoice;
        this.lines = lines != null ? lines : new ArrayList<>();
        this.breakdowns = breakdowns != null ? breakdowns : new ArrayList<>();
    }

    // getter
    public UUID getId() {return id;}
    public InvoiceNumber getInvoiceNumber() {return invoiceNumber;}
    public InvoiceType getType() {return type;}
    public InvoiceStatus getStatus() {return status;}
    public Company getCompany() {return company;}
    public PosTerminal getTerminal() {return terminal;}
    public LocalDate getIssueDate() {return issueDate;}
    public Money getBaseAmount() {return baseAmount;}
    public Money getTaxAmount() {return taxAmount;}
    public Money getTotalAmount() {return totalAmount;}
    public Invoice getRectifiedInvoice() {return rectifiedInvoice;}
    public List<InvoiceLine> getLines() {return lines;}
    public List<TaxBreakdown> getBreakdowns() {return breakdowns;}

    // setter
    public void setId(UUID id) {this.id = id;}
    public void setInvoiceNumber(InvoiceNumber invoiceNumber) {this.invoiceNumber = invoiceNumber;}
    public void setType(InvoiceType type) {this.type = type;}
    public void setStatus(InvoiceStatus status) {this.status = status;}
    public void setCompany(Company company) {this.company = company;}
    public void setTerminal(PosTerminal terminal) {this.terminal = terminal;}
    public void setIssueDate(LocalDate issueDate) {this.issueDate = issueDate;}
    public void setBaseAmount(Money baseAmount) {this.baseAmount = baseAmount;}
    public void setTaxAmount(Money taxAmount) {this.taxAmount = taxAmount;}
    public void setTotalAmount(Money totalAmount) {this.totalAmount = totalAmount;}
    public void setRectifiedInvoice(Invoice rectifiedInvoice) {this.rectifiedInvoice = rectifiedInvoice;}
    public void setLines(List<InvoiceLine> lines) {this.lines = lines != null ? lines : new ArrayList<>();}
    public void setBreakdowns(List<TaxBreakdown> breakdowns) {this.breakdowns = breakdowns != null ? breakdowns : new ArrayList<>();}
}
