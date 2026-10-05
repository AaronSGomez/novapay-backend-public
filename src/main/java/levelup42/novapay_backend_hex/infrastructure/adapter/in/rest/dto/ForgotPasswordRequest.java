package levelup42.novapay_backend_hex.infrastructure.adapter.in.rest.dto;

import jakarta.validation.constraints.*;
import lombok.*;

/**
 * DTO para solicitud de recuperación de contraseña.
 * El usuario envía su email y recibe una contraseña temporal por email.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ForgotPasswordRequest {

    @NotBlank(message = "El email es obligatorio")
    @Email(message = "El email debe ser válido")
    private String email;
}
