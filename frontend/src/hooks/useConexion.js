/**
 * @file hooks/useConexion.js
 * @description Estado de conexión a internet y cantidad de ventas offline pendientes; dispara la sincronización automática.
 * @created 21/10/2026
 * @updated 21/10/2026
 * @author Equipo SIGIF
 */
import { useCallback, useEffect, useState } from 'react';
import { ventasPendientes } from '../offline/db';
import { sincronizarVentas } from '../offline/sincronizador';

/**
 * @param {(resumen: {aplicadas: number, conflictos: number}) => void} [alSincronizar] callback tras sincronizar
 * @returns {{enLinea: boolean, pendientes: number, sincronizar: () => Promise<void>}} estado de conexión
 */
export default function useConexion(alSincronizar) {
  const [enLinea, setEnLinea] = useState(navigator.onLine);
  const [pendientes, setPendientes] = useState(0);

  const contar = useCallback(async () => setPendientes((await ventasPendientes()).length), []);

  const sincronizar = useCallback(async () => {
    try {
      const resumen = await sincronizarVentas();
      if (resumen && alSincronizar) {
        alSincronizar(resumen);
      }
    } catch {
      // Se reintentará en el siguiente ciclo.
    } finally {
      contar();
    }
  }, [alSincronizar, contar]);

  useEffect(() => {
    const online = () => {
      setEnLinea(true);
      sincronizar();
    };
    const offline = () => setEnLinea(false);
    window.addEventListener('online', online);
    window.addEventListener('offline', offline);
    window.addEventListener('sigif:pendientes-cambiaron', contar);
    contar();
    const intervalo = setInterval(() => navigator.onLine && sincronizar(), 60000);
    return () => {
      window.removeEventListener('online', online);
      window.removeEventListener('offline', offline);
      window.removeEventListener('sigif:pendientes-cambiaron', contar);
      clearInterval(intervalo);
    };
  }, [sincronizar, contar]);

  return { enLinea, pendientes, sincronizar };
}
