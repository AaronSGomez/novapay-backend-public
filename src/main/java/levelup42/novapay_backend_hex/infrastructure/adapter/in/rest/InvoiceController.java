package levelup42.novapay_backend_hex.infrastructure.adapter.in.rest;

import jakarta.validation.Valid;
import levelup42.novapay_backend_hex.domain.port.in.InvoiceCancelUseCase;
import levelup42.novapay_backend_hex.domain.port.in.InvoiceCreateUseCase;
import levelup42.novapay_backend_hex.domain.port.in.InvoiceGetUseCase;
import levelup42.novapay_backend_hex.infrastructure.adapter.in.rest.dto.InvoiceCancelRequestDTO;
import levelup42.novapay_backend_hex.infrastructure.adapter.in.rest.dto.InvoiceRequestDTO;
import levelup42.novapay_backend_hex.infrastructure.adapter.in.rest.dto.InvoiceResponse;
import levelup42.novapay_backend_hex.infrastructure.adapter.in.rest.mapper.WebMapper;
import lombok.RequiredArgsConstructor;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/invoices")
@RequiredArgsConstructor
public class InvoiceController {

    private final InvoiceCreateUseCase createUseCase;
    private final InvoiceGetUseCase getUseCase;
    private final InvoiceCancelUseCase cancelUseCase;
    private final WebMapper mapper;

    @Operation(summary = "Emitir factura", 
               description = "Crea y registra una factura en VERIFACTU",
               security = @SecurityRequirement(name = "Bearer Auth"))
    @ApiResponse(responseCode = "201", description = "Factura creada")
    @ApiResponse(responseCode = "400", description = "Datos inválidos")
    @ApiResponse(responseCode = "401", description = "Token inválido o expirado")
    @PostMapping
    public ResponseEntity<InvoiceResponse> emit(@Valid @RequestBody InvoiceRequestDTO request) {
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(mapper.toResponse(createUseCase.emit(mapper.toCommand(request))));
    }

    @Operation(summary = "Obtener factura", 
               description = "Recupera los datos de una factura existente",
               security = @SecurityRequirement(name = "Bearer Auth"))
    @ApiResponse(responseCode = "200", description = "Factura encontrada")
    @ApiResponse(responseCode = "404", description = "Factura no encontrada")
    @GetMapping("/{id}")
    public ResponseEntity<InvoiceResponse> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(mapper.toResponse(getUseCase.getById(id)));
    }

    @Operation(summary = "Anular factura", 
               description = "Anula una factura existente en VERIFACTU",
               security = @SecurityRequirement(name = "Bearer Auth"))
    @ApiResponse(responseCode = "200", description = "Factura anulada con éxito")
    @ApiResponse(responseCode = "400", description = "Datos inválidos o factura ya anulada")
    @ApiResponse(responseCode = "404", description = "Factura no encontrada")
    @PostMapping("/{id}/cancel")
    public ResponseEntity<InvoiceResponse> cancel(@PathVariable UUID id, @Valid @RequestBody InvoiceCancelRequestDTO request) {
        return ResponseEntity.ok(mapper.toResponse(cancelUseCase.cancel(mapper.toCommand(request))));
    }
}

