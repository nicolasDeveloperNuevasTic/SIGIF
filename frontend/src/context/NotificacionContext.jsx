/**
 * @file context/NotificacionContext.jsx
 * @description Notificaciones globales (Snackbar de Material UI) de éxito, error y advertencia.
 * @created 21/10/2026
 * @updated 21/10/2026
 * @author Equipo SIGIF
 */
import { createContext, useCallback, useContext, useMemo, useState } from 'react';
import { Alert, Snackbar } from '@mui/material';
import { mensajeError } from '../api/cliente';

const NotificacionContext = createContext(null);

/**
 * Proveedor de notificaciones.
 * @param {{children: import('react').ReactNode}} props hijos
 * @returns {JSX.Element} proveedor
 */
export function NotificacionProvider({ children }) {
  const [aviso, setAviso] = useState(null);

  const notificar = useCallback((mensaje, severidad = 'success') => setAviso({ mensaje, severidad, clave: Date.now() }), []);
  const notificarError = useCallback((error) => notificar(mensajeError(error), 'error'), [notificar]);
  const valor = useMemo(() => ({ notificar, notificarError }), [notificar, notificarError]);

  return (
    <NotificacionContext.Provider value={valor}>
      {children}
      <Snackbar
        key={aviso?.clave}
        open={Boolean(aviso)}
        autoHideDuration={aviso?.severidad === 'error' ? 8000 : 4000}
        onClose={() => setAviso(null)}
        anchorOrigin={{ vertical: 'bottom', horizontal: 'center' }}
      >
        <Alert severity={aviso?.severidad || 'info'} variant="filled" onClose={() => setAviso(null)} data-testid="notificacion">
          {aviso?.mensaje}
        </Alert>
      </Snackbar>
    </NotificacionContext.Provider>
  );
}

/**
 * @returns {{notificar: (mensaje: string, severidad?: string) => void, notificarError: (error: unknown) => void}} API de notificaciones
 */
export function useNotificacion() {
  return useContext(NotificacionContext);
}
