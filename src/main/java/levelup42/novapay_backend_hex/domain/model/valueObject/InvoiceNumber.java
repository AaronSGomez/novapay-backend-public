package levelup42.novapay_backend_hex.domain.model.valueObject;

import java.util.Objects;

public final class InvoiceNumber {
    private final String series;
    private final int number;

    private InvoiceNumber(String series, int number) {
        this.series = series;
        this.number = number;
    }

    // getter
    public String getSeries() {return series;}
    public int getNumber() {return number;}

    // metodos
    public static InvoiceNumber of(String series, int number) {
        // 1. Fallo rápido: Si la serie es nula, explota aquí mismo.
        Objects.requireNonNull(series, "La serie no puede ser nula");

        // 2. Regla de negocio: La serie tiene que tener texto real, no valen espacios en blanco.
        if (series.isBlank()) {
            throw new IllegalArgumentException("La serie no puede estar vacía");
        }

        // 3. Regla fiscal: No existen facturas con número 0 o negativos.
        if (number <= 0) {
            throw new IllegalArgumentException("El número debe ser > 0");
        }

        // 4. Limpieza: Quitamos espacios extra y lo ponemos en mayúsculas por convención fiscal.
        return new InvoiceNumber(series.toUpperCase().trim(), number);
    }

    public String formatted() {
        // "%s": Pone el texto de la serie tal cual.
        // "-": Pone un guion literal.
        // "%05d": Pone el número asegurando que tenga al menos 5 cifras, rellenando con 0s si hace falta.
        return String.format("%s-%05d", series, number);
    }

    /**
     * Sobrescribimos equals() porque es un Value Object.
     * Dos números de factura son "iguales" si tienen la misma serie y el mismo número,
     * aunque sean dos objetos diferentes en la memoria RAM.
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof InvoiceNumber n)) return false;
        return number == n.number && Objects.equals(series, n.series);
    }

    /**
     * Sobrescribimos hashCode() de la mano de equals() para que funcione bien
     * si metemos este objeto en un HashSet o como clave de un HashMap.
     */
    @Override
    public int hashCode() {
        return Objects.hash(series, number);
    }

    /**
     * Cuando hagamos un System.out.println() o lo pasemos a String,
     * devolverá el formato limpio y bonito directamente.
     */
    @Override
    public String toString() {
        return formatted();
    }

}
