package levelup42.novapay_backend_hex.infrastructure.adapter.in.rest.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

/**
 * DTO para solicitud de verificación de email.
 * El cliente recibe un token por email y lo envía para activar su cuenta.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VerifyEmailRequest {

    @NotBlank(message = "El token de verificación es obligatorio")
    private String verificationToken;
}
