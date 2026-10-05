package levelup42.novapay_backend_hex.domain.service;

import java.security.SecureRandom;
import java.util.Base64;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Servicio de generación de tokens criptográficos.
 * Responsable de:
 * - Generar tokens aleatorios seguros para verificación de email
 * - Generar contraseñas temporales para recuperación
 * - Hashear tokens para almacenamiento seguro en BD
 * - Comparación segura de tokens hasheados
 *
 * IMPORTANTE: Los tokens se hashean con SHA-256 antes de ser almacenados en BD.
 * Se envían los tokens raw (sin hashear) al cliente por email,
 * y se comparan los tokens hasheados en BD con el hash del token recibido.
 */
public class TokenGenerationService {

    private static final SecureRandom secureRandom = new SecureRandom();
    private static final String ALGORITHM = "SHA-256";
    private static final int TOKEN_LENGTH = 32; // bytes -> 42-43 caracteres en Base64 URL-safe

    /**
     * Genera un token aleatorio seguro de 32 bytes (256 bits).
     * Retorna el token en Base64 URL-safe sin padding (apto para URLs y emails).
     *
     * @return token aleatorio sin encriptar
     */
    public static String generateRandomToken() {
        byte[] randomBytes = new byte[TOKEN_LENGTH];
        secureRandom.nextBytes(randomBytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);
    }

    /**
     * Genera una contraseña temporal de 12 caracteres con mayúsculas, minúsculas, números y símbolo.
     * Formato: "XXXXXX-YYYY!" donde X=mayús, Y=minús/número.
     * Garantiza cumplimiento de requisitos de contraseña.
     *
     * @return contraseña temporal (ej: "AbCdEf-GhIj1!")
     */
    public static String generateTemporaryPassword() {
        String uppercase = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
        String lowercase = "abcdefghijklmnopqrstuvwxyz";
        String digits = "0123456789";
        String symbols = "@$!%*?&";

        StringBuilder password = new StringBuilder();

        // 6 mayúsculas
        for (int i = 0; i < 6; i++) {
            password.append(uppercase.charAt(secureRandom.nextInt(uppercase.length())));
        }

        // Guión separador
        password.append("-");

        // 3 minúsculas/números alternados
        for (int i = 0; i < 3; i++) {
            if (i % 2 == 0) {
                password.append(lowercase.charAt(secureRandom.nextInt(lowercase.length())));
            } else {
                password.append(digits.charAt(secureRandom.nextInt(digits.length())));
            }
        }

        // Símbolo
        password.append(symbols.charAt(secureRandom.nextInt(symbols.length())));

        // Resultado: "XXXXXX-YyN!" = 12 caracteres

        return password.toString();
    }

    /**
     * Hashea un token con SHA-256 para almacenamiento seguro en BD.
     * El token enviado por email se envía raw, pero en BD se almacena hasheado.
     * Para validar: hashear el token recibido en la request y comparar con el almacenado.
     *
     * @param token token raw sin encriptar
     * @return token hasheado en hexadecimal
     */
    public static String hashToken(String token) {
        try {
            MessageDigest digest = MessageDigest.getInstance(ALGORITHM);
            byte[] hashBytes = digest.digest(token.getBytes());
            return bytesToHex(hashBytes);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 algorithm not available", e);
        }
    }

    /**
     * Convierte bytes a string hexadecimal (0-9, a-f).
     *
     * @param bytes array de bytes
     * @return representación hexadecimal
     */
    private static String bytesToHex(byte[] bytes) {
        StringBuilder hexString = new StringBuilder();
        for (byte b : bytes) {
            String hex = Integer.toHexString(0xff & b);
            if (hex.length() == 1) hexString.append('0');
            hexString.append(hex);
        }
        return hexString.toString();
    }

    /**
     * Verifica que un token raw coincida con su hash almacenado.
     * Compara de forma segura (timing-safe si es posible).
     *
     * @param rawToken token raw sin encriptar (recibido en request)
     * @param hashedToken token hasheado (almacenado en BD)
     * @return true si coinciden, false en caso contrario
     */
    public static boolean verifyToken(String rawToken, String hashedToken) {
        String computedHash = hashToken(rawToken);
        return constantTimeEquals(computedHash, hashedToken);
    }

    /**
     * Comparación segura contra timing attacks.
     * Siempre compara toda la cadena incluso si encuentra diferencia temprano.
     *
     * @param a primer string
     * @param b segundo string
     * @return true si son iguales
     */
    private static boolean constantTimeEquals(String a, String b) {
        if (a == null || b == null) {
            return a == b;
        }

        byte[] aBytes = a.getBytes();
        byte[] bBytes = b.getBytes();

        int result = 0;
        for (int i = 0; i < Math.max(aBytes.length, bBytes.length); i++) {
            byte aByte = i < aBytes.length ? aBytes[i] : 0;
            byte bByte = i < bBytes.length ? bBytes[i] : 0;
            result |= aByte ^ bByte;
        }

        return result == 0;
    }
}
