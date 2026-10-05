package levelup42.novapay_backend_hex.domain.model;

import levelup42.novapay_backend_hex.domain.model.enums.FiscalRecordType;
import levelup42.novapay_backend_hex.domain.model.enums.FiscalStatus;
import levelup42.novapay_backend_hex.domain.model.enums.TaxAgency;

import java.time.OffsetDateTime;
import java.util.UUID;

public class FiscalRecord {

    private UUID id;
    private Invoice invoice;
    private TaxAgency taxAgency;
    private String previousHash;
    private String currentHash;
    private String sentXml;
    private String responseXml;
    private FiscalStatus status;
    private FiscalRecordType type;
    private int retryCount = 0;
    private OffsetDateTime sentAt;
    private OffsetDateTime respondedAt;

    // constructor
    public FiscalRecord(UUID id, Invoice invoice, TaxAgency taxAgency, String previousHash, String currentHash,
                        String sentXml, String responseXml, FiscalStatus status, FiscalRecordType type,
                        OffsetDateTime sentAt, int retryCount, OffsetDateTime respondedAt) {
        this.id = id;
        this.invoice = invoice;
        this.taxAgency = taxAgency;
        this.previousHash = previousHash;
        this.currentHash = currentHash;
        this.sentXml = sentXml;
        this.responseXml = responseXml;
        this.status = status;
        this.type = type;
        this.sentAt = sentAt;
        this.retryCount = retryCount;
        this.respondedAt = respondedAt;
    }

    // getter
    public UUID getId() {return id;}
    public Invoice getInvoice() {return invoice;}
    public TaxAgency getTaxAgency() {return taxAgency;}
    public String getPreviousHash() {return previousHash;}
    public String getCurrentHash() {return currentHash;}
    public String getSentXml() {return sentXml;}
    public String getResponseXml() {return responseXml;}
    public FiscalStatus getStatus() {return status;}
    public FiscalRecordType getType() {return type;}
    public int getRetryCount() {return retryCount;}
    public OffsetDateTime getSentAt() {return sentAt;}
    public OffsetDateTime getRespondedAt() {return respondedAt;}

    // setter
    public void setId(UUID id) {this.id = id;}
    public void setStatus(FiscalStatus status) {this.status = status;}
    public void setRetryCount(int retryCount) {this.retryCount = retryCount;}
    public void setSentAt(OffsetDateTime sentAt) {this.sentAt = sentAt;}
    public void setRespondedAt(OffsetDateTime respondedAt) {this.respondedAt = respondedAt;}
    public void setSentXml(String sentXml) {this.sentXml = sentXml;}
    public void setResponseXml(String responseXml) {this.responseXml = responseXml;}
    public void setInvoice(Invoice invoice) {this.invoice = invoice;}
    public void setPreviousHash(String previousHash) {this.previousHash = previousHash;}
    public void setCurrentHash(String currentHash) {this.currentHash = currentHash;}
}
