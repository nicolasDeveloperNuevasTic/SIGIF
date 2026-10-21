<!--
  Nombre: README.md
  Descripción: Guía del proyecto SIGIF: arquitectura, ejecución, pruebas, CI/CD y trazabilidad con el SRS.
  Fecha de creación: 21/10/2026
  Última actualización: 21/10/2026
  Autor: Equipo SIGIF
-->
# SIGIF — Sistema Integral de Gestión de Inventario, Compras y Ventas para Ferretería

Implementación del SRS *PMOI – Documento de requerimientos v1.0* (ISO/IEC/IEEE 29148:2018, ISO/IEC 25010:2023).

| Capa | Tecnología |
|---|---|
| Base de datos | PostgreSQL 16, Flyway, triggers PL/pgSQL de auditoría y cierre |
| Back-End | Java 21, Spring Boot 3.3, Spring Data JPA, Spring Security (JWT + API Key, BCrypt), Lombok, Bean Validation, i18n, OpenAPI |
| Front-End | React 18, Material UI 5, Formik + Yup, react-i18next, IndexedDB + Service Worker (offline), Vite |
| Pruebas | JUnit 5, Mockito, Spring Boot Test, Testcontainers, Playwright, Selenium, k6 |
| CI/CD | GitHub Actions: pruebas → despliegue a producción o issue + notificación; reportes de ingeniería |

## Estructura

```
backend/            API REST (paquetes por módulo: producto, inventario, compra, venta, tercero, cierre,
                    usuario, auditoria, migracion, sincronizacion, reporte, security, config, common)
  src/main/resources/db/migration/   Modelo de datos (V1 esquema, V2 triggers, V3 datos semilla)
frontend/           SPA React + MUI
  src/components/   Componentes reutilizables (tabla, campos Formik, buscador con lector de código de barras...)
  src/pages/        Pantallas por módulo
  src/offline/      IndexedDB y sincronización de ventas sin internet
  src/validation/   Esquemas Yup
  src/i18n/         Traducciones es / en
  tests/            E2E Playwright y Selenium
pruebas-carga/      k6 (ventas concurrentes, p95 de búsquedas)
scripts/reportes/   % de cumplimiento de criterios, pendientes y cambios por integrante
docs/               Decisiones de diseño (preguntas §11.4) y modelo de datos
.github/workflows/  Pipeline CI/CD
```

## Ejecutar con Docker (recomendado)

```bash
docker compose up -d --build
```

- Aplicación: https://localhost (certificado autofirmado; acepte la advertencia del navegador)
- Swagger UI: http://localhost:8080/swagger-ui.html
- Usuario administrador: `admin` / `Admin123*` (cámbielo con la variable `ADMIN_PASSWORD`)
- Usuarios demo (`SEED_DEMO_USERS=true`, contraseña `Sigif123*`): `bodega`, `compras`, `cajero`, `auditor`, `integracion`

Copie `.env.example` a `.env` y defina `JWT_SECRET`, contraseñas y `SEED_DEMO_USERS=false` para producción.

## Ejecutar en desarrollo

Requisitos: JDK 21, Maven 3.9, Node 20 y un PostgreSQL 16 (o `docker compose up -d db`).

```bash
cd backend && mvn spring-boot:run
```

```bash
cd frontend && npm install && npm run dev
```

El Front-End queda en http://localhost:5173 con proxy a `/api` → `localhost:8080`.

## Pruebas

```bash
cd backend && mvn verify
```

Ejecuta las pruebas unitarias (Surefire) y las de integración `*IT` contra PostgreSQL real con Testcontainers
(requiere Docker): triggers de auditoría, periodo cerrado, concurrencia sin saldos negativos y BCrypt.

```bash
cd frontend && npx playwright test
```

```bash
cd frontend && npm run test:selenium
```

```bash
k6 run -e BASE_URL=https://localhost pruebas-carga/ventas-concurrentes.js
```

Las pruebas E2E y de carga se ejecutan contra el entorno de `docker compose`.

## Trazabilidad con el SRS

| Requerimiento | Implementación principal | Prueba |
|---|---|---|
| REQ-F-001..005 Productos | `producto/ProductoController`, `ProductoService`; `pages/productos` | CP-001/002/003 `ProductoControllerTest`, `productos.spec.js`, Selenium |
| REQ-F-006..008 Fracciones | `ConversionService`, `conversion_unidad` | CP-004 `ConversionServiceTest`, `InventarioIntegracionIT` |
| REQ-F-009..011 Precios | `PrecioService`, `historial_precio` | CP-005 `PrecioServiceTest` |
| REQ-F-012..017 Inventario | `InventarioService`, `inventario_saldo`, `lote`, `ReservaService` | CP-006 `InventarioServiceTest`, `InventarioSaldoTest`, k6 |
| REQ-F-018..020 Compras | `CompraService` → Documento de Ingreso `DI-########` | CP-007 IT + E2E |
| REQ-F-021..024 Ventas | `VentaService` → Factura `FV-########`, bloqueo `FOR UPDATE` | CP-007 IT + E2E, concurrencia |
| REQ-F-025..026 Terceros | `TerceroService`; `TercerosPage` | Validación Yup + Bean Validation |
| REQ-F-027..029 Cierres | `CierreService`, `CierreScheduler`, trigger de periodo | `triggerBloqueaMovimientosEnPeriodoCerrado` |
| REQ-F-030..032 RBAC | `SecurityConfig`, permisos por endpoint, `RutaProtegida` | CP-008 401/403, BCrypt |
| REQ-F-033..035 Auditoría | `V2__triggers...sql`, `AuditoriaController` | CP-009 `InventarioIntegracionIT` |
| REQ-F-036..037 Excel | `MigracionExcelService` (+ plantilla) | `MigracionExcelServiceTest` |
| REQ-F-038..039 Offline | `offline/db.js`, `sincronizador.js`, `SincronizacionService` | CP-010 `flujo-compra-venta-offline.spec.js` |
| REQ-F-040..041 Lector / externos | `useLectorCodigoBarras`, `ApiKeyAuthenticationFilter` | `consultaPorCodigoDeBarras` |
| REQ-F-042..047 CI/CD y reportes | `.github/workflows/ci-cd.yml`, `scripts/reportes` | CP-011 |

Las preguntas abiertas de la sección 11.4 se resolvieron con supuestos documentados en
[docs/DECISIONES-DISENO.md](docs/DECISIONES-DISENO.md), que deben validarse con el negocio.

## Integración con sistemas externos

Cree un usuario con rol `ROLE_INTEGRATION`, genere su API Key en *Usuarios y roles* y envíela en la cabecera
`X-API-Key`. Puede consultar productos e inventario y registrar compras y ventas con los mismos endpoints REST.
El contrato completo está en `/v3/api-docs` (OpenAPI 3).
