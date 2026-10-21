-- =============================================================================
-- Nombre: V1__esquema_inicial.sql
-- Descripción: Esquema físico de la base de datos SIGIF (PostgreSQL 16).
--              Catálogo, inventario en bodega única por estados, compras,
--              ventas, terceros, cierres mensuales, RBAC, auditoría y sync offline.
-- Fecha de creación: 21/10/2026
-- Última actualización: 21/10/2026
-- Autor: Equipo SIGIF
-- =============================================================================

-- ----------------------------------------------------------------------------
-- Catálogos maestros
-- ----------------------------------------------------------------------------
CREATE TABLE categoria (
    id                  BIGSERIAL PRIMARY KEY,
    nombre              VARCHAR(120) NOT NULL,
    descripcion         VARCHAR(255),
    categoria_padre_id  BIGINT REFERENCES categoria(id),
    activo              BOOLEAN NOT NULL DEFAULT TRUE,
    creado_en           TIMESTAMPTZ NOT NULL DEFAULT now(),
    actualizado_en      TIMESTAMPTZ
);
CREATE UNIQUE INDEX ux_categoria_nombre ON categoria (lower(nombre), coalesce(categoria_padre_id, 0));

CREATE TABLE marca (
    id              BIGSERIAL PRIMARY KEY,
    nombre          VARCHAR(120) NOT NULL,
    descripcion     VARCHAR(255),
    activo          BOOLEAN NOT NULL DEFAULT TRUE,
    creado_en       TIMESTAMPTZ NOT NULL DEFAULT now(),
    actualizado_en  TIMESTAMPTZ
);
CREATE UNIQUE INDEX ux_marca_nombre ON marca (lower(nombre));

CREATE TABLE presentacion (
    id              BIGSERIAL PRIMARY KEY,
    nombre          VARCHAR(120) NOT NULL,
    descripcion     VARCHAR(255),
    activo          BOOLEAN NOT NULL DEFAULT TRUE,
    creado_en       TIMESTAMPTZ NOT NULL DEFAULT now(),
    actualizado_en  TIMESTAMPTZ
);
CREATE UNIQUE INDEX ux_presentacion_nombre ON presentacion (lower(nombre));

CREATE TABLE unidad_medida (
    id                 BIGSERIAL PRIMARY KEY,
    codigo             VARCHAR(10)  NOT NULL UNIQUE,
    nombre             VARCHAR(120) NOT NULL,
    descripcion        VARCHAR(255),
    activo             BOOLEAN NOT NULL DEFAULT TRUE,
    creado_en          TIMESTAMPTZ NOT NULL DEFAULT now(),
    actualizado_en     TIMESTAMPTZ
);

-- ----------------------------------------------------------------------------
-- Producto: cada fila es una combinación Producto + Marca + Presentación
-- identificada por un único código de barras (RN-003, RN-004).
-- ----------------------------------------------------------------------------
CREATE TABLE producto (
    id                        BIGSERIAL PRIMARY KEY,
    codigo_barras             VARCHAR(50)  NOT NULL,
    nombre                    VARCHAR(200) NOT NULL,
    descripcion               TEXT,
    categoria_id              BIGINT NOT NULL REFERENCES categoria(id),
    marca_id                  BIGINT NOT NULL REFERENCES marca(id),
    presentacion_id           BIGINT NOT NULL REFERENCES presentacion(id),
    unidad_base_id            BIGINT NOT NULL REFERENCES unidad_medida(id),
    estado                    VARCHAR(10)  NOT NULL DEFAULT 'ACTIVO',
    permite_fraccion          BOOLEAN NOT NULL DEFAULT FALSE,
    precio_compra             NUMERIC(18,4) NOT NULL,
    porcentaje_incremento     NUMERIC(7,2)  NOT NULL,
    precio_venta              NUMERIC(18,4) NOT NULL,
    porcentaje_iva            NUMERIC(5,2)  NOT NULL DEFAULT 0,
    tiene_garantia            BOOLEAN NOT NULL DEFAULT FALSE,
    meses_garantia            INTEGER,
    tiene_vencimiento         BOOLEAN NOT NULL DEFAULT FALSE,
    dias_alerta_vencimiento   INTEGER NOT NULL DEFAULT 30,
    stock_minimo              NUMERIC(18,4) NOT NULL DEFAULT 0,
    version                   BIGINT NOT NULL DEFAULT 0,
    creado_en                 TIMESTAMPTZ NOT NULL DEFAULT now(),
    actualizado_en            TIMESTAMPTZ,
    CONSTRAINT ux_producto_codigo_barras UNIQUE (codigo_barras),
    CONSTRAINT ck_producto_estado CHECK (estado IN ('ACTIVO', 'INACTIVO')),
    CONSTRAINT ck_producto_precio_compra CHECK (precio_compra > 0),
    CONSTRAINT ck_producto_porcentaje CHECK (porcentaje_incremento >= 0),
    CONSTRAINT ck_producto_precio_venta CHECK (precio_venta > 0),
    CONSTRAINT ck_producto_iva CHECK (porcentaje_iva BETWEEN 0 AND 100),
    CONSTRAINT ck_producto_garantia CHECK (NOT tiene_garantia OR (meses_garantia IS NOT NULL AND meses_garantia > 0)),
    CONSTRAINT ck_producto_stock_minimo CHECK (stock_minimo >= 0)
);
CREATE INDEX ix_producto_nombre ON producto (lower(nombre));
CREATE INDEX ix_producto_categoria ON producto (categoria_id);
CREATE INDEX ix_producto_estado ON producto (estado);

