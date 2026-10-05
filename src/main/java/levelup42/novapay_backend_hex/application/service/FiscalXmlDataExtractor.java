package levelup42.novapay_backend_hex.application.service;

import org.springframework.stereotype.Component;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilderFactory;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;

@Component
public class FiscalXmlDataExtractor {

    public record ExtractedFiscalData(String csv, String responseCode, String responseDescription) {}

    public ExtractedFiscalData extract(String responseXml) {
        if (responseXml == null || responseXml.isBlank()) {
            return new ExtractedFiscalData(null, null, null);
        }

        try {
            Document doc = parseXml(responseXml);

            NodeList responseList = doc.getElementsByTagNameNS("*", "RespuestaRegFactuSistemaFacturacion");
            if (responseList.getLength() == 0) {
                return new ExtractedFiscalData(null, null, null);
            }

            Element response = (Element) responseList.item(0);
            String csv = extractTextAnyNS(response, "CSV");

            String responseCode = null;
            String responseDescription = null;

            NodeList lines = response.getElementsByTagNameNS("*", "RespuestaLinea");
            if (lines.getLength() > 0) {
                Element line = (Element) lines.item(0);
                responseCode = extractTextAnyNS(line, "CodigoErrorRegistro");
                responseDescription = extractTextAnyNS(line, "DescripcionErrorRegistro");
            }

            return new ExtractedFiscalData(csv, responseCode, responseDescription);
        } catch (Exception ignored) {
            return new ExtractedFiscalData(null, null, null);
        }
    }

    private Document parseXml(String xml) throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);
        factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        return factory.newDocumentBuilder().parse(new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)));
    }

    private String extractTextAnyNS(Element parent, String localName) {
        NodeList nodes = parent.getElementsByTagNameNS("*", localName);
        if (nodes.getLength() == 0) {
            nodes = parent.getElementsByTagName(localName);
        }
        if (nodes.getLength() == 0) {
            return null;
        }
        String text = nodes.item(0).getTextContent();
        return (text == null || text.isBlank()) ? null : text.trim();
    }
}