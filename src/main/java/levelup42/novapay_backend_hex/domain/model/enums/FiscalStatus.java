package levelup42.novapay_backend_hex.domain.model.enums;

/**
 * Representa el estado de la comunicación con la Agencia Tributaria.
 */
public enum FiscalStatus {
    PENDIENTE_ENVIO,    // Aún no hemos intentado enviarla.
    ENVIANDO,           // Estamos conectando con el servidor de la AEAT/TicketBAI.
    ACEPTADO,           // Hacienda dice que todo OK (Generamos QR).
    RECHAZADO,          // Hacienda dice que hay un error en los datos.
    REINTENTO,          // Hubo un fallo técnico (internet) y probaremos luego.
    ERROR_PERMANENTE,   // No se puede enviar tras muchos intentos.
    ANULADO             // Se ha enviado una anulación y ha sido aceptada.
}
