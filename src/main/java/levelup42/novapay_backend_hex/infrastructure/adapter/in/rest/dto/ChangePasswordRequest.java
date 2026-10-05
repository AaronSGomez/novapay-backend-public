package levelup42.novapay_backend_hex.infrastructure.adapter.in.rest.dto;

import jakarta.validation.constraints.*;
import lombok.*;

/**
 * DTO para solicitud de cambio de contraseña.
 * Se usa tanto después de recuperación (con temporalPassword)
 * como para usuario autenticado (con currentPassword).
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChangePasswordRequest {

    /**
     * La contraseña temporal recibida por email durante recuperación.
     * Obligatoria si es flujo "forgot password", NULL si es "cambiar contraseña autenticado".
     */
    private String temporalPassword;

    /**
     * La contraseña actual del usuario (para flujo de cambio autenticado).
     * Obligatoria si NO se proporciona temporalPassword.
     */
    private String currentPassword;

    /**
     * La nueva contraseña deseada.
     * Debe cumplir requisitos: 12+ chars, mayús, minús, número, símbolo.
     */
    @NotBlank(message = "La nueva contraseña es obligatoria")
    @Size(min = 12, message = "La contraseña debe tener al menos 12 caracteres")
    @Pattern(
        regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&])[A-Za-z\\d@$!%*?&]+$",
        message = "La contraseña debe contener mayúsculas, minúsculas, números y símbolos (@$!%*?&)"
    )
    private String newPassword;

    /**
     * Validación: exactamente uno de temporalPassword o currentPassword debe estar presente.
     */
    @AssertTrue(message = "Debe proporcionar temporalPassword (recuperación) o currentPassword (autenticado), pero no ambos")
    public boolean isPasswordFlowValid() {
        boolean hasTemporalPassword = temporalPassword != null && !temporalPassword.isBlank();
        boolean hasCurrentPassword = currentPassword != null && !currentPassword.isBlank();
        return hasTemporalPassword ^ hasCurrentPassword; // XOR: exactly one true
    }
}
