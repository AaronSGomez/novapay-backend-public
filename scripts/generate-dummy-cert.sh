#!/usr/bin/env bash
# Generador de Certificado Dummy PKCS#12 para Pruebas Locales (Bash)

KEYSTORE_PATH="src/test/resources/test-keystore.p12"
PASSWORD="changeit"
ALIAS="test-alias"

echo "Generando certificado autofirmado dummy PKCS#12..."

keytool -genkeypair \
    -alias "$ALIAS" \
    -keyalg RSA \
    -keysize 2048 \
    -storetype PKCS12 \
    -keystore "$KEYSTORE_PATH" \
    -storepass "$PASSWORD" \
    -keypass "$PASSWORD" \
    -dname "CN=Novapay Test, OU=Dev, O=Novapay Public Demo, L=Madrid, ST=Madrid, C=ES" \
    -validity 3650

if [ $? -eq 0 ]; then
    echo "✅ Certificado dummy generado con éxito en: $KEYSTORE_PATH"
    echo "   Alias: $ALIAS"
    echo "   Password: $PASSWORD"
else
    echo "❌ Error al generar el certificado con keytool."
fi
