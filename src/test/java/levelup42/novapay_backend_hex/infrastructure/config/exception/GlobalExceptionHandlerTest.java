package levelup42.novapay_backend_hex.infrastructure.config.exception;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;

import levelup42.novapay_backend_hex.domain.exception.CompanyNotFoundException;
import levelup42.novapay_backend_hex.domain.exception.InvoiceNotFoundException;
import levelup42.novapay_backend_hex.domain.exception.InvalidInvoiceStateException;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void handleInvoiceNotFound_devuelve404() {
        InvoiceNotFoundException ex = new InvoiceNotFoundException(UUID.randomUUID());
        ResponseEntity<ProblemDetail> res = handler.handle(ex);
        assertEquals(HttpStatus.NOT_FOUND, res.getStatusCode());
        assertNotNull(res.getBody().getDetail());
    }

    @Test
    void handleCompanyNotFound_devuelve404() {
        CompanyNotFoundException ex = new CompanyNotFoundException(UUID.randomUUID());
        ResponseEntity<ProblemDetail> res = handler.handle(ex);
        assertEquals(HttpStatus.NOT_FOUND, res.getStatusCode());
    }

    @Test
    void handleInvalidInvoiceState_devuelve409() {
        InvalidInvoiceStateException ex = new InvalidInvoiceStateException("Invalid state");
        ResponseEntity<ProblemDetail> res = handler.handle(ex);
        assertEquals(HttpStatus.CONFLICT, res.getStatusCode());
    }

    @Test
    void handleValidationErrors_devuelve400ConDetalles() {
        MethodArgumentNotValidException ex = mock(MethodArgumentNotValidException.class);
        BindingResult bindingResult = mock(BindingResult.class);
        
        FieldError error = new FieldError("objectName", "campo", "Mensaje de error");
        when(ex.getBindingResult()).thenReturn(bindingResult);
        when(bindingResult.getFieldErrors()).thenReturn(List.of(error));

        ResponseEntity<ProblemDetail> res = handler.handle(ex);
        assertEquals(HttpStatus.BAD_REQUEST, res.getStatusCode());
    }
}