-- Factores de conversión: cuántas unidades base equivalen a 1 unidad alterna
-- (ej. base METRO; ROLLO = 100; CAJA = 1000). Admite N niveles expresados contra la base.
CREATE TABLE conversion_unidad (
    id                 BIGSERIAL PRIMARY KEY,
    producto_id        BIGINT NOT NULL REFERENCES producto(id) ON DELETE CASCADE,
    unidad_medida_id   BIGINT NOT NULL REFERENCES unidad_medida(id),
    factor             NUMERIC(18,6) NOT NULL,
    uso_compra         BOOLEAN NOT NULL DEFAULT TRUE,
    uso_venta          BOOLEAN NOT NULL DEFAULT TRUE,
    creado_en          TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT ux_conversion UNIQUE (producto_id, unidad_medida_id),
    CONSTRAINT ck_conversion_factor CHECK (factor > 0)
);

CREATE TABLE historial_precio (
    id                       BIGSERIAL PRIMARY KEY,
    producto_id              BIGINT NOT NULL REFERENCES producto(id),
    precio_compra_anterior   NUMERIC(18,4),
    precio_compra_nuevo      NUMERIC(18,4) NOT NULL,
    porcentaje_anterior      NUMERIC(7,2),
    porcentaje_nuevo         NUMERIC(7,2)  NOT NULL,
    precio_venta_anterior    NUMERIC(18,4),
    precio_venta_nuevo       NUMERIC(18,4) NOT NULL,
    motivo                   VARCHAR(30) NOT NULL,
    referencia               VARCHAR(60),
    usuario                  VARCHAR(60) NOT NULL,
    fecha                    TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT ck_historial_motivo CHECK (motivo IN ('CREACION', 'ACTUALIZACION_MANUAL', 'COMPRA', 'MIGRACION'))
);
CREATE INDEX ix_historial_precio_producto ON historial_precio (producto_id, fecha DESC);

-- ----------------------------------------------------------------------------
-- Terceros (clientes y proveedores)
-- ----------------------------------------------------------------------------
CREATE TABLE tercero (
    id                  BIGSERIAL PRIMARY KEY,
    tipo_persona        VARCHAR(10) NOT NULL,
    tipo_documento      VARCHAR(5)  NOT NULL,
    numero_documento    VARCHAR(20) NOT NULL,
    digito_verificacion VARCHAR(1),
    nombre              VARCHAR(100),
    apellido            VARCHAR(100),
    razon_social        VARCHAR(200),
    direccion           VARCHAR(200) NOT NULL,
    telefono            VARCHAR(20)  NOT NULL,
    correo              VARCHAR(150) NOT NULL,
    es_cliente          BOOLEAN NOT NULL DEFAULT FALSE,
    es_proveedor        BOOLEAN NOT NULL DEFAULT FALSE,
    es_generico         BOOLEAN NOT NULL DEFAULT FALSE,
    activo              BOOLEAN NOT NULL DEFAULT TRUE,
    creado_en           TIMESTAMPTZ NOT NULL DEFAULT now(),
    actualizado_en      TIMESTAMPTZ,
    CONSTRAINT ux_tercero_documento UNIQUE (numero_documento),
    CONSTRAINT ck_tercero_tipo_persona CHECK (tipo_persona IN ('NATURAL', 'JURIDICA')),
    CONSTRAINT ck_tercero_tipo_documento CHECK (tipo_documento IN ('CC', 'CE', 'NIT', 'PAS')),
    CONSTRAINT ck_tercero_natural CHECK (tipo_persona <> 'NATURAL' OR (nombre IS NOT NULL AND apellido IS NOT NULL)),
    CONSTRAINT ck_tercero_juridica CHECK (tipo_persona <> 'JURIDICA' OR (razon_social IS NOT NULL AND tipo_documento = 'NIT')),
    CONSTRAINT ck_tercero_rol CHECK (es_cliente OR es_proveedor)
);
CREATE INDEX ix_tercero_nombre ON tercero (lower(coalesce(razon_social, nombre || ' ' || apellido)));

