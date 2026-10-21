/**
 * @file hooks/useLectorCodigoBarras.js
 * @description Captura lecturas de lectores de código de barras USB/Bluetooth HID (emulan teclado).
 * @created 21/10/2026
 * @updated 21/10/2026
 * @author Equipo SIGIF
 */
import { useEffect, useRef } from 'react';

/** Tiempo máximo entre teclas para considerar que la entrada proviene del lector (ms). */
const INTERVALO_MAX_MS = 50;
/** Longitud mínima de un código válido. */
const LONGITUD_MIN = 4;

/**
 * Detecta ráfagas de teclas rápidas terminadas en Enter cuando el foco no está en un campo de
 * texto, y las entrega como un código escaneado (REQ-F-040).
 * @param {(codigo: string) => void} alEscanear callback con el código leído
 * @param {boolean} [activo=true] habilita la captura
 * @returns {void}
 */
export default function useLectorCodigoBarras(alEscanear, activo = true) {
  const buffer = useRef('');
  const ultima = useRef(0);
  const callback = useRef(alEscanear);
  callback.current = alEscanear;

  useEffect(() => {
    if (!activo) {
      return undefined;
    }
    const manejar = (e) => {
      const destino = e.target;
      const enCampo = destino instanceof HTMLElement
        && (destino.tagName === 'INPUT' || destino.tagName === 'TEXTAREA' || destino.isContentEditable);
      if (enCampo) {
        return;
      }
      const ahora = Date.now();
      if (ahora - ultima.current > INTERVALO_MAX_MS) {
        buffer.current = '';
      }
      ultima.current = ahora;
      if (e.key === 'Enter') {
        if (buffer.current.length >= LONGITUD_MIN) {
          callback.current(buffer.current);
        }
        buffer.current = '';
      } else if (e.key.length === 1) {
        buffer.current += e.key;
      }
    };
    window.addEventListener('keydown', manejar);
    return () => window.removeEventListener('keydown', manejar);
  }, [activo]);
}
