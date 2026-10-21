<!--
  Nombre: ARQUITECTURA.md
  Descripción: Diagramas de arquitectura de SIGIF: despliegue, capas del Back-End y flujo de venta offline.
  Fecha de creación: 21/10/2026
  Última actualización: 21/10/2026
  Autor: Equipo SIGIF
-->
# Arquitectura de SIGIF

## 1. Vista general y despliegue

```mermaid
flowchart LR
    subgraph Puesto["Puesto de bodega / mostrador"]
        Lector["Lector de código de barras<br/>USB/Bluetooth HID"]
        Impresora["Impresora<br/>Facturas y Documentos de Ingreso"]
        subgraph Navegador["Navegador - SPA React + Material UI"]
            UI["Pantallas<br/>Formik + Yup, i18n es/en"]
            SW["Service Worker<br/>app disponible sin internet"]
            IDB[("IndexedDB<br/>catálogo, clientes,<br/>ventas pendientes")]
        end
    end

    Externo["Sistemas externos<br/>contabilidad, ERP"]

    subgraph Servidor["Servidor - docker compose"]
        Nginx["Nginx<br/>HTTPS TLS 1.2/1.3<br/>sirve la SPA y hace proxy de /api"]
        API["Back-End Spring Boot 3<br/>API REST JSON /api/v1"]
        Sched["Tareas programadas<br/>cierre mensual, expiración de reservas"]
        DB[("PostgreSQL 16<br/>Flyway + triggers")]
    end

    Lector -->|"teclado"| UI
    UI -->|"window.print"| Impresora
    UI <--> IDB
    SW -.-> UI
    UI -->|"HTTPS + JWT"| Nginx
    Externo -->|"HTTPS + X-API-Key"| Nginx
    Nginx -->|"/api"| API
    API -->|"JPA / JDBC"| DB
    Sched --> API
```

## 2. Capas del Back-End

```mermaid
flowchart TB
    Req(["Petición HTTP"]) --> Filtros

    subgraph Seguridad["Spring Security"]
        Filtros["JwtAuthenticationFilter<br/>ApiKeyAuthenticationFilter"]
        RBAC["@PreAuthorize por permiso<br/>RBAC: rol → permisos"]
    end

    subgraph Web["Controladores REST"]
        C["Producto, Catalogo, Inventario, Reserva,<br/>Compra, Venta, Tercero, Cierre,<br/>Usuario, Auditoria, Migracion,<br/>Sincronizacion, Reporte"]
        Err["GlobalExceptionHandler<br/>ApiError JSON i18n (400/401/403/404/409)"]
    end

    subgraph Servicios["Servicios de dominio - @Transactional"]
        Prod["ProductoService<br/>PrecioService<br/>ConversionService"]
        Inv["InventarioService<br/>SELECT ... FOR UPDATE<br/>FEFO por lote"]
        Docs["CompraService → Documento de Ingreso<br/>VentaService → Factura<br/>ConsecutivoService"]
        Otros["CierreService, ReservaService,<br/>SincronizacionService, MigracionExcelService"]
    end

    Aspecto["AuditoriaContextoAspect<br/>set_config('sigif.usuario')"]

    subgraph Datos["Persistencia"]
        Repo["Repositorios Spring Data JPA<br/>+ JdbcTemplate"]
        PG[("PostgreSQL")]
        Trg["Triggers: auditoría,<br/>periodo cerrado, inmutabilidad"]
    end

    Filtros --> RBAC --> C
    C --> Prod & Inv & Docs & Otros
    C -.-> Err
    Docs --> Inv
    Otros --> Inv
    Prod & Inv & Docs & Otros --> Repo
    Aspecto -.-> Repo
    Repo --> PG
    PG --- Trg
```

## 3. Venta sin internet y sincronización

```mermaid
sequenceDiagram
    actor Cajero
    participant POS as Punto de venta (React)
    participant IDB as IndexedDB
    participant API as API /sincronizacion
    participant DB as PostgreSQL

    Note over POS,IDB: Al iniciar sesión se descarga el catálogo offline
    Cajero->>POS: Escanea productos y factura
    alt Hay conexión
        POS->>API: POST /ventas
        API->>DB: Bloquea saldos, descuenta, crea factura FV-
        API-->>POS: 201 Factura
    else Sin conexión
        POS->>IDB: Guarda venta con idLocal (UUID)
        POS-->>Cajero: Recibo local OFF-xxxx
        Note over POS: Evento "online" o cada 60 s
        POS->>API: POST /sincronizacion/ventas
        loop Cada venta, en orden de fecha local
            API->>DB: Aplica en su propia transacción
            alt Hay stock
                DB-->>API: Factura FV- (APLICADA)
            else Stock insuficiente
                DB-->>API: rollback (CONFLICTO, sin saldo negativo)
            end
        end
        API-->>POS: Resultado por venta
        POS->>IDB: Borra las ventas procesadas
    end
```
