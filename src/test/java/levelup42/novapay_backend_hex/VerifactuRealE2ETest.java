package levelup42.novapay_backend_hex;

import levelup42.novapay_backend_hex.domain.model.enums.InvoiceType;
import levelup42.novapay_backend_hex.domain.model.enums.TaxType;
import levelup42.novapay_backend_hex.domain.port.in.InvoiceCreateCommand;
import levelup42.novapay_backend_hex.domain.port.in.InvoiceLineCommand;
import levelup42.novapay_backend_hex.domain.port.in.InvoiceResult;
import levelup42.novapay_backend_hex.domain.port.in.InvoiceCreateUseCase;
import levelup42.novapay_backend_hex.domain.model.valueObject.Money;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.annotation.Rollback;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@SpringBootTest
public class VerifactuRealE2ETest {

    @Autowired
    private InvoiceCreateUseCase emitInvoiceService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    @Rollback(false)
    public void executeRealAeatsubmission() {
        UUID companyId;
        String companyTaxId = "12345678Z";
        String companyName = "EMPRESA DE PRUEBAS S.L.";
        String companyAddress = "Calle Mayor 123, Madrid";
        String companyTaxAgency = "AEAT";

        try {
            companyId = jdbcTemplate.queryForObject("SELECT id FROM companies WHERE tax_id = ?", UUID.class, companyTaxId);
            jdbcTemplate.update("UPDATE companies SET name = ?, address = ?, tax_agency = ? WHERE id = ?",
                    companyName, companyAddress, companyTaxAgency, companyId);
            System.out.println("EMPRESA ENCONTRADA Y ACTUALIZADA EN BD: " + companyId);
        } catch (org.springframework.dao.EmptyResultDataAccessException e) {
            companyId = UUID.randomUUID();
            jdbcTemplate.update("INSERT INTO companies (id, name, tax_id, address, tax_agency) VALUES (?, ?, ?, ?, ?)",
                    companyId, companyName, companyTaxId, companyAddress, companyTaxAgency);
            System.out.println("EMPRESA CREADA EN BD: " + companyId);
        }

        UUID terminalId;
        try {
            terminalId = jdbcTemplate.queryForObject("SELECT id FROM pos_terminals WHERE company_id = ? LIMIT 1", UUID.class, companyId);
            System.out.println("TPV ENCONTRADO EN BD: " + terminalId);
        } catch (org.springframework.dao.EmptyResultDataAccessException e) {
            terminalId = UUID.randomUUID();
            jdbcTemplate.update("INSERT INTO pos_terminals (id, name, serial_number, company_id, active) VALUES (?, ?, ?, ?, ?)",
                    terminalId, "Caja Principal", "TPV-TEST-001", companyId, true);
            System.out.println("TPV CREADO EN BD: " + terminalId);
        }

        // Simulamos el comando que llega desde la aplicación cliente
        InvoiceCreateCommand command = new InvoiceCreateCommand(
                "TEST",
                (int) (Math.random() * 10000),
                InvoiceType.SIMPLIFICADA,
                companyId,
                terminalId,
                LocalDate.now(),
                List.of(
                        new InvoiceLineCommand("Café de prueba", new BigDecimal("1"), Money.of("1.50"), TaxType.IVA_REDUCIDO),
                        new InvoiceLineCommand("Tostada", new BigDecimal("1"), Money.of("2.50"), TaxType.IVA_REDUCIDO)
                ),
                null
        );

        System.out.println("==========================================");
        System.out.println("EMITIENDO FACTURA AL ENDPOINT DE VERIFACTU...");
        System.out.println("==========================================");

        try {
            emitInvoiceService.emit(command);

            System.out.println("==========================================");
            System.out.println("PRUEBA REALIZADA. RECUPERANDO DATOS DE LA BD...");
            System.out.println("==========================================");

            List<Map<String, Object>> records = jdbcTemplate.queryForList("SELECT * FROM fiscal_records");
            if (!records.isEmpty()) {
                Map<String, Object> record = records.get(records.size() - 1); // Recuperar el último registro
                System.out.println("--- RESULTADO DEL ENVÍO ---");
                System.out.println("ID Registro Fiscal: " + record.get("id"));
                System.out.println("Factura ID: " + record.get("invoice_id"));
                System.out.println("Status: " + record.get("status"));
                System.out.println("Verification URL (Para QR): " + record.get("verification_url"));
                System.out.println("Hash de encadenamiento: " + record.get("payload_hash"));
                System.out.println("Agency Invoice ID (CSV): " + record.get("agency_invoice_id"));
                System.out.println("--- REQUEST BODY (XML ENVIADO) ---");
                System.out.println(record.get("sent_xml"));
                System.out.println("--- RESPONSE BODY (XML RECIBIDO) ---");
                System.out.println(record.get("response_xml"));
            } else {
                System.out.println("ALERTA: No se encontró ningún registro fiscal guardado en la BD.");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        System.out.println("==========================================");
        System.out.println("PRUEBA REALIZADA. REVISA LA TABLA fiscal_records");
        System.out.println("==========================================");
    }
}
