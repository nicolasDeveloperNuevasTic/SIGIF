/**
 * @file components/RutaProtegida.jsx
 * @description Restringe pantallas a usuarios autenticados y con el permiso requerido (CA-09.2).
 * @created 21/10/2026
 * @updated 21/10/2026
 * @author Equipo SIGIF
 */
import { Navigate } from 'react-router-dom';
import { Alert } from '@mui/material';
import { useTranslation } from 'react-i18next';
import { useAuth } from '../context/AuthContext';

/**
 * @param {{permisos?: string[], children: import('react').ReactNode}} props permisos requeridos (basta uno) e hijos
 * @returns {JSX.Element} contenido o redirección
 */
export default function RutaProtegida({ permisos = [], children }) {
  const { autenticado, tienePermiso } = useAuth();
  const { t } = useTranslation();
  if (!autenticado) {
    return <Navigate to="/login" replace />;
  }
  if (!tienePermiso(...permisos)) {
    return <Alert severity="error" data-testid="sin-permiso">{t('error.accesoDenegado')}</Alert>;
  }
  return children;
}
