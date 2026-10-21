/**
 * @file utils/formato.js
 * @description Formateo de moneda, cantidades y fechas según el idioma activo.
 * @created 21/10/2026
 * @updated 21/10/2026
 * @author Equipo SIGIF
 */
import i18n from '../i18n';

const locale = () => (i18n.language?.startsWith('en') ? 'en-US' : 'es-CO');

/**
 * @param {number|string|null} valor valor monetario
 * @returns {string} valor en pesos colombianos
 */
export function moneda(valor) {
  if (valor === null || valor === undefined || valor === '') return '';
  return new Intl.NumberFormat(locale(), { style: 'currency', currency: 'COP', maximumFractionDigits: 2 }).format(Number(valor));
}

/**
 * @param {number|string|null} valor cantidad
 * @returns {string} cantidad con hasta 4 decimales
 */
export function cantidad(valor) {
  if (valor === null || valor === undefined || valor === '') return '';
  return new Intl.NumberFormat(locale(), { maximumFractionDigits: 4 }).format(Number(valor));
}

/**
 * @param {string|null} iso fecha ISO-8601
 * @param {boolean} [conHora=true] incluir hora
 * @returns {string} fecha legible
 */
export function fecha(iso, conHora = true) {
  if (!iso) return '';
  const d = new Date(iso.length === 10 ? `${iso}T00:00:00` : iso);
  return conHora
    ? d.toLocaleString(locale(), { dateStyle: 'short', timeStyle: 'short' })
    : d.toLocaleDateString(locale(), { dateStyle: 'medium' });
}

/**
 * Redondeo monetario a 2 decimales (HALF_UP) usado en cálculos offline.
 * @param {number} v valor
 * @returns {number} valor redondeado
 */
export function redondear(v) {
  return Math.round((v + Number.EPSILON) * 100) / 100;
}
