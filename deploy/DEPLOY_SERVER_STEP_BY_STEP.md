# Guía de despliegue del backend NovaPay (servidor)

Esta guía despliega:
- PostgreSQL (privado para el servidor)
- Backend Spring Boot (privado dentro de la red Docker)
- Proxy inverso Nginx (vinculado solo a localhost en el servidor)
- CrowdSec (protección básica a nivel de contenedor)

## 1) Lo que ya está preparado en el repositorio

- Compose para servidor: deploy/docker-compose.server.yml
- Configuración de Nginx: deploy/nginx.conf
- Plantilla de variables de entorno: deploy/.env.server.example
- Archivo de construcción de la imagen Docker: Dockerfile

## 2) Requisitos previos del servidor

En un servidor Ubuntu/Debian:

```bash
sudo apt-get update
sudo apt-get install -y ca-certificates curl gnupg

# Docker
sudo install -m 0755 -d /etc/apt/keyrings
curl -fsSL https://download.docker.com/linux/ubuntu/gpg | sudo gpg --dearmor -o /etc/apt/keyrings/docker.gpg
echo \
  "deb [arch=$(dpkg --print-architecture) signed-by=/etc/apt/keyrings/docker.gpg] https://download.docker.com/linux/ubuntu \
  $(. /etc/os-release && echo $VERSION_CODENAME) stable" | sudo tee /etc/apt/sources.list.d/docker.list > /dev/null
sudo apt-get update
sudo apt-get install -y docker-ce docker-ce-cli containerd.io docker-buildx-plugin docker-compose-plugin

sudo systemctl enable docker
sudo systemctl start docker
```

## 3) Copiar el proyecto backend al servidor

Destino recomendado:

```bash
sudo mkdir -p /opt/novapay
sudo chown -R $USER:$USER /opt/novapay
```

Copiar desde la máquina local (ejemplo):

```bash
rsync -avz --delete /local/path/novapay_backend_hex/ user@server:/opt/novapay/novapay_backend_hex/
```

## 4) Preparar secretos seguros en el servidor

Nunca subas secretos a git.

### 4.1 Crear el archivo de entorno de ejecución

```bash
cd /opt/novapay/novapay_backend_hex/deploy
cp .env.server.example .env.server
chmod 600 .env.server
```

Editar valores:
- DB_PASSWORD
- JWT_SECRET
- CERT_ALIAS
- CERT_PASSWORD
- VERIFACTU_*
- opcional FISCAL_CHAIN_SEED_HASH

### 4.2 Preparar el certificado como base64 en variables de entorno

Genera el base64 en una máquina de confianza y pégalo directamente en `CERT_P12_BASE64` dentro de `.env.server`:

```bash
base64 -w0 certificado.p12 > cert_p12_base64.txt
```

Después copia el valor completo al servidor dentro de `.env.server` y protege el archivo:

```bash
scp .env.server user@server:/opt/novapay/novapay_backend_hex/deploy/.env.server
ssh user@server "chmod 600 /opt/novapay/novapay_backend_hex/deploy/.env.server"
```

El backend lee directamente estas variables del entorno:
- CERT_P12_BASE64
- CERT_PASSWORD
- CERT_ALIAS

## 5) Construir y levantar la pila

```bash
cd /opt/novapay/novapay_backend_hex/deploy
docker compose -f docker-compose.server.yml --env-file .env.server up -d --build
```

Comprobar estado:

```bash
docker compose -f docker-compose.server.yml ps
docker compose -f docker-compose.server.yml logs -f backend
```

## 6) Validar el backend y el comportamiento de la cadena

Desde el servidor:

```bash
# Nginx (solo localhost)
curl -i http://127.0.0.1:8081/
```

Desde tu portátil mediante túnel SSH:

```bash
ssh -L 8081:127.0.0.1:8081 user@server
```

Después llama a la API contra:
- http://127.0.0.1:8081

Secuencia de validación recomendada:
1. Emitir la primera factura para la empresa/NIF
2. Emitir la segunda factura
3. Verificar que la segunda usa el hash de la primera
4. Forzar un caso pendiente/error y reintentar
5. Confirmar que el backend bloquea la firma si falta el hash anterior pero existe historial

## 7) Conexión del frontend cuando el backend esté online

Configura la URL base de la API del frontend con la URL del servidor que expondrás públicamente más adelante (o con la URL del túnel mientras pruebas).

Si el frontend se ejecuta en el navegador y el backend está en un origen distinto, añade la configuración CORS en el backend antes de exponerlo públicamente en producción.

## 8) Endurecimiento opcional para producción (siguiente paso)

- Sustituir Nginx solo-localhost por un endpoint público HTTPS (certificado TLS)
- Añadir reglas de firewall (permitir solo 22/443)
- Mover los secretos a Docker secrets o a un gestor de secretos externo
- Añadir una política de copias de seguridad para el volumen postgres_data
- Añadir monitorización y alertas

## 9) Operaciones comunes

Actualizar el código del backend:

```bash
cd /opt/novapay/novapay_backend_hex
# hacer pull o rsync del código nuevo
cd deploy
docker compose -f docker-compose.server.yml --env-file .env.server up -d --build
```

Detener la pila:

```bash
docker compose -f docker-compose.server.yml down
```

Detener y eliminar volúmenes (peligro: borra la base de datos):

```bash
docker compose -f docker-compose.server.yml down -v
```
