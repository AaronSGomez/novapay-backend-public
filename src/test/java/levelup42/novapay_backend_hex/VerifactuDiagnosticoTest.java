package levelup42.novapay_backend_hex;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

@SpringBootTest
public class VerifactuDiagnosticoTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    public void mostrarUltimoXMLenviadoYRecibido() throws IOException {
        List<Map<String, Object>> records = jdbcTemplate.queryForList(
                "SELECT status, sent_xml, response_xml, current_hash FROM fiscal_records WHERE response_xml IS NOT NULL ORDER BY id DESC LIMIT 1"
        );

        if (records.isEmpty()) {
            System.out.println(">>> No se encontraron registros fiscales en la BD.");
            return;
        }

        Map<String, Object> r = records.get(0);
        String status = String.valueOf(r.get("status"));
        String sentXml = String.valueOf(r.get("sent_xml"));
        String responseXml = String.valueOf(r.get("response_xml"));
        String hash = String.valueOf(r.get("current_hash"));

        System.out.println("STATUS: " + status + "  HASH: " + hash);

        Path sentFile = Path.of("target/last_sent.xml");
        Path responseFile = Path.of("target/last_response.xml");
        Files.writeString(sentFile, sentXml != null ? sentXml : "NULL");
        Files.writeString(responseFile, responseXml != null ? responseXml : "NULL");
        System.out.println("XMLs escritos en: " + sentFile + " y " + responseFile);
    }
}
