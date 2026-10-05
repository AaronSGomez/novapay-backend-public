package levelup42.novapay_backend_hex.infrastructure.configuration;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Carga explícitamente el archivo .env al iniciar Spring Boot.
 * 
 * Se ejecuta como un ApplicationRunner temprano para asegurar que
 * las variables de entorno (CERT_P12_BASE64, CERT_ALIAS, etc.)
 * estén disponibles incluso si se ejecuta desde diferentes directorios de trabajo.
 */
@Slf4j
@Component
public class DotenvLoader implements ApplicationRunner {

    private static final String DOT_ENV_FILE = ".env";

    @Override
    public void run(org.springframework.boot.ApplicationArguments args) throws Exception {
        log.info("═══════════════════════════════════════════════════════════════════════════════════");
        log.info("=== CARGANDO VARIABLES DE ENTORNO DESDE .env ===");
        log.info("═══════════════════════════════════════════════════════════════════════════════════");

        try {
            // Intenta encontrar el archivo .env en múltiples ubicaciones
            Path[] possiblePaths = {
                Path.of(DOT_ENV_FILE),                                    // directorio actual
                Path.of(".").resolve(DOT_ENV_FILE),                       // relativo
                Path.of("novapay_backend_hex").resolve(DOT_ENV_FILE),     // workspace relativo
                Path.of(System.getProperty("user.dir")).resolve(DOT_ENV_FILE) // user.dir
            };

            Path dotenvPath = null;
            for (Path path : possiblePaths) {
                if (Files.exists(path)) {
                    dotenvPath = path;
                    log.info("✓ ENCONTRADO: {}", path.toAbsolutePath());
                    break;
                }
            }

            if (dotenvPath == null) {
                log.warn("⚠️  ADVERTENCIA: archivo .env no encontrado en las ubicaciones esperadas");
                log.warn("   Esperadas:");
                for (Path path : possiblePaths) {
                    log.warn("     - {}", path.toAbsolutePath());
                }
                log.info("═══════════════════════════════════════════════════════════════════════════════════");
                return;
            }

            Map<String, String> values = loadEnvFile(dotenvPath);

            // Carga las variables en el System para que System.getProperty() las encuentre
            // y para que puedan sobreescribir los valores que ya existan en el entorno del proceso.
            int count = 0;
            for (Map.Entry<String, String> entry : values.entrySet()) {
                String key = entry.getKey();
                String value = entry.getValue();
                if (value != null && !value.isBlank()) {
                    System.setProperty(key, value);
                    count++;
                    
                    // Log selektivo: no loguea valores sensibles completos
                    if (key.contains("PASSWORD") || key.contains("SECRET") || key.contains("BASE64")) {
                        log.debug("  {} = [REDACTED, length={}]", key, value.length());
                    } else {
                        log.debug("  {} = {}", key, value);
                    }
                }
            }

            if (count > 0) {
                log.info("✅ SE HAN CARGADO {} VARIABLES DESDE .env Y ESTABLECIDAS EN System.setProperty()", count);
            } else {
                log.warn("⚠️  El archivo .env está vacío o no contiene variables válidas");
            }

            log.info("═══════════════════════════════════════════════════════════════════════════════════");

        } catch (Exception e) {
            log.error("❌ ERROR AL CARGAR .env: {}", e.getMessage(), e);
            log.error("═══════════════════════════════════════════════════════════════════════════════════");
        }
    }

    private Map<String, String> loadEnvFile(Path dotenvPath) throws Exception {
        Charset charset = StandardCharsets.UTF_16;
        Map<String, String> values = new LinkedHashMap<>();

        try (BufferedReader reader = Files.newBufferedReader(dotenvPath, charset)) {
            String line;
            while ((line = reader.readLine()) != null) {
                String trimmed = line.trim();
                if (trimmed.isEmpty() || trimmed.startsWith("#")) {
                    continue;
                }

                int equalsIndex = trimmed.indexOf('=');
                if (equalsIndex <= 0) {
                    continue;
                }

                String key = trimmed.substring(0, equalsIndex).trim();
                String value = trimmed.substring(equalsIndex + 1).trim();
                if (value.length() >= 2 && ((value.startsWith("\"") && value.endsWith("\"")) || (value.startsWith("'") && value.endsWith("'")))) {
                    value = value.substring(1, value.length() - 1);
                }
                values.put(key, value);
            }
        }

        return values;
    }
}

