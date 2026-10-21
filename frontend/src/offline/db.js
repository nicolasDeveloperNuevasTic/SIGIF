/**
 * @file offline/db.js
 * @description Almacenamiento local IndexedDB para operar sin internet: catálogo, clientes y ventas pendientes.
 * @created 21/10/2026
 * @updated 21/10/2026
 * @author Equipo SIGIF
 */
import { openDB } from 'idb';

const NOMBRE = 'sigif-offline';
const VERSION = 1;

let conexion;

/**
 * Abre (o crea) la base local.
 * @returns {Promise<import('idb').IDBPDatabase>} base IndexedDB
 */
function db() {
  if (!conexion) {
    conexion = openDB(NOMBRE, VERSION, {
      upgrade(base) {
        const productos = base.createObjectStore('productos', { keyPath: 'id' });
        productos.createIndex('codigoBarras', 'codigoBarras', { unique: true });
        base.createObjectStore('clientes', { keyPath: 'id' });
        base.createObjectStore('ventasPendientes', { keyPath: 'idLocal' });
        base.createObjectStore('meta');
      },
    });
  }
  return conexion;
}

/**
 * Reemplaza el catálogo local con el descargado del servidor.
 * @param {{productos: object[], clientes: object[], generadoEn: string}} catalogo catálogo offline
 * @returns {Promise<void>}
 */
export async function guardarCatalogo(catalogo) {
  const base = await db();
  const tx = base.transaction(['productos', 'clientes', 'meta'], 'readwrite');
  await tx.objectStore('productos').clear();
  await tx.objectStore('clientes').clear();
  await Promise.all(catalogo.productos.map((p) => tx.objectStore('productos').put(p)));
  await Promise.all(catalogo.clientes.map((c) => tx.objectStore('clientes').put(c)));
  await tx.objectStore('meta').put(catalogo.generadoEn, 'catalogoGeneradoEn');
  await tx.done;
}

/**
 * @param {string} codigo código de barras
 * @returns {Promise<object|undefined>} producto local
 */
export async function productoPorCodigo(codigo) {
  return (await db()).getFromIndex('productos', 'codigoBarras', codigo);
}

/**
 * @param {string} texto texto a buscar en nombre o código
 * @param {number} [limite=20] máximo de resultados
 * @returns {Promise<object[]>} productos coincidentes
 */
export async function buscarProductos(texto, limite = 20) {
  const t = texto.trim().toLowerCase();
  const todos = await (await db()).getAll('productos');
  return todos.filter((p) => p.nombre.toLowerCase().includes(t) || p.codigoBarras.includes(t)).slice(0, limite);
}

/**
 * @returns {Promise<object[]>} clientes locales
 */
export async function clientesLocales() {
  return (await db()).getAll('clientes');
}

/**
 * Guarda una venta para sincronizar y descuenta el disponible local (referencial).
 * @param {object} venta venta offline {idLocal, fechaLocal, dispositivo, venta}
 * @param {{productoId: number, cantidadBase: number}[]} descuentos cantidades a descontar
 * @returns {Promise<void>}
 */
export async function guardarVentaPendiente(venta, descuentos) {
  const base = await db();
  const tx = base.transaction(['ventasPendientes', 'productos'], 'readwrite');
  await tx.objectStore('ventasPendientes').put(venta);
  for (const d of descuentos) {
    const p = await tx.objectStore('productos').get(d.productoId);
    if (p) {
      p.disponible = Number(p.disponible) - d.cantidadBase;
      await tx.objectStore('productos').put(p);
    }
  }
  await tx.done;
}

/**
 * @returns {Promise<object[]>} ventas pendientes por sincronizar
 */
export async function ventasPendientes() {
  return (await db()).getAll('ventasPendientes');
}

/**
 * @param {string[]} ids idLocal de las ventas ya procesadas por el servidor
 * @returns {Promise<void>}
 */
export async function eliminarPendientes(ids) {
  const base = await db();
  const tx = base.transaction('ventasPendientes', 'readwrite');
  await Promise.all(ids.map((id) => tx.store.delete(id)));
  await tx.done;
}

/**
 * @returns {Promise<string|undefined>} fecha de generación del catálogo local
 */
export async function fechaCatalogo() {
  return (await db()).get('meta', 'catalogoGeneradoEn');
}
