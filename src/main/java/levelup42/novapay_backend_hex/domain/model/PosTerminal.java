package levelup42.novapay_backend_hex.domain.model;

import java.util.UUID;

public class PosTerminal {
    private UUID id;
    private String serialNumber;
    private Company company;
    private boolean active;

    // constructor

    public PosTerminal(UUID id, String serialNumber, Company company, boolean active) {
        this.id = id;
        this.serialNumber = serialNumber;
        this.company = company;
        this.active = active;
    }

    // getter
    public UUID getId() {return id;}
    public String getSerialNumber() {return serialNumber;}
    public Company getCompany() {return company;}
    public boolean isActive() {return active;}

    // setter
    public void setId(UUID id) {this.id = id;}
    public void setSerialNumber(String serialNumber) {this.serialNumber = serialNumber;}
    public void setCompany(Company company) {this.company = company;}
    public void setActive(boolean active) {this.active = active;}
}
