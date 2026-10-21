/**
 * @file offline/sincronizador.js
 * @description Envía al servidor las ventas registradas sin conexión y refresca el catálogo local.
 * @created 21/10/2026
 * @updated 21/10/2026
 * @author Equipo SIGIF
 */
import { sincronizacionApi } from '../api/servicios';
import { eliminarPendientes, guardarCatalogo, ventasPendientes } from './db';

let enCurso = false;

/**
 * Sincroniza las ventas pendientes. Es idempotente: el servidor identifica cada venta por su
 * idLocal (UUID) y no la duplica si se reenvía.
 * @returns {Promise<{aplicadas: number, conflictos: number, resultados: object[]}|null>} resumen o null si no había pendientes
 */
export async function sincronizarVentas() {
  if (enCurso || !navigator.onLine) {
    return null;
  }
  enCurso = true;
  try {
    const pendientes = await ventasPendientes();
    if (!pendientes.length) {
      return null;
    }
    const resultados = await sincronizacionApi.enviarVentas(pendientes);
    // Las ventas en CONFLICTO quedan registradas en el servidor para revisión del supervisor.
    await eliminarPendientes(resultados.map((r) => r.idLocal));
    const conflictos = resultados.filter((r) => r.estado === 'CONFLICTO').length;
    window.dispatchEvent(new Event('sigif:pendientes-cambiaron'));
    return { aplicadas: resultados.length - conflictos, conflictos, resultados };
  } finally {
    enCurso = false;
  }
}

/**
 * Descarga el catálogo de productos y clientes para operar sin internet.
 * @returns {Promise<void>}
 */
export async function actualizarCatalogoOffline() {
  const catalogo = await sincronizacionApi.catalogo();
  await guardarCatalogo(catalogo);
}

/**
 * @returns {string} identificador estable de este puesto de venta
 */
export function idDispositivo() {
  let id = localStorage.getItem('sigif.dispositivo');
  if (!id) {
    id = `POS-${crypto.randomUUID().slice(0, 8)}`;
    localStorage.setItem('sigif.dispositivo', id);
  }
  return id;
}
