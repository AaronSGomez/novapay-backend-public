package levelup42.novapay_backend_hex.domain.model.enums;

import java.math.BigDecimal;

public enum TaxType {

    IVA_GENERAL(new BigDecimal("21.00")),
    IVA_REDUCIDO(new BigDecimal("10.00")),
    IVA_SUPERREDUCIDO(new BigDecimal("4.00")),
    IVA_RECARGO_EQUIVALENCIA(new BigDecimal("5.20")),
    EXENTO(BigDecimal.ZERO),
    NO_SUJETO(BigDecimal.ZERO),
    IPSI(new BigDecimal("4.00")),
    IGIC(new BigDecimal("7.00"));

    /**
     * Porcentaje del impuesto (ej. 21.00 para el IVA general).
     * Usado para calcular la cuota: base * rate / 100.
     */
    private final BigDecimal rate;


    TaxType(BigDecimal rate) {
        this.rate = rate;
    }

    public BigDecimal getRate() {
        return rate;
    }
}
