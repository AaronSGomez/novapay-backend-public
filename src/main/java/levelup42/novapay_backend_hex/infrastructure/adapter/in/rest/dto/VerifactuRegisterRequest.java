package levelup42.novapay_backend_hex.infrastructure.adapter.in.rest.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

/**
 * Request de registro Verifactu desde frontend TPV.
 * Crea empresa y cliente API asociado.
 */
@Getter
@Setter
public class VerifactuRegisterRequest {

    @NotBlank(message = "companyName es obligatorio")
    private String companyName;

    @NotBlank(message = "taxId es obligatorio")
    private String taxId;

    @NotBlank(message = "address es obligatorio")
    private String address;

    private String clientHash;

    @NotBlank(message = "email es obligatorio")
    @Email(message = "email debe tener formato válido")
    private String email;

    @NotBlank(message = "password es obligatorio")
    private String password;

    @NotBlank(message = "passwordConfirmation es obligatorio")
    private String passwordConfirmation;

    @NotBlank(message = "planCode es obligatorio")
    private String planCode;

    @NotBlank(message = "billingCycle es obligatorio")
    private String billingCycle;

    @JsonProperty("isNewSystem")
    private boolean newSystem;
}
