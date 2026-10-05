package levelup42.novapay_backend_hex.domain.model;

import levelup42.novapay_backend_hex.domain.model.enums.TaxAgency;
import levelup42.novapay_backend_hex.domain.model.valueObject.TaxId;

import java.util.UUID;

public class Company {
     private UUID id;
     private String name;
     private TaxId taxId; // ObjectValue
     private String address;
     private TaxAgency taxAgency;


     // constructor
     public Company(UUID id, String name, TaxId taxId, String address, TaxAgency taxAgency) {
          this.id = id;
          this.name = name;
          this.taxId = taxId;
          this.address = address;
          this.taxAgency = taxAgency;
     }

     // getter
     public UUID getId() {return id;}
     public String getName() {return name;}
     public TaxId getTaxId() {return taxId;}
     public String getAddress() {return address;}
     public TaxAgency getTaxAgency() {return taxAgency;}

}
