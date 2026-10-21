/**
 * @file context/AuthContext.jsx
 * @description Contexto de autenticación: sesión JWT, usuario y verificación de permisos RBAC.
 * @created 21/10/2026
 * @updated 21/10/2026
 * @author Equipo SIGIF
 */
import { createContext, useCallback, useContext, useEffect, useMemo, useState } from 'react';
import { authApi } from '../api/servicios';
import { CLAVE_SESION, leerSesion } from '../api/cliente';
import i18n from '../i18n';

const AuthContext = createContext(null);

/**
 * Proveedor de la sesión del usuario.
 * @param {{children: import('react').ReactNode}} props hijos
 * @returns {JSX.Element} proveedor
 */
export function AuthProvider({ children }) {
  const [sesion, setSesion] = useState(() => leerSesion());

  useEffect(() => {
    const expirar = () => setSesion(null);
    window.addEventListener('sigif:sesion-expirada', expirar);
    return () => window.removeEventListener('sigif:sesion-expirada', expirar);
  }, []);

  const iniciarSesion = useCallback(async (username, password) => {
    const r = await authApi.login({ username, password });
    const nueva = { token: r.token, usuario: r.usuario };
    localStorage.setItem(CLAVE_SESION, JSON.stringify(nueva));
    if (r.usuario.idioma) {
      i18n.changeLanguage(r.usuario.idioma);
    }
    setSesion(nueva);
    return nueva;
  }, []);

  const cerrarSesion = useCallback(() => {
    localStorage.removeItem(CLAVE_SESION);
    setSesion(null);
  }, []);

  /**
   * @param {...string} permisos permisos o roles requeridos (basta uno)
   * @returns {boolean} true si el usuario tiene alguno
   */
  const tienePermiso = useCallback(
    (...permisos) => permisos.length === 0 || permisos.some((p) => sesion?.usuario?.autoridades?.includes(p)),
    [sesion],
  );

  const valor = useMemo(
    () => ({ sesion, usuario: sesion?.usuario, autenticado: Boolean(sesion?.token), iniciarSesion, cerrarSesion, tienePermiso }),
    [sesion, iniciarSesion, cerrarSesion, tienePermiso],
  );

  return <AuthContext.Provider value={valor}>{children}</AuthContext.Provider>;
}

/**
 * @returns {{sesion: object, usuario: object, autenticado: boolean, iniciarSesion: Function, cerrarSesion: Function, tienePermiso: Function}} contexto de autenticación
 */
export function useAuth() {
  return useContext(AuthContext);
}
