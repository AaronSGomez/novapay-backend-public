package levelup42.novapay_backend_hex.domain.model.valueObject;

import java.util.Objects;
import java.util.regex.Pattern;

/**
 * Value Object que representa un Identificador Fiscal Español (NIF, CIF o NIE).
 * En el mundo real, es el "DNI de la empresa o persona" que emite o recibe la factura.
 * Hacienda (AEAT / TicketBAI) exige que este dato sea estrictamente válido.
 * Al ser inmutable y estar autovalidado, aseguramos que en toda la aplicación
 * no exista jamás un TaxId con formato incorrecto.
 */
public final class TaxId {

    // =========================================================================
    // Expresiones Regulares (RegEx) - Las "plantillas" de validación
    // =========================================================================

    /**
     * Plantilla para validar un NIF (DNI español de toda la vida).
     * Explicación para "tontos": ^ (empieza por) \d{8} (exactamente 8 números) [A-Z] (una letra mayúscula) $ (y termina ahí).
     */
    private static final Pattern NIF = Pattern.compile("^\\d{8}[A-Z]$");

    /**
     * Plantilla para validar un CIF (El identificador de las empresas).
     * Explicación: Empieza por una letra específica de empresa (A-W), seguida de 7 números, y termina en número o letra de la A a la J.
     */
    private static final Pattern CIF = Pattern.compile("^[ABCDEFGHJKLMNPQRSUVW]\\d{7}[A-J0-9]$");

    /**
     * Plantilla para validar un NIE (El identificador de extranjeros).
     * Explicación: Empieza por X, Y o Z, sigue con 7 números, y termina con una letra mayúscula.
     */
    private static final Pattern NIE = Pattern.compile("^[XYZ]\\d{7}[A-Z]$");

    // =========================================================================

    /**
     * El valor real del documento de identidad (ej. "B12345678").
     * Es final para que nadie pueda cambiarlo una vez creado.
     */
    private final String value;

    /**
     * Constructor privado.
     * Lo hacemos privado para evitar que un programador haga un `new TaxId("HOLA")`
     * saltándose todas las comprobaciones de seguridad.
     */
    private TaxId(String value) {
        this.value = value;
    }

    /**
     * Método de fábrica estático (Static Factory).
     * Es la única puerta de entrada para crear un TaxId. Actúa como filtro de seguridad.
     * * @param value El identificador fiscal en texto (ej. un DNI que metió el usuario en el frontend).
     * @return Una instancia validada de TaxId.
     * @throws IllegalArgumentException si el formato no cuadra con un DNI, NIE o CIF español.
     */
    public static TaxId of(String value) {
        // 1. Validamos que no nos pasen un nulo. Si es nulo, la app rompe aquí mismo (Falla rápido).
        Objects.requireNonNull(value, "El NIF no puede ser nulo");

        // 2. Limpieza de datos: Quitamos los espacios en blanco por los lados (trim)
        // y lo pasamos todo a mayúsculas para evitar problemas tontos de formato.
        String v = value.trim().toUpperCase();

        // 3. Comprobación cruzada: Comprobamos si el texto encaja en ALGUNA de las tres plantillas (NIF, CIF o NIE).
        // Si no encaja en ninguna (las tres dicen 'false'), lanzamos un error que bloqueará la creación de la factura.
        if (!NIF.matcher(v).matches() && !CIF.matcher(v).matches() && !NIE.matcher(v).matches()) {
            throw new IllegalArgumentException("NIF/CIF/NIE inválido: " + value);
        }

        // 4. Si ha superado todas las barreras, creamos el objeto seguro y lo devolvemos.
        return new TaxId(v);
    }

    /**
     * Devuelve el texto limpio del identificador para guardarlo en la base de datos
     * o meterlo en el XML de TicketBAI.
     */
    public String getValue() {
        return value;
    }

    /**
     * Sobrescribimos el método equals.
     * Sirve para que Java entienda que dos TaxId son el mismo cliente si su texto es "12345678Z",
     * aunque estén guardados en sitios diferentes de la memoria.
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof TaxId t)) return false;
        return Objects.equals(value, t.value);
    }

    /**
     * Genera un código interno (hash) basado en el texto del DNI/CIF.
     * Es obligatorio sobrescribirlo si sobrescribimos el `equals`, para que funcione
     * bien en las listas o colecciones (como Set o Map).
     */
    @Override
    public int hashCode() {
        return Objects.hash(value);
    }

    /**
     * Cuando hagamos un System.out.println() o intentemos concatenarlo,
     * devolverá directamente el valor del DNI para que sea fácil de leer en los logs.
     */
    @Override
    public String toString() {
        return value;
    }
}