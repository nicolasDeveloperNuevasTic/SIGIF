<!--
  Nombre: MODELO-DATOS.md
  Descripción: Diagrama entidad-relación completo de la base de datos PostgreSQL de SIGIF.
  Fecha de creación: 21/10/2026
  Última actualización: 21/10/2026
  Autor: Equipo SIGIF
-->
# Modelo de datos (PostgreSQL 16)

Las migraciones Flyway en `backend/src/main/resources/db/migration` son la fuente de verdad:

- `V1__esquema_inicial.sql`: 21 tablas, llaves, índices y restricciones CHECK (saldos `>= 0`, estados válidos).
- `V2__triggers_auditoria_y_reglas.sql`: triggers de auditoría, inmutabilidad y bloqueo de periodos cerrados.
- `V3__datos_iniciales.sql`: roles, permisos, unidades, presentaciones, consecutivos y terceros genéricos.

## Diagrama entidad-relación

```mermaid
erDiagram
    CATEGORIA ||--o{ CATEGORIA : "subcategoria"
    CATEGORIA ||--o{ PRODUCTO : "clasifica"
    MARCA ||--o{ PRODUCTO : "fabrica"
    PRESENTACION ||--o{ PRODUCTO : "empaca"
    UNIDAD_MEDIDA ||--o{ PRODUCTO : "unidad base"
    PRODUCTO ||--o{ CONVERSION_UNIDAD : "factores"
    UNIDAD_MEDIDA ||--o{ CONVERSION_UNIDAD : "unidad alterna"
    PRODUCTO ||--o{ HISTORIAL_PRECIO : "costos y precios"
    PRODUCTO ||--|| INVENTARIO_SALDO : "existencias"
    PRODUCTO ||--o{ LOTE : "vencimientos"
    PRODUCTO ||--o{ MOVIMIENTO_INVENTARIO : "kardex"
    TERCERO ||--o{ DOCUMENTO_INGRESO : "proveedor"
    DOCUMENTO_INGRESO ||--|{ DETALLE_INGRESO : "lineas"
    DOCUMENTO_INGRESO ||--o{ LOTE : "origina"
    PRODUCTO ||--o{ DETALLE_INGRESO : "comprado"
    LOTE ||--o{ DETALLE_INGRESO : "ingresa"
    TERCERO ||--o{ FACTURA : "cliente"
    FACTURA ||--|{ DETALLE_FACTURA : "lineas"
    PRODUCTO ||--o{ DETALLE_FACTURA : "vendido"
    TERCERO ||--o{ RESERVA : "cliente"
    PRODUCTO ||--o{ RESERVA : "reservado"
    RESERVA ||--o{ DETALLE_FACTURA : "consumida en"
    DOCUMENTO_INGRESO ||--o{ MOVIMIENTO_INVENTARIO : "ENTRADA_COMPRA"
    FACTURA ||--o{ MOVIMIENTO_INVENTARIO : "SALIDA_VENTA"
    RESERVA ||--o{ MOVIMIENTO_INVENTARIO : "CAMBIO_ESTADO"
    LOTE ||--o{ MOVIMIENTO_INVENTARIO : "lote afectado"
    CIERRE_MENSUAL ||--|{ CIERRE_MENSUAL_DETALLE : "consolida"
    PRODUCTO ||--o{ CIERRE_MENSUAL_DETALLE : "saldo del mes"
    FACTURA ||--o| SINCRONIZACION_OPERACION : "venta offline"
    USUARIO ||--o{ USUARIO_ROL : ""
    ROL ||--o{ USUARIO_ROL : ""
    ROL ||--o{ ROL_PERMISO : ""
    PERMISO ||--o{ ROL_PERMISO : ""

    CATEGORIA {
        bigint id PK
        varchar nombre "UK con padre"
        varchar descripcion
        bigint categoria_padre_id FK
        boolean activo
    }
    MARCA {
        bigint id PK
        varchar nombre UK
        boolean activo
    }
    PRESENTACION {
        bigint id PK
        varchar nombre UK
        boolean activo
    }
    UNIDAD_MEDIDA {
        bigint id PK
        varchar codigo UK "UND, MT, KG..."
        varchar nombre
        boolean activo
    }
    PRODUCTO {
        bigint id PK
        varchar codigo_barras UK
        varchar nombre
        text descripcion
        bigint categoria_id FK
        bigint marca_id FK
        bigint presentacion_id FK
        bigint unidad_base_id FK
        varchar estado "ACTIVO | INACTIVO"
        boolean permite_fraccion
        numeric precio_compra "costo promedio > 0"
        numeric porcentaje_incremento ">= 0"
        numeric precio_venta "compra x (1 + %/100)"
        numeric porcentaje_iva
        boolean tiene_garantia
        int meses_garantia
        boolean tiene_vencimiento
        numeric stock_minimo
        bigint version "bloqueo optimista"
    }
    CONVERSION_UNIDAD {
        bigint id PK
        bigint producto_id FK
        bigint unidad_medida_id FK
        numeric factor "unidades base"
        boolean uso_compra
        boolean uso_venta
    }
    HISTORIAL_PRECIO {
        bigint id PK
        bigint producto_id FK
        numeric precio_compra_anterior
        numeric precio_compra_nuevo
        numeric porcentaje_anterior
        numeric porcentaje_nuevo
        numeric precio_venta_anterior
        numeric precio_venta_nuevo
        varchar motivo "CREACION | COMPRA | ..."
        varchar referencia
        varchar usuario
        timestamptz fecha
    }
    INVENTARIO_SALDO {
        bigint producto_id PK, FK
        numeric cantidad_disponible "CHECK >= 0"
        numeric cantidad_reservada "CHECK >= 0"
        numeric cantidad_danada "CHECK >= 0"
        bigint version
    }
    LOTE {
        bigint id PK
        bigint producto_id FK
        varchar codigo_lote "UK con producto"
        date fecha_vencimiento
        numeric cantidad_inicial
        numeric cantidad_actual "CHECK >= 0"
        bigint documento_ingreso_id FK
    }
    TERCERO {
        bigint id PK
        varchar tipo_persona "NATURAL | JURIDICA"
        varchar tipo_documento "CC | CE | NIT | PAS"
        varchar numero_documento UK
        varchar digito_verificacion
        varchar nombre
        varchar apellido
        varchar razon_social
        varchar direccion
        varchar telefono
        varchar correo
        boolean es_cliente
        boolean es_proveedor
        boolean es_generico
        boolean activo
    }
    DOCUMENTO_INGRESO {
        bigint id PK
        varchar numero UK "DI-00000001"
        bigint proveedor_id FK
        timestamptz fecha
        int periodo "AAAAMM"
        varchar numero_factura_proveedor
        numeric subtotal
        numeric total_iva
        numeric total
        varchar usuario
    }
    DETALLE_INGRESO {
        bigint id PK
        bigint documento_ingreso_id FK
        bigint producto_id FK
        bigint unidad_medida_id FK
        numeric cantidad
        numeric factor_conversion
        numeric cantidad_base
        numeric costo_unitario
        numeric porcentaje_iva
        numeric total
        bigint lote_id FK
    }
    FACTURA {
        bigint id PK
        varchar numero UK "FV-00000001"
        bigint cliente_id FK
        timestamptz fecha
        int periodo "AAAAMM"
        numeric subtotal
        numeric total_descuento
        numeric total_iva
        numeric total
        varchar origen "EN_LINEA | OFFLINE | API"
        uuid id_local_offline UK
        varchar usuario
    }
    DETALLE_FACTURA {
        bigint id PK
        bigint factura_id FK
        bigint producto_id FK
        bigint unidad_medida_id FK
        numeric cantidad "entera o fraccion"
        numeric factor_conversion
        numeric cantidad_base
        numeric precio_unitario
        numeric porcentaje_descuento
        numeric porcentaje_iva
        numeric total
        bigint reserva_id FK
        int meses_garantia
        date fecha_fin_garantia
        varchar numero_serie
    }
    RESERVA {
        bigint id PK
        bigint cliente_id FK
        bigint producto_id FK
        numeric cantidad
        numeric cantidad_pendiente
        timestamptz fecha_expiracion
        varchar estado "ACTIVA | CONSUMIDA | EXPIRADA | CANCELADA"
        varchar usuario
    }
    MOVIMIENTO_INVENTARIO {
        bigint id PK
        varchar tipo "ENTRADA_COMPRA | SALIDA_VENTA | CAMBIO_ESTADO"
        bigint producto_id FK
        numeric cantidad "> 0"
        varchar estado_origen
        varchar estado_destino
        numeric costo_unitario
        bigint documento_ingreso_id FK
        bigint factura_id FK
        bigint reserva_id FK
        bigint lote_id FK
        varchar motivo
        varchar usuario
        timestamptz fecha
        int periodo "AAAAMM"
    }
    CIERRE_MENSUAL {
        bigint id PK
        int periodo UK "AAAAMM"
        timestamptz fecha_ejecucion
        varchar tipo_ejecucion "PROGRAMADO | MANUAL"
        int operaciones_pendientes
        numeric valor_inventario
        varchar usuario
    }
    CIERRE_MENSUAL_DETALLE {
        bigint id PK
        bigint cierre_id FK
        bigint producto_id FK
        numeric inicial_disponible
        numeric inicial_reservada
        numeric inicial_danada
        numeric compras
        numeric ventas
        numeric final_disponible
        numeric final_reservada
        numeric final_danada
        numeric costo_promedio
        numeric valor_inventario
    }
    SINCRONIZACION_OPERACION {
        bigint id PK
        uuid id_local UK
        varchar dispositivo
        timestamptz fecha_local
        varchar estado "APLICADA | CONFLICTO | RESUELTA | DESCARTADA"
        varchar mensaje
        bigint factura_id FK
        jsonb payload
        varchar usuario
    }
    USUARIO {
        bigint id PK
        varchar username UK
        varchar password_hash "BCrypt"
        varchar nombre_completo
        varchar correo UK
        varchar idioma "es | en"
        boolean activo
        varchar api_key_hash UK "SHA-256"
    }
    ROL {
        bigint id PK
        varchar codigo UK "ROLE_ADMIN..."
        varchar nombre
    }
    PERMISO {
        bigint id PK
        varchar codigo UK "PRODUCTO_LEER..."
        varchar modulo
    }
    USUARIO_ROL {
        bigint usuario_id PK, FK
        bigint rol_id PK, FK
    }
    ROL_PERMISO {
        bigint rol_id PK, FK
        bigint permiso_id PK, FK
    }
    CONSECUTIVO {
        varchar codigo PK "FACTURA | DOC_INGRESO"
        varchar prefijo
        bigint siguiente
    }
    AUDITORIA {
        bigint id PK
        varchar tabla
        varchar operacion "INSERT | UPDATE | DELETE | ACCESO_DENEGADO..."
        varchar registro_id
        varchar usuario
        timestamptz fecha
        jsonb datos_anteriores
        jsonb datos_nuevos
    }
```