-- ----------------------------------------------------------------------------
-- Seguridad RBAC
-- ----------------------------------------------------------------------------
CREATE TABLE usuario (
    id                 BIGSERIAL PRIMARY KEY,
    username           VARCHAR(60)  NOT NULL UNIQUE,
    password_hash      VARCHAR(100) NOT NULL,
    nombre_completo    VARCHAR(150) NOT NULL,
    correo             VARCHAR(150) NOT NULL UNIQUE,
    idioma             VARCHAR(5)   NOT NULL DEFAULT 'es',
    activo             BOOLEAN NOT NULL DEFAULT TRUE,
    api_key_hash       VARCHAR(64) UNIQUE,
    ultimo_acceso      TIMESTAMPTZ,
    creado_en          TIMESTAMPTZ NOT NULL DEFAULT now(),
    actualizado_en     TIMESTAMPTZ
);

CREATE TABLE rol (
    id           BIGSERIAL PRIMARY KEY,
    codigo       VARCHAR(40)  NOT NULL UNIQUE,
    nombre       VARCHAR(100) NOT NULL,
    descripcion  VARCHAR(255)
);

CREATE TABLE permiso (
    id           BIGSERIAL PRIMARY KEY,
    codigo       VARCHAR(60)  NOT NULL UNIQUE,
    modulo       VARCHAR(40)  NOT NULL,
    descripcion  VARCHAR(255) NOT NULL
);

CREATE TABLE rol_permiso (
    rol_id      BIGINT NOT NULL REFERENCES rol(id) ON DELETE CASCADE,
    permiso_id  BIGINT NOT NULL REFERENCES permiso(id) ON DELETE CASCADE,
    PRIMARY KEY (rol_id, permiso_id)
);

CREATE TABLE usuario_rol (
    usuario_id  BIGINT NOT NULL REFERENCES usuario(id) ON DELETE CASCADE,
    rol_id      BIGINT NOT NULL REFERENCES rol(id) ON DELETE CASCADE,
    PRIMARY KEY (usuario_id, rol_id)
);

-- ----------------------------------------------------------------------------
-- Numeración de documentos (Factura y Documento de Ingreso)
-- ----------------------------------------------------------------------------
CREATE TABLE consecutivo (
    codigo     VARCHAR(20) PRIMARY KEY,
    prefijo    VARCHAR(10) NOT NULL,
    siguiente  BIGINT NOT NULL DEFAULT 1
);

-- ----------------------------------------------------------------------------
-- Inventario en bodega única por estado (RN-002, RN-006, RN-008)
-- ----------------------------------------------------------------------------
CREATE TABLE inventario_saldo (
    producto_id          BIGINT PRIMARY KEY REFERENCES producto(id),
    cantidad_disponible  NUMERIC(18,4) NOT NULL DEFAULT 0,
    cantidad_reservada   NUMERIC(18,4) NOT NULL DEFAULT 0,
    cantidad_danada      NUMERIC(18,4) NOT NULL DEFAULT 0,
    version              BIGINT NOT NULL DEFAULT 0,
    actualizado_en       TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT ck_saldo_disponible CHECK (cantidad_disponible >= 0),
    CONSTRAINT ck_saldo_reservada  CHECK (cantidad_reservada  >= 0),
    CONSTRAINT ck_saldo_danada     CHECK (cantidad_danada     >= 0)
);

