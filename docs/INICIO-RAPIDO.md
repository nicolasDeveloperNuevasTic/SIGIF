<!--
  Nombre: INICIO-RAPIDO.md
  Descripción: Comandos iniciales para clonar, levantar y acceder a SIGIF.
  Fecha de creación: 21/10/2026
  Última actualización: 21/10/2026
  Autor: Equipo SIGIF
-->
# Inicio rápido de SIGIF

## 1. Requisitos

- **Git**
- **Docker Desktop** (Windows/Mac) o Docker Engine + Docker Compose (Linux), con el motor encendido.

No se necesita instalar Java, Maven, Node ni PostgreSQL: todo corre en contenedores.

Puertos que deben estar libres: **80, 443, 5432 y 8080**.

## 2. Clonar y levantar

```bash
git clone https://github.com/nicolasDeveloperNuevasTic/SIGIF.git
cd SIGIF
docker compose up -d --build
```

La primera vez tarda entre 5 y 10 minutos (descarga imágenes y compila). Revise que los tres servicios estén arriba:

```bash
docker compose ps
```

Debe ver `db`, `backend` y `frontend` en estado `Up`. El backend tarda unos 30 segundos más en quedar listo después de arrancar.

## 3. Acceder

| Qué | Dirección |
|---|---|
| Aplicación | https://localhost |
| Documentación del API (Swagger) | http://localhost:8080/swagger-ui.html |
| Base de datos PostgreSQL | `localhost:5432`, base `sigif`, usuario `sigif`, contraseña `sigif` |

El navegador mostrará una advertencia de certificado (es autofirmado): elija **Avanzado → Continuar a localhost**.

**Usuarios iniciales**

| Usuario | Contraseña | Rol |
|---|---|---|
| `admin` | `Admin123*` | Administrador (acceso total) |
| `bodega` | `Sigif123*` | Jefe de bodega |
| `compras` | `Sigif123*` | Compras / almacén |
| `cajero` | `Sigif123*` | Vendedor / cajero |
| `auditor` | `Sigif123*` | Auditor (solo lectura) |

Primeros pasos sugeridos: crear categorías y marcas en *Catálogos*, crear productos, registrar un proveedor en
*Clientes y proveedores*, hacer una compra y luego una venta en el *Punto de venta*.

## 4. Comandos del día a día

```bash
docker compose logs -f backend
```

Ver el log del backend en vivo (Ctrl+C para salir).

```bash
docker compose stop
```

Apagar el sistema conservando los datos.

```bash
docker compose start
```

Volver a encenderlo.

```bash
git pull && docker compose up -d --build
```

Actualizar a la última versión del repositorio.

```bash
docker compose down -v
```

Borrar todo, **incluida la base de datos**, para empezar de cero.

## 5. Configuración para producción

Copie el archivo de ejemplo y cambie los valores antes del primer `docker compose up`:

```bash
cp .env.example .env
```

Defina como mínimo `DB_PASSWORD`, `ADMIN_PASSWORD`, `JWT_SECRET` (genérelo con `openssl rand -base64 48`) y
`SEED_DEMO_USERS=false` para no crear los usuarios de demostración.

## 6. Problemas frecuentes

| Síntoma | Solución |
|---|---|
| `port is already allocated` | Otro programa usa el puerto 80, 443, 5432 u 8080. Ciérrelo o cambie el puerto en `docker-compose.yml`. |
| La página carga pero el login falla con "No fue posible conectar con el servidor" | El backend aún está arrancando; espere 30 s y revise `docker compose logs backend`. |
| `Cannot connect to the Docker daemon` | Abra Docker Desktop y espere a que diga *Engine running*. |
| Docker Desktop no abre (Windows) con un error `rename ...sock ... The file cannot be accessed by the system` | Cierre Docker Desktop, renombre las carpetas `%LOCALAPPDATA%\Docker\run` y `%LOCALAPPDATA%\docker-secrets-engine` (por ejemplo agregándoles `.old`) y vuelva a abrir Docker. |

## 7. Desarrollo sin Docker (opcional)

Requiere JDK 21, Maven 3.9 y Node 20. Levante solo la base de datos con Docker:

```bash
docker compose up -d db
```

```bash
cd backend && mvn spring-boot:run
```

En otra terminal:

```bash
cd frontend && npm install && npm run dev
```

La aplicación queda en http://localhost:5173 (sin HTTPS, con recarga automática al editar el código).
