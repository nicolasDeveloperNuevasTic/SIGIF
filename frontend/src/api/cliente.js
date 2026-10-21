/**
 * @file api/cliente.js
 * @description Cliente HTTP (axios) del API REST: token JWT, Accept-Language y normalización de errores.
 * @created 21/10/2026
 * @updated 21/10/2026
 * @author Equipo SIGIF
 */
import axios from 'axios';
import i18n from '../i18n';

/** Clave de almacenamiento de la sesión. */
export const CLAVE_SESION = 'sigif.sesion';

/** Instancia configurada para /api/v1. */
const cliente = axios.create({
  baseURL: import.meta.env.VITE_API_URL || '/api/v1',
  timeout: 20000,
  headers: { 'Content-Type': 'application/json; charset=UTF-8' },
});

cliente.interceptors.request.use((config) => {
  const sesion = leerSesion();
  if (sesion?.token) {
    config.headers.Authorization = `Bearer ${sesion.token}`;
  }
  config.headers['Accept-Language'] = i18n.language?.startsWith('en') ? 'en' : 'es';
  return config;
});

cliente.interceptors.response.use(
  (r) => r,
  (error) => {
    if (error.response?.status === 401 && !error.config.url.endsWith('/auth/login')) {
      localStorage.removeItem(CLAVE_SESION);
      window.dispatchEvent(new Event('sigif:sesion-expirada'));
    }
    return Promise.reject(error);
  },
);

/**
 * Lee la sesión guardada.
 * @returns {{token: string, usuario: object}|null} sesión o null
 */
export function leerSesion() {
  try {
    return JSON.parse(localStorage.getItem(CLAVE_SESION));
  } catch {
    return null;
  }
}

/**
 * Indica si el error se debe a falta de conexión (sin respuesta del servidor).
 * @param {unknown} error error de axios
 * @returns {boolean} true si no hubo respuesta
 */
export function esErrorDeRed(error) {
  return Boolean(error?.isAxiosError && !error.response);
}

/**
 * Extrae el mensaje i18n del ApiError del Back-End.
 * @param {unknown} error error de axios
 * @returns {string} mensaje para mostrar
 */
export function mensajeError(error) {
  const datos = error?.response?.data;
  if (datos?.errores?.length) {
    return `${datos.mensaje} ${datos.errores.map((e) => `${e.campo}: ${e.mensaje}`).join(' | ')}`;
  }
  return datos?.mensaje || i18n.t('comun.errorConexion');
}

export default cliente;
