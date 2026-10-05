package levelup42.novapay_backend_hex.infrastructure.adapter.in.rest;

import levelup42.novapay_backend_hex.domain.port.in.FiscalGetStatusUseCase;
import levelup42.novapay_backend_hex.domain.port.in.FiscalInteractionResult;
import levelup42.novapay_backend_hex.domain.port.in.FiscalListInteractionsUseCase;
import levelup42.novapay_backend_hex.domain.port.in.FiscalRetrySubmitUseCase;
import levelup42.novapay_backend_hex.domain.port.in.FiscalStatusResult;
import lombok.RequiredArgsConstructor;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/fiscal")
@RequiredArgsConstructor
public class FiscalController {

    private final FiscalGetStatusUseCase getStatusUseCase;
    private final FiscalRetrySubmitUseCase retryUseCase;
    private final FiscalListInteractionsUseCase listInteractionsUseCase;

    @Operation(summary = "Obtener estado fiscal", 
               description = "Consulta el estado actual de una factura en la agencia tributaria",
               security = @SecurityRequirement(name = "Bearer Auth"))
    @ApiResponse(responseCode = "200", description = "Estado recuperado con éxito")
    @ApiResponse(responseCode = "404", description = "Factura o registro fiscal no encontrado")
    @GetMapping("/status/{invoiceId}")
    public ResponseEntity<FiscalStatusResult> getStatus(@PathVariable UUID invoiceId) {
        return ResponseEntity.ok(getStatusUseCase.getStatus(invoiceId));
    }

    @Operation(summary = "Listar interacciones fiscales",
               description = "Obtiene las ultimas interacciones enviadas a VERIFACTU con su estado actual",
               security = @SecurityRequirement(name = "Bearer Auth"))
    @ApiResponse(responseCode = "200", description = "Interacciones recuperadas con éxito")
    @GetMapping("/interactions")
    public ResponseEntity<List<FiscalInteractionResult>> listInteractions(
            @RequestParam(name = "limit", defaultValue = "100") int limit,
            Authentication authentication) {
        String clientId = authentication != null ? authentication.getName() : null;
        if (clientId == null || clientId.isBlank()) {
            return ResponseEntity.ok(List.of());
        }
        return ResponseEntity.ok(listInteractionsUseCase.listInteractionsForClient(clientId, limit));
    }

    @Operation(summary = "Reintentar envío", 
               description = "Reintenta el envío a VERIFACTU de una factura rechazada o fallida",
               security = @SecurityRequirement(name = "Bearer Auth"))
    @ApiResponse(responseCode = "204", description = "Reintento ejecutado con éxito")
    @ApiResponse(responseCode = "404", description = "Factura o registro fiscal no encontrado")
    @PostMapping("/retry/{invoiceId}")
    public ResponseEntity<Void> retry(@PathVariable UUID invoiceId) {
        retryUseCase.retry(invoiceId);
        return ResponseEntity.noContent().build();
    }
}
