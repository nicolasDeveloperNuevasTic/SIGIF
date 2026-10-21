/**
 * @file pages/LoginPage.jsx
 * @description Pantalla de inicio de sesión con validación Formik + Yup.
 * @created 21/10/2026
 * @updated 21/10/2026
 * @author Equipo SIGIF
 */
import { useMemo, useState } from 'react';
import { Navigate, useNavigate } from 'react-router-dom';
import { Alert, Box, Button, Card, CardContent, MenuItem, Select, Stack, Typography } from '@mui/material';
import { Form, Formik } from 'formik';
import { useTranslation } from 'react-i18next';
import CampoTexto from '../components/CampoTexto';
import { useAuth } from '../context/AuthContext';
import { mensajeError } from '../api/cliente';
import { loginEsquema } from '../validation/esquemas';
import { actualizarCatalogoOffline } from '../offline/sincronizador';

/**
 * @returns {JSX.Element} formulario de login
 */
export default function LoginPage() {
  const { t, i18n } = useTranslation();
  const { autenticado, iniciarSesion } = useAuth();
  const navigate = useNavigate();
  const [error, setError] = useState(null);
  const esquema = useMemo(() => loginEsquema(t), [t]);

  if (autenticado) {
    return <Navigate to="/" replace />;
  }

  return (
    <Box sx={{ minHeight: '100vh', display: 'grid', placeItems: 'center', bgcolor: 'background.default', p: 2 }}>
      <Card sx={{ width: '100%', maxWidth: 400 }}>
        <CardContent>
          <Stack direction="row" justifyContent="space-between" alignItems="center" mb={1}>
            <Typography variant="h4" fontWeight={700} color="primary">{t('app.nombre')}</Typography>
            <Select size="small" value={i18n.language?.startsWith('en') ? 'en' : 'es'} onChange={(e) => i18n.changeLanguage(e.target.value)}>
              <MenuItem value="es">ES</MenuItem>
              <MenuItem value="en">EN</MenuItem>
            </Select>
          </Stack>
          <Typography variant="body2" color="text.secondary" mb={3}>{t('app.subtitulo')}</Typography>
          {!navigator.onLine && <Alert severity="warning" sx={{ mb: 2 }}>{t('login.offline')}</Alert>}
          {error && <Alert severity="error" sx={{ mb: 2 }} data-testid="error-login">{error}</Alert>}
          <Formik
            initialValues={{ username: '', password: '' }}
            validationSchema={esquema}
            onSubmit={async (v, { setSubmitting }) => {
              setError(null);
              try {
                await iniciarSesion(v.username.trim(), v.password);
                actualizarCatalogoOffline().catch(() => {});
                navigate('/');
              } catch (e) {
                setError(mensajeError(e));
              } finally {
                setSubmitting(false);
              }
            }}
          >
            {({ isSubmitting }) => (
              <Form noValidate>
                <Stack spacing={2}>
                  <CampoTexto name="username" label={t('login.usuario')} autoComplete="username" autoFocus />
                  <CampoTexto name="password" label={t('login.password')} type="password" autoComplete="current-password" />
                  <Button type="submit" variant="contained" size="large" disabled={isSubmitting} data-testid="ingresar">
                    {t('login.ingresar')}
                  </Button>
                </Stack>
              </Form>
            )}
          </Formik>
        </CardContent>
      </Card>
    </Box>
  );
}
