-- =============================================================================
-- Nombre: V3__datos_iniciales.sql
-- Descripción: Datos semilla: roles (actores ACT-01..ACT-09), permisos RBAC,
--              unidades de medida, presentaciones, consecutivos y terceros genéricos.
--              El usuario administrador se crea al arrancar (DataInitializer) con BCrypt.
-- Fecha de creación: 21/10/2026
-- Última actualización: 21/10/2026
-- Autor: Equipo SIGIF
-- =============================================================================
SELECT set_config('sigif.usuario', 'MIGRACION_FLYWAY', false);

INSERT INTO rol (codigo, nombre, descripcion) VALUES
 ('ROLE_ADMIN',             'Administrador del Sistema',      'Acceso total (ACT-01)'),
 ('ROLE_INVENTORY_MANAGER', 'Jefe de Bodega / Inventario',    'Control de stock, catálogo y cierres (ACT-02)'),
 ('ROLE_PURCHASING',        'Gestor de Compras / Almacenista','Entradas por compra (ACT-03)'),
 ('ROLE_SALES',             'Vendedor / Cajero',              'Salidas por venta y facturación (ACT-04)'),
 ('ROLE_AUDITOR',           'Auditor / Contador',             'Solo lectura y auditoría (ACT-05)'),
 ('ROLE_INTEGRATION',       'Sistema Externo',                'Integración M2M vía API Key (ACT-09)');

INSERT INTO permiso (codigo, modulo, descripcion) VALUES
 ('PRODUCTO_LEER',           'PRODUCTOS',      'Consultar productos'),
 ('PRODUCTO_ESCRIBIR',       'PRODUCTOS',      'Crear, editar e inactivar productos'),
 ('CATALOGO_ESCRIBIR',       'PRODUCTOS',      'Gestionar categorías, marcas, presentaciones, unidades y conversiones'),
 ('PRECIO_ESCRIBIR',         'PRECIOS',        'Actualizar costos y porcentajes de incremento'),
 ('PRECIO_HISTORIAL_LEER',   'PRECIOS',        'Consultar historial de costos y precios'),
 ('INVENTARIO_LEER',         'INVENTARIO',     'Consultar existencias, lotes y movimientos'),
 ('INVENTARIO_TRANSICION',   'INVENTARIO',     'Cambiar unidades entre estados'),
 ('RESERVA_GESTIONAR',       'INVENTARIO',     'Crear y cancelar reservas'),
 ('COMPRA_LEER',             'COMPRAS',        'Consultar documentos de ingreso'),
 ('COMPRA_REGISTRAR',        'COMPRAS',        'Registrar entradas por compra'),
 ('VENTA_LEER',              'VENTAS',         'Consultar facturas'),
 ('VENTA_REGISTRAR',         'VENTAS',         'Registrar ventas y emitir facturas'),
 ('TERCERO_LEER',            'TERCEROS',       'Consultar clientes y proveedores'),
 ('TERCERO_ESCRIBIR',        'TERCEROS',       'Crear y editar clientes y proveedores'),
 ('CIERRE_LEER',             'CIERRES',        'Consultar cierres mensuales'),
 ('CIERRE_EJECUTAR',         'CIERRES',        'Ejecutar cierres mensuales manuales'),
 ('USUARIO_GESTIONAR',       'SEGURIDAD',      'Administrar usuarios, roles y permisos'),
 ('AUDITORIA_LEER',          'AUDITORIA',      'Consultar bitácora de auditoría'),
 ('MIGRACION_EJECUTAR',      'MIGRACION',      'Importar datos desde Excel'),
 ('SINCRONIZACION_EJECUTAR', 'SINCRONIZACION', 'Sincronizar operaciones offline'),
 ('SINCRONIZACION_RESOLVER', 'SINCRONIZACION', 'Resolver conflictos de sincronización'),
 ('REPORTE_LEER',            'REPORTES',       'Consultar tablero e indicadores');

-- ACT-01 Administrador: todos los permisos
INSERT INTO rol_permiso (rol_id, permiso_id)
SELECT r.id, p.id FROM rol r CROSS JOIN permiso p WHERE r.codigo = 'ROLE_ADMIN';

-- ACT-02 Jefe de Bodega: F-01 a F-05, F-08, F-11, F-13
INSERT INTO rol_permiso (rol_id, permiso_id)
SELECT r.id, p.id FROM rol r JOIN permiso p ON p.codigo IN (
 'PRODUCTO_LEER','PRODUCTO_ESCRIBIR','CATALOGO_ESCRIBIR','PRECIO_ESCRIBIR','PRECIO_HISTORIAL_LEER',
 'INVENTARIO_LEER','INVENTARIO_TRANSICION','RESERVA_GESTIONAR','COMPRA_LEER','COMPRA_REGISTRAR',
 'TERCERO_LEER','CIERRE_LEER','CIERRE_EJECUTAR','MIGRACION_EJECUTAR','REPORTE_LEER','VENTA_LEER',
 'SINCRONIZACION_RESOLVER')