-- ----------------------------------------------------------------------------
-- Documento de Ingreso (entrada por compra)
-- ----------------------------------------------------------------------------
CREATE TABLE documento_ingreso (
    id                         BIGSERIAL PRIMARY KEY,
    numero                     VARCHAR(30) NOT NULL UNIQUE,
    proveedor_id               BIGINT NOT NULL REFERENCES tercero(id),
    fecha                      TIMESTAMPTZ NOT NULL DEFAULT now(),
    periodo                    INTEGER NOT NULL,
    numero_factura_proveedor   VARCHAR(40),
    subtotal                   NUMERIC(18,2) NOT NULL DEFAULT 0,
    total_iva                  NUMERIC(18,2) NOT NULL DEFAULT 0,
    total                      NUMERIC(18,2) NOT NULL DEFAULT 0,
    observaciones              VARCHAR(500),
    usuario                    VARCHAR(60) NOT NULL,
    creado_en                  TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX ix_documento_ingreso_fecha ON documento_ingreso (fecha DESC);
CREATE INDEX ix_documento_ingreso_proveedor ON documento_ingreso (proveedor_id);

CREATE TABLE lote (
    id                    BIGSERIAL PRIMARY KEY,
    producto_id           BIGINT NOT NULL REFERENCES producto(id),
    codigo_lote           VARCHAR(40) NOT NULL,
    fecha_vencimiento     DATE NOT NULL,
    cantidad_inicial      NUMERIC(18,4) NOT NULL,
    cantidad_actual       NUMERIC(18,4) NOT NULL,
    documento_ingreso_id  BIGINT REFERENCES documento_ingreso(id),
    creado_en             TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT ux_lote UNIQUE (producto_id, codigo_lote),
    CONSTRAINT ck_lote_cantidad CHECK (cantidad_actual >= 0 AND cantidad_inicial > 0)
);
CREATE INDEX ix_lote_vencimiento ON lote (producto_id, fecha_vencimiento) WHERE cantidad_actual > 0;

CREATE TABLE detalle_ingreso (
    id                    BIGSERIAL PRIMARY KEY,
    documento_ingreso_id  BIGINT NOT NULL REFERENCES documento_ingreso(id) ON DELETE CASCADE,
    producto_id           BIGINT NOT NULL REFERENCES producto(id),
    unidad_medida_id      BIGINT NOT NULL REFERENCES unidad_medida(id),
    cantidad              NUMERIC(18,4) NOT NULL,
    factor_conversion     NUMERIC(18,6) NOT NULL,
    cantidad_base         NUMERIC(18,4) NOT NULL,
    costo_unitario        NUMERIC(18,4) NOT NULL,
    porcentaje_iva        NUMERIC(5,2)  NOT NULL DEFAULT 0,
    subtotal              NUMERIC(18,2) NOT NULL,
    valor_iva             NUMERIC(18,2) NOT NULL,
    total                 NUMERIC(18,2) NOT NULL,
    lote_id               BIGINT REFERENCES lote(id),
    CONSTRAINT ck_detalle_ingreso_cantidad CHECK (cantidad > 0 AND cantidad_base > 0),
    CONSTRAINT ck_detalle_ingreso_costo CHECK (costo_unitario > 0)
);

-- ----------------------------------------------------------------------------
-- Factura (salida por venta)
-- ----------------------------------------------------------------------------
CREATE TABLE factura (
    id                 BIGSERIAL PRIMARY KEY,
    numero             VARCHAR(30) NOT NULL UNIQUE,
    cliente_id         BIGINT NOT NULL REFERENCES tercero(id),
    fecha              TIMESTAMPTZ NOT NULL DEFAULT now(),
    periodo            INTEGER NOT NULL,
    subtotal           NUMERIC(18,2) NOT NULL DEFAULT 0,
    total_descuento    NUMERIC(18,2) NOT NULL DEFAULT 0,
    total_iva          NUMERIC(18,2) NOT NULL DEFAULT 0,
    total              NUMERIC(18,2) NOT NULL DEFAULT 0,
    origen             VARCHAR(10) NOT NULL DEFAULT 'EN_LINEA',
    id_local_offline   UUID UNIQUE,
    observaciones      VARCHAR(500),
    usuario            VARCHAR(60) NOT NULL,
    creado_en          TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT ck_factura_origen CHECK (origen IN ('EN_LINEA', 'OFFLINE', 'API'))
);
CREATE INDEX ix_factura_fecha ON factura (fecha DESC);
CREATE INDEX ix_factura_cliente ON factura (cliente_id);

-- ----------------------------------------------------------------------------
-- Reservas (estado RESERVADA vinculado a cliente)
-- ----------------------------------------------------------------------------
CREATE TABLE reserva (
    id                   BIGSERIAL PRIMARY KEY,
    cliente_id           BIGINT NOT NULL REFERENCES tercero(id),
    producto_id          BIGINT NOT NULL REFERENCES producto(id),
    cantidad             NUMERIC(18,4) NOT NULL,
    cantidad_pendiente   NUMERIC(18,4) NOT NULL,
    fecha_expiracion     TIMESTAMPTZ NOT NULL,
    estado               VARCHAR(12) NOT NULL DEFAULT 'ACTIVA',
    observaciones        VARCHAR(300),
    usuario              VARCHAR(60) NOT NULL,
    creado_en            TIMESTAMPTZ NOT NULL DEFAULT now(),
    actualizado_en       TIMESTAMPTZ,
    CONSTRAINT ck_reserva_estado CHECK (estado IN ('ACTIVA', 'CONSUMIDA', 'EXPIRADA', 'CANCELADA')),
    CONSTRAINT ck_reserva_cantidad CHECK (cantidad > 0 AND cantidad_pendiente >= 0 AND cantidad_pendiente <= cantidad)
);
CREATE INDEX ix_reserva_activa ON reserva (estado, fecha_expiracion);

CREATE TABLE detalle_factura (
    id                    BIGSERIAL PRIMARY KEY,
    factura_id            BIGINT NOT NULL REFERENCES factura(id) ON DELETE CASCADE,
    producto_id           BIGINT NOT NULL REFERENCES producto(id),
    unidad_medida_id      BIGINT NOT NULL REFERENCES unidad_medida(id),
    cantidad              NUMERIC(18,4) NOT NULL,
    factor_conversion     NUMERIC(18,6) NOT NULL,
    cantidad_base         NUMERIC(18,4) NOT NULL,
    precio_unitario       NUMERIC(18,4) NOT NULL,
    porcentaje_descuento  NUMERIC(5,2)  NOT NULL DEFAULT 0,
    porcentaje_iva        NUMERIC(5,2)  NOT NULL DEFAULT 0,
    subtotal              NUMERIC(18,2) NOT NULL,
    valor_descuento       NUMERIC(18,2) NOT NULL DEFAULT 0,
    valor_iva             NUMERIC(18,2) NOT NULL DEFAULT 0,
    total                 NUMERIC(18,2) NOT NULL,
    reserva_id            BIGINT REFERENCES reserva(id),
    meses_garantia        INTEGER,
    fecha_fin_garantia    DATE,
    numero_serie          VARCHAR(60),
    CONSTRAINT ck_detalle_factura_cantidad CHECK (cantidad > 0 AND cantidad_base > 0),
    CONSTRAINT ck_detalle_factura_descuento CHECK (porcentaje_descuento BETWEEN 0 AND 100)
);

-- ----------------------------------------------------------------------------
-- Movimientos de inventario (RN-001: solo compras, ventas y cambios de estado)
-- ----------------------------------------------------------------------------
CREATE TABLE movimiento_inventario (
    id                    BIGSERIAL PRIMARY KEY,
    tipo                  VARCHAR(20) NOT NULL,
    producto_id           BIGINT NOT NULL REFERENCES producto(id),
    cantidad              NUMERIC(18,4) NOT NULL,
    estado_origen         VARCHAR(12),
    estado_destino        VARCHAR(12),
    costo_unitario        NUMERIC(18,4),
    documento_ingreso_id  BIGINT REFERENCES documento_ingreso(id),
    factura_id            BIGINT REFERENCES factura(id),
    reserva_id            BIGINT REFERENCES reserva(id),
    lote_id               BIGINT REFERENCES lote(id),
    motivo                VARCHAR(300),
    usuario               VARCHAR(60) NOT NULL,
    fecha                 TIMESTAMPTZ NOT NULL DEFAULT now(),
    periodo               INTEGER NOT NULL,
    CONSTRAINT ck_mov_tipo CHECK (tipo IN ('ENTRADA_COMPRA', 'SALIDA_VENTA', 'CAMBIO_ESTADO')),
    CONSTRAINT ck_mov_cantidad CHECK (cantidad > 0),
    CONSTRAINT ck_mov_estados CHECK (
        (tipo = 'ENTRADA_COMPRA' AND estado_origen IS NULL AND estado_destino = 'DISPONIBLE' AND documento_ingreso_id IS NOT NULL)
     OR (tipo = 'SALIDA_VENTA'  AND estado_origen IN ('DISPONIBLE', 'RESERVADA') AND estado_destino IS NULL AND factura_id IS NOT NULL)
     OR (tipo = 'CAMBIO_ESTADO' AND estado_origen IN ('DISPONIBLE', 'RESERVADA', 'DANADA')
                                AND estado_destino IN ('DISPONIBLE', 'RESERVADA', 'DANADA')
                                AND estado_origen <> estado_destino AND motivo IS NOT NULL)
    )
);
CREATE INDEX ix_mov_producto_fecha ON movimiento_inventario (producto_id, fecha DESC);
CREATE INDEX ix_mov_periodo ON movimiento_inventario (periodo);
CREATE INDEX ix_mov_tipo_fecha ON movimiento_inventario (tipo, fecha DESC);

-- ----------------------------------------------------------------------------
-- Cierres mensuales (RN-011)
-- ----------------------------------------------------------------------------
CREATE TABLE cierre_mensual (
    id                       BIGSERIAL PRIMARY KEY,
    periodo                  INTEGER NOT NULL UNIQUE,
    fecha_ejecucion          TIMESTAMPTZ NOT NULL DEFAULT now(),
    tipo_ejecucion           VARCHAR(12) NOT NULL,
    operaciones_pendientes   INTEGER NOT NULL DEFAULT 0,
    valor_inventario         NUMERIC(20,2) NOT NULL DEFAULT 0,
    observaciones            VARCHAR(500),
    usuario                  VARCHAR(60) NOT NULL,
    CONSTRAINT ck_cierre_tipo CHECK (tipo_ejecucion IN ('PROGRAMADO', 'MANUAL'))
);

CREATE TABLE cierre_mensual_detalle (
    id                   BIGSERIAL PRIMARY KEY,
    cierre_id            BIGINT NOT NULL REFERENCES cierre_mensual(id),
    producto_id          BIGINT NOT NULL REFERENCES producto(id),
    inicial_disponible   NUMERIC(18,4) NOT NULL,
    inicial_reservada    NUMERIC(18,4) NOT NULL,
    inicial_danada       NUMERIC(18,4) NOT NULL,
    compras              NUMERIC(18,4) NOT NULL,
    ventas               NUMERIC(18,4) NOT NULL,
    final_disponible     NUMERIC(18,4) NOT NULL,
    final_reservada      NUMERIC(18,4) NOT NULL,
    final_danada         NUMERIC(18,4) NOT NULL,
    costo_promedio       NUMERIC(18,4) NOT NULL,
    valor_inventario     NUMERIC(20,2) NOT NULL,
    CONSTRAINT ux_cierre_detalle UNIQUE (cierre_id, producto_id)
);

-- ----------------------------------------------------------------------------
-- Sincronización de operaciones offline (REQ-F-038/039)
-- ----------------------------------------------------------------------------
CREATE TABLE sincronizacion_operacion (
    id                BIGSERIAL PRIMARY KEY,
    id_local          UUID NOT NULL UNIQUE,
    tipo              VARCHAR(20) NOT NULL DEFAULT 'VENTA',
    dispositivo       VARCHAR(100),
    fecha_local       TIMESTAMPTZ NOT NULL,
    fecha_recepcion   TIMESTAMPTZ NOT NULL DEFAULT now(),
    estado            VARCHAR(12) NOT NULL,
    mensaje           VARCHAR(500),
    factura_id        BIGINT REFERENCES factura(id),
    payload           JSONB NOT NULL,
    usuario           VARCHAR(60) NOT NULL,
    resuelto_por      VARCHAR(60),
    fecha_resolucion  TIMESTAMPTZ,
    CONSTRAINT ck_sync_estado CHECK (estado IN ('APLICADA', 'CONFLICTO', 'RESUELTA', 'DESCARTADA'))
);
CREATE INDEX ix_sync_estado ON sincronizacion_operacion (estado);

-- ----------------------------------------------------------------------------
-- Bitácora de auditoría (alimentada por triggers - REQ-F-033/034)
-- ----------------------------------------------------------------------------
CREATE TABLE auditoria (
    id                 BIGSERIAL PRIMARY KEY,
    tabla              VARCHAR(60) NOT NULL,
    operacion          VARCHAR(30) NOT NULL,
    registro_id        VARCHAR(60),
    usuario            VARCHAR(60) NOT NULL,
    fecha              TIMESTAMPTZ NOT NULL DEFAULT clock_timestamp(),
    datos_anteriores   JSONB,
    datos_nuevos       JSONB,
    detalle            VARCHAR(500)
);
CREATE INDEX ix_auditoria_tabla_fecha ON auditoria (tabla, fecha DESC);
CREATE INDEX ix_auditoria_usuario ON auditoria (usuario, fecha DESC);
CREATE INDEX ix_auditoria_fecha ON auditoria (fecha DESC);
