/**
 * @file main.jsx
 * @description Punto de entrada: tema Material UI, i18n, contextos globales y enrutador.
 * @created 21/10/2026
 * @updated 21/10/2026
 * @author Equipo SIGIF
 */
import React from 'react';
import ReactDOM from 'react-dom/client';
import { BrowserRouter } from 'react-router-dom';
import { CssBaseline, ThemeProvider, createTheme } from '@mui/material';
import { esES, enUS } from '@mui/material/locale';
import { useTranslation } from 'react-i18next';
import './i18n';
import './estilos.css';
import App from './App';
import { AuthProvider } from './context/AuthContext';
import { NotificacionProvider } from './context/NotificacionContext';

/**
 * Aplica el tema con la localización de MUI según el idioma activo.
 * @returns {JSX.Element} aplicación con tema
 */
function Raiz() {
  const { i18n } = useTranslation();
  const tema = React.useMemo(() => createTheme({
    palette: { primary: { main: '#1b4f72' }, secondary: { main: '#d68910' }, background: { default: '#f4f6f8' } },
    shape: { borderRadius: 8 },
  }, i18n.language?.startsWith('en') ? enUS : esES), [i18n.language]);

  return (
    <ThemeProvider theme={tema}>
      <CssBaseline />
      <NotificacionProvider>
        <AuthProvider>
          <BrowserRouter>
            <App />
          </BrowserRouter>
        </AuthProvider>
      </NotificacionProvider>
    </ThemeProvider>
  );
}

ReactDOM.createRoot(document.getElementById('root')).render(
  <React.StrictMode>
    <Raiz />
  </React.StrictMode>,
);