WHERE r.codigo = 'ROLE_INVENTORY_MANAGER';

-- ACT-03 Compras: F-01, F-03, F-04, F-05, F-07, F-12, F-13
INSERT INTO rol_permiso (rol_id, permiso_id)
SELECT r.id, p.id FROM rol r JOIN permiso p ON p.codigo IN (
 'PRODUCTO_LEER','PRECIO_ESCRIBIR','PRECIO_HISTORIAL_LEER','INVENTARIO_LEER','COMPRA_LEER',
 'COMPRA_REGISTRAR','TERCERO_LEER','TERCERO_ESCRIBIR','SINCRONIZACION_EJECUTAR','REPORTE_LEER')
WHERE r.codigo = 'ROLE_PURCHASING';

-- ACT-04 Ventas: F-01, F-02, F-04, F-06, F-07, F-12, F-13
INSERT INTO rol_permiso (rol_id, permiso_id)
SELECT r.id, p.id FROM rol r JOIN permiso p ON p.codigo IN (
 'PRODUCTO_LEER','INVENTARIO_LEER','RESERVA_GESTIONAR','VENTA_LEER','VENTA_REGISTRAR',
 'TERCERO_LEER','TERCERO_ESCRIBIR','SINCRONIZACION_EJECUTAR','REPORTE_LEER')
WHERE r.codigo = 'ROLE_SALES';

-- ACT-05 Auditor: solo lectura F-03, F-08, F-10
INSERT INTO rol_permiso (rol_id, permiso_id)
SELECT r.id, p.id FROM rol r JOIN permiso p ON p.codigo IN (
 'PRODUCTO_LEER','PRECIO_HISTORIAL_LEER','INVENTARIO_LEER','COMPRA_LEER','VENTA_LEER',
 'TERCERO_LEER','CIERRE_LEER','AUDITORIA_LEER','REPORTE_LEER')
WHERE r.codigo = 'ROLE_AUDITOR';

-- ACT-09 Sistema externo: F-05, F-06, F-13
INSERT INTO rol_permiso (rol_id, permiso_id)
SELECT r.id, p.id FROM rol r JOIN permiso p ON p.codigo IN (
 'PRODUCTO_LEER','INVENTARIO_LEER','COMPRA_LEER','COMPRA_REGISTRAR','VENTA_LEER',
 'VENTA_REGISTRAR','TERCERO_LEER')
WHERE r.codigo = 'ROLE_INTEGRATION';

INSERT INTO unidad_medida (codigo, nombre) VALUES
 ('UND', 'Unidad'), ('MT', 'Metro'), ('CM', 'Centímetro'), ('KG', 'Kilogramo'),
 ('GR', 'Gramo'), ('LT', 'Litro'), ('ML', 'Mililitro'), ('GL', 'Galón'),
 ('CAJA', 'Caja'), ('ROLLO', 'Rollo'), ('PAQ', 'Paquete'), ('BULTO', 'Bulto');

INSERT INTO presentacion (nombre) VALUES
 ('Unidad'), ('Caja'), ('Rollo'), ('Bolsa'), ('Galón'), ('Cuarto de galón'), ('Paquete'), ('Bulto');

INSERT INTO categoria (nombre, descripcion) VALUES ('General', 'Categoría por defecto');

INSERT INTO marca (nombre) VALUES ('Genérica');

INSERT INTO consecutivo (codigo, prefijo, siguiente) VALUES
 ('FACTURA', 'FV', 1),
 ('DOC_INGRESO', 'DI', 1);

-- Terceros genéricos: Consumidor Final (ventas) y Saldo Inicial (migración de inventario)
INSERT INTO tercero (tipo_persona, tipo_documento, numero_documento, nombre, apellido, direccion, telefono, correo,
                     es_cliente, es_proveedor, es_generico)
VALUES ('NATURAL', 'CC', '222222222222', 'CONSUMIDOR', 'FINAL', 'N/A', '0000000', 'consumidor.final@sigif.local',
        TRUE, FALSE, TRUE);

INSERT INTO tercero (tipo_persona, tipo_documento, numero_documento, razon_social, direccion, telefono, correo,
                     es_cliente, es_proveedor, es_generico)
VALUES ('JURIDICA', 'NIT', '000000000', 'SALDO INICIAL MIGRACIÓN', 'N/A', '0000000', 'migracion@sigif.local',
        FALSE, TRUE, TRUE);
