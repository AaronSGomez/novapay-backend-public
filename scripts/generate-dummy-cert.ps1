# Generador de Certificado Dummy PKCS#12 para Pruebas Locales (PowerShell)
# Genera un keystore .p12 autofirmado para pruebas de firma XML / Verifactu

Param(
    [string]$KeystorePath = "src/test/resources/test-keystore.p12",
    [string]$Password = "changeit",
    [string]$Alias = "test-alias"
)

Write-Host "Generando certificado autofirmado dummy PKCS#12..." -ForegroundColor Green

keytool -genkeypair `
    -alias $Alias `
    -keyalg RSA `
    -keysize 2048 `
    -storetype PKCS12 `
    -keystore $KeystorePath `
    -storepass $Password `
    -keypass $Password `
    -dname "CN=Novapay Test, OU=Dev, O=Novapay Public Demo, L=Madrid, ST=Madrid, C=ES" `
    -validity 3650

if ($LASTEXITCODE -eq 0) {
    Write-Host "✅ Certificado dummy generado con éxito en: $KeystorePath" -ForegroundColor Green
    Write-Host "   Alias: $Alias"
    Write-Host "   Password: $Password"
} else {
    Write-Host "❌ Error al generar el certificado con keytool." -ForegroundColor Red
}
