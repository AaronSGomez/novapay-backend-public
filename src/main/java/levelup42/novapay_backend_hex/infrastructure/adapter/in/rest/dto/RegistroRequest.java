package levelup42.novapay_backend_hex.infrastructure.adapter.in.rest.dto;

import jakarta.validation.constraints.*;
import lombok.*;

import java.util.UUID;

/**
 * DTO para solicitud de registro de nuevo cliente.
 * Valida email único, contraseña segura (12+ chars, mayús+minús+número+símbolo),
 * y opcionalmente datos de migración fiscal.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RegistroRequest {

    @NotBlank(message = "El email es obligatorio")
    @Email(message = "El email debe ser válido")
    private String email;

    @NotBlank(message = "La contraseña es obligatoria")
    @Size(min = 12, message = "La contraseña debe tener al menos 12 caracteres")
    @Pattern(
        regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&])[A-Za-z\\d@$!%*?&]+$",
        message = "La contraseña debe contener mayúsculas, minúsculas, números y símbolos (@$!%*?&)"
    )
    private String password;

    // Datos fiscales opcionales para migración
    private UUID companyId;

    private String clientSigningPreviousHash;

    /**
     * Validación adicional: evita que ambos o ninguno de los campos fiscales estén presentes.
     */
    @AssertTrue(message = "Si proporciona datos fiscales, ambos companyId y clientSigningPreviousHash son required")
    public boolean isFiscalDataValid() {
        boolean hasCompanyId = companyId != null;
        boolean hasHash = clientSigningPreviousHash != null && !clientSigningPreviousHash.isBlank();
        return hasCompanyId == hasHash;
    }
}
