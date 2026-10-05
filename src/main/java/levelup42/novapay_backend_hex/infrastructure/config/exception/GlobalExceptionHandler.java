package levelup42.novapay_backend_hex.infrastructure.config.exception;

import levelup42.novapay_backend_hex.domain.exception.*;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.net.URI;
import java.time.Instant;
import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {

    // --- 404 — Recursos no encontrados ---

    @ExceptionHandler(InvoiceNotFoundException.class)
    public ResponseEntity<ProblemDetail> handle(InvoiceNotFoundException ex) {
        return buildResponse(HttpStatus.NOT_FOUND, "invoice-not-found", ex.getMessage());
    }

    @ExceptionHandler(CompanyNotFoundException.class)
    public ResponseEntity<ProblemDetail> handle(CompanyNotFoundException ex) {
        return buildResponse(HttpStatus.NOT_FOUND, "company-not-found", ex.getMessage());
    }

    @ExceptionHandler(PosTerminalNotFoundException.class)
    public ResponseEntity<ProblemDetail> handle(PosTerminalNotFoundException ex) {
        return buildResponse(HttpStatus.NOT_FOUND, "pos-terminal-not-found", ex.getMessage());
    }

    @ExceptionHandler(FiscalRecordNotFoundException.class)
    public ResponseEntity<ProblemDetail> handle(FiscalRecordNotFoundException ex) {
        return buildResponse(HttpStatus.NOT_FOUND, "fiscal-record-not-found", ex.getMessage());
    }

    // --- 409 — Estado inválido ---

    @ExceptionHandler(InvalidInvoiceStateException.class)
    public ResponseEntity<ProblemDetail> handle(InvalidInvoiceStateException ex) {
        return buildResponse(HttpStatus.CONFLICT, "invalid-invoice-state", ex.getMessage());
    }

    // --- 502 — Error de envío fiscal (agencia externa) ---

    @ExceptionHandler(FiscalSubmissionException.class)
    public ResponseEntity<ProblemDetail> handle(FiscalSubmissionException ex) {
        return buildResponse(HttpStatus.BAD_GATEWAY, "fiscal-submission-error", ex.getMessage());
    }

    // --- 400 — Validación de request ---

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ProblemDetail> handle(MethodArgumentNotValidException ex) {
        String detail = ex.getBindingResult().getFieldErrors().stream()
                .map(fe -> fe.getField() + ": " + fe.getDefaultMessage())
                .collect(Collectors.joining(", "));
        return buildResponse(HttpStatus.BAD_REQUEST, "validation-error", detail);
    }

    // --- 500 — Error interno genérico ---

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ProblemDetail> handle(Exception ex) {
        return buildResponse(HttpStatus.INTERNAL_SERVER_ERROR, "internal-error",
                "Error interno del servidor: " + ex.getMessage());
    }

    // --- Helper ---

    private ResponseEntity<ProblemDetail> buildResponse(HttpStatus status, String type, String detail) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detail);
        problem.setType(URI.create("https://novapay.levelup42.com/errors/" + type));
        problem.setProperty("timestamp", Instant.now().toString());
        return ResponseEntity.status(status).body(problem);
    }
}

