package levelup42.novapay_backend_hex.infrastructure.adapter.in.rest.dto;

import levelup42.novapay_backend_hex.domain.model.enums.InvoiceStatus;
import levelup42.novapay_backend_hex.domain.model.enums.InvoiceType;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/**
 * DTO de respuesta HTTP para facturas.
 * Desacopla el contrato de la API REST del modelo interno de dominio.
 */
public record InvoiceResponse(
        UUID id,
        String series,
        Integer number,
        InvoiceType type,
        InvoiceStatus status,
        LocalDate issueDate,
        BigDecimal totalAmount
) {}
