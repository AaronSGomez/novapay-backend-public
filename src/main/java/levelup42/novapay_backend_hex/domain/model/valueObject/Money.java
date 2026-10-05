package levelup42.novapay_backend_hex.domain.model.valueObject;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;


/**
 * Value Object que representa una cantidad monetaria.
 * La persistencia en BD la gestiona MoneyConverter (autoApply = true),
 * que mapea Money ↔ BigDecimal en una sola columna NUMERIC(19,6).
 */
public final class Money {

    /** * Escala interna (6 decimales).
     * Aunque el cliente pague 10,50€, internamente calculamos con 10,500000
     * para que al multiplicar por el IVA (21%) el resultado sea ultra-preciso.
     */
    private static final int INTERNAL_SCALE = 6;

    /** Escala fiscal (2 decimales). Lo que se imprime en el papel oficial. */
    private static final int FISCAL_SCALE = 2;

    /** Redondeo HALF_UP: Si el tercer decimal es 5 o más, redondea hacia arriba. */
    private static final RoundingMode ROUNDING_MODE = RoundingMode.HALF_UP;

    /** Constante para valor cero (útil para inicializar totales) */
    public static final Money ZERO = new Money(BigDecimal.ZERO);

    /** Constante para valor mínimo legal (1 céntimo) */
    public static final Money MINIMUM = new Money(new BigDecimal("0.01"));

    /** * El importe real. Es 'final' porque el dinero, una vez creado el objeto,
     * no se puede "cambiar", solo se puede operar para crear un objeto nuevo.
     */
    private final BigDecimal amount;

    /**
     * Constructor para JPA.
     * Las herramientas de base de datos necesitan este constructor "invisible"
     * para poder recrear el objeto cuando leen de la tabla.
     */
    protected Money() {
        this.amount = BigDecimal.ZERO;
    }

    /**
     * Constructor privado de seguridad.
     * Obligamos a que la escala siempre sea de 6 decimales desde el nacimiento del objeto.
     */
    private Money(BigDecimal amount) {
        this.amount = amount.setScale(INTERNAL_SCALE, ROUNDING_MODE);
    }

    /**
     * Método de Fábrica (Static Factory). Es la única forma de crear dinero.
     * Actúa como filtro: no deja pasar nulos ni números negativos si no es legal.
     * * @param amount El importe (ej: 15.50)
     * @return Un objeto Money blindado.
     */
    public static Money of(BigDecimal amount) {
        Objects.requireNonNull(amount, "El importe no puede ser nulo");
        // En facturas de venta normales, el importe no debería ser negativo.
        // Si necesitas abonos, se gestionan mediante Facturas Rectificativas.
        if (amount.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("El importe no puede ser negativo: " + amount);
        }
        return new Money(amount);
    }

    /**
     * Crea dinero a partir de un texto.
     * Es la forma más segura de recibir datos del frontend (JSON).
     */
    public static Money of(String amount) {
        Objects.requireNonNull(amount, "El importe no puede ser nulo");
        return of(new BigDecimal(amount));
    }

    /**
     * Devuelve el importe redondeado para Hacienda (2 decimales).
     * @return El número que irá en el XML de TicketBAI o VERIFACTU.
     */
    public BigDecimal toFiscalScale() {
        return amount.setScale(FISCAL_SCALE, ROUNDING_MODE);
    }

    /**
     * Devuelve el valor con toda su precisión interna.
     */
    public BigDecimal getValue() {
        return amount;
    }

    /**
     * Suma de dinero.
     * Crea un objeto Money NUEVO. No modifica el actual (Inmutabilidad).
     */
    public Money add(Money other) {
        Objects.requireNonNull(other, "No puedes sumar un valor nulo");
        return new Money(this.amount.add(other.amount));
    }

    /**
     * Resta de dinero.
     * Bloquea la operación si el resultado daría negativo para evitar errores de caja.
     */
    public Money subtract(Money other) {
        Objects.requireNonNull(other, "No puedes restar un valor nulo");
        BigDecimal result = this.amount.subtract(other.amount);
        if (result.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("La resta no puede dar un resultado negativo");
        }
        return new Money(result);
    }

    /**
     * Multiplicación (ej: multiplicar precio por cantidad o por porcentaje de IVA).
     */
    public Money multiply(BigDecimal factor) {
        Objects.requireNonNull(factor, "El factor de multiplicación no puede ser nulo");
        return new Money(this.amount.multiply(factor));
    }

    /**
     * Multiplica por un porcentaje entero (ej: 21 para el 21% de IVA).
     * Explicación: Divide el porcentaje por 100 y luego multiplica.
     */
    public Money multiplyPercentage(int percentage) {
        BigDecimal factor = BigDecimal.valueOf(percentage)
                .divide(BigDecimal.valueOf(100), INTERNAL_SCALE, ROUNDING_MODE);
        return multiply(factor);
    }

    /**
     * División de dinero.
     * Útil para desglosar importes a partir de un total con IVA incluido.
     */
    public Money divide(BigDecimal divisor) {
        Objects.requireNonNull(divisor, "El divisor no puede ser nulo");
        if (divisor.compareTo(BigDecimal.ZERO) == 0) {
            throw new ArithmeticException("No puedes dividir dinero por cero");
        }
        return new Money(this.amount.divide(divisor, INTERNAL_SCALE, ROUNDING_MODE));
    }

    /**
     * Compara si es mayor que otro importe.
     */
    public boolean isGreaterThan(Money other) {
        return this.amount.compareTo(other.amount) > 0;
    }

    /**
     * Verifica si el importe es exactamente 0.000000.
     */
    public boolean isZero() {
        return amount.compareTo(BigDecimal.ZERO) == 0;
    }

    /**
     * Formato bonito para humanos.
     * @return "100,50 €"
     */
    public String formatWithEuro() {
        return String.format("%.2f €", toFiscalScale());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Money money)) return false;
        // Comparamos el valor numérico ignorando si uno tiene más ceros finales que otro.
        return this.amount.compareTo(money.amount) == 0;
    }

    @Override
    public int hashCode() {
        // Normalizamos el hash para que 10.5 y 10.50 den el mismo código.
        return Objects.hash(amount.stripTrailingZeros());
    }

    @Override
    public String toString() {
        return toFiscalScale().toPlainString();
    }
}