package levelup42.novapay_backend_hex.infrastructure.adapter.in.rest.dto;

import jakarta.validation.constraints.*;
import lombok.*;

/**
 * DTO para solicitud de login (autenticación).
 * Valida email y contraseña ingresadas por el usuario.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LoginRequest {

    @NotBlank(message = "El email es obligatorio")
    @Email(message = "El email debe ser válido")
    private String email;

    @NotBlank(message = "La contraseña es obligatoria")
    private String password;
}
