<!--
  Nombre: DECISIONES-DISENO.md
  Descripción: Respuestas de diseño adoptadas para las preguntas abiertas de la sección 11.4 del SRS.
  Fecha de creación: 21/10/2026
  Última actualización: 21/10/2026
  Autor: Equipo SIGIF
-->
# Decisiones de diseño — Preguntas abiertas (SRS §11.4)

El SRS deja 16 preguntas sin responder antes del diseño de la base de datos. Para poder construir el sistema,
se adoptó la respuesta más conservadora y común en ferreterías colombianas. **Cada decisión debe ser validada
por Gerencia/Operaciones**; la columna "Dónde cambiarla" indica el punto exacto del código.

| # | Pregunta | Decisión adoptada | Dónde cambiarla |
|---|---|---|---|
| 1 | Producto – Marca – Presentación – Código de barras | Cada fila de `producto` es la combinación **Producto + Marca + Presentación** con **un único código de barras** (`ux_producto_codigo_barras`). Marca y presentación son catálogos. | `V1__esquema_inicial.sql` (tabla `producto`) |
| 2 | Estado del producto vs. estados de inventario | Son independientes. `producto.estado` = **ACTIVO/INACTIVO** (catálogo, eliminación lógica). Las existencias se reparten simultáneamente en **disponible / reservada / dañada** en `inventario_saldo`. | `EstadoProducto.java`, `InventarioSaldo.java` |
| 3 | Jerarquía de categorías | Jerarquía opcional (`categoria_padre_id`); una lista plana también funciona. | `Categoria.java` |
| 4 | Niveles de conversión y decimales | N niveles, todos expresados contra la **unidad base** (ej. base METRO; ROLLO = 100; CAJA = 1000). Venta: máx. **3 decimales**; inventario: 4 decimales. Si una conversión requiere más precisión se **rechaza** (sin redondeos silenciosos). | `sigif.inventario.decimales-maximos-venta`, `ConversionService` |
| 5 | Precio de fracciones | **Estrictamente proporcional** al precio por unidad base. | `VentaService.procesarLinea` |
| 6 | Método de costeo | **Costo promedio ponderado**. El % de incremento se aplica sobre ese costo y cada recálculo queda en `historial_precio`. | `PrecioService.costoPromedio` |
| 7 | Impuestos y descuentos | IVA por producto (`porcentaje_iva`) aplicado **después** del incremento y del descuento por línea. Compras registran su propio IVA. | `VentaService`, `CompraService` |
| 8 | Vencimientos por lote | **Sí**, control por lotes (`lote`) con consumo **FEFO**. Se bloquea la venta si solo quedan unidades vencidas. | `InventarioService.registrarSalida` |
| 9 | Garantía por serial | Fecha de factura + meses de garantía del producto (`fecha_fin_garantia`); **serial opcional** por línea. | `DetalleFactura.numeroSerie` |
| 10 | Devoluciones y salida de dañados | **Fuera del alcance de la v1.0** (RN-001 solo permite compras y ventas). Las unidades dañadas permanecen en estado DANADA. Se recomienda definir un tipo de movimiento "Baja" en v1.1. | `ck_mov_tipo` |
| 11 | Estado reservada | Una **reserva** (cliente + producto + cantidad) mueve DISPONIBLE→RESERVADA; vence a los **3 días** (configurable) y un proceso cada 15 min la devuelve a DISPONIBLE. Solo ese cliente puede comprarlas. | `sigif.reservas.*`, `ReservaService` |
| 12 | Corte del cierre mensual | **Día 1 a las 00:05 (America/Bogota)** se cierra el mes anterior. Las reservas activas **pasan** al mes siguiente. El cierre bloquea el periodo mediante trigger. | `sigif.cierre.cron`, `CierreService` |
| 13 | Persona natural vs. jurídica | Natural: CC/CE/PAS, **nombre y apellido** obligatorios. Jurídica: **NIT y razón social** obligatorios. Dirección, teléfono y correo obligatorios en ambos. Existe el cliente genérico **Consumidor Final** (CC 222222222222). | `TerceroService.aplicar`, `V3__datos_iniciales.sql` |
| 14 | Conflictos offline | El **servidor es la fuente de verdad**. Las ventas offline se aplican en orden de fecha local; la que no encuentre stock queda en **CONFLICTO** para que un supervisor la reintente o descarte. Nunca se generan saldos negativos. | `SincronizacionService` |
| 15 | Estructura del Excel | Hojas `Productos`, `Terceros`, `InventarioInicial` (plantilla descargable en la app). El inventario inicial entra como **compra** al proveedor genérico "SALDO INICIAL". | `MigracionExcelService.COLUMNAS` |
| 16 | Sistemas externos | API REST JSON genérica con **API Key** (`X-API-Key`, rol ROLE_INTEGRATION) sobre los mismos endpoints. Facturación electrónica DIAN / contabilidad específicas quedan pendientes de definir. | `ApiKeyAuthenticationFilter` |

## Desviaciones respecto al contrato REST de la sección 9.3

- `PATCH /api/v1/productos/{id}/estado` recibe **ACTIVO/INACTIVO**. El SRS menciona DISPONIBLE/RESERVADA/DANADA,
  pero esos son estados de las **existencias**, no del producto (pregunta #2). Las transiciones de existencias se
  hacen con `POST /api/v1/inventario/transiciones` y `POST /api/v1/reservas`.
- `ProductoRequest` recibe los **identificadores** de categoría, marca, presentación y unidad base
  (`categoriaId`, `marcaId`, ...). `ProductoResponse` devuelve los nombres (`categoria`, `marca`, `presentacion`) tal
  como el ejemplo del SRS, además de los ids y las existencias por estado.
- `fechaVencimiento` en la respuesta es el **próximo vencimiento** entre los lotes con existencias.

## Limitaciones conocidas

- El valor del cierre se calcula con el costo promedio vigente al ejecutar el cierre.
- Los lotes registran la cantidad física restante; las unidades marcadas como dañadas no se descuentan del lote.
- La numeración de facturas es secuencial sin huecos; las ventas se serializan sobre el consecutivo (adecuado para
  una sola bodega; para cientos de cajas simultáneas conviene una numeración por caja).