`AUDITORIA` y `CONSECUTIVO` no tienen llaves foráneas: `AUDITORIA` la alimentan los triggers de las demás tablas y
`CONSECUTIVO` numera facturas y documentos de ingreso.

## Reglas garantizadas por la base de datos

| Regla | Mecanismo |
|---|---|
| RN-003 Código de barras único | `UNIQUE (codigo_barras)` |
| RN-006 Estados de inventario | Columnas `cantidad_disponible/reservada/danada` + `ck_mov_estados` |
| RN-008 Sin saldos negativos | `CHECK (cantidad_* >= 0)` en `inventario_saldo` y `lote` + `SELECT ... FOR UPDATE` en la aplicación |
| RN-001 Solo compras/ventas | `ck_mov_tipo` (`ENTRADA_COMPRA`, `SALIDA_VENTA`, `CAMBIO_ESTADO`) con documento soporte obligatorio |
| RN-011 Periodo cerrado | Trigger `fn_bloquear_periodo_cerrado` en movimientos, facturas y documentos de ingreso |
| RN-014 Auditoría universal | Trigger `fn_auditoria` (usuario, fecha, operación, valores anterior/nuevo en JSONB); `auditoria` es de solo inserción |
| Contraseñas | Solo `password_hash` BCrypt; los triggers excluyen `password_hash` y `api_key_hash` de la bitácora |

El usuario de aplicación llega a los triggers mediante `set_config('sigif.usuario', ..., true)` que ejecuta
`AuditoriaContextoAspect` al inicio de cada transacción de escritura.
