/**
 * @file pages/UsuariosPage.jsx
 * @description Administración RBAC: usuarios (contraseñas encriptadas en el servidor), roles, permisos y API Keys.
 * @created 21/10/2026
 * @updated 21/10/2026
 * @author Equipo SIGIF
 */
import { useCallback, useEffect, useMemo, useState } from 'react';
import {
  Alert, Button, Checkbox, Chip, Dialog, DialogActions, DialogContent, DialogTitle, FormControlLabel, FormGroup,
  Grid, IconButton, Stack, Switch, Tab, Tabs, TextField, Tooltip, Typography,
} from '@mui/material';
import AddIcon from '@mui/icons-material/Add';
import EditIcon from '@mui/icons-material/Edit';
import KeyIcon from '@mui/icons-material/VpnKey';
import PasswordIcon from '@mui/icons-material/Password';
import { Form, Formik } from 'formik';
import { useTranslation } from 'react-i18next';
import { usuariosApi } from '../api/servicios';
import { useNotificacion } from '../context/NotificacionContext';
import EncabezadoPagina from '../components/EncabezadoPagina';
import TablaDatos from '../components/TablaDatos';
import CampoTexto from '../components/CampoTexto';
import { usuarioEsquema } from '../validation/esquemas';
import { fecha } from '../utils/formato';

/**
 * @returns {JSX.Element} pantalla de usuarios y roles
 */
export default function UsuariosPage() {
  const { t } = useTranslation();
  const { notificar, notificarError } = useNotificacion();
  const [pestana, setPestana] = useState('usuarios');
  const [usuarios, setUsuarios] = useState([]);
  const [roles, setRoles] = useState([]);
  const [permisos, setPermisos] = useState([]);
  const [editando, setEditando] = useState(undefined);
  const [apiKey, setApiKey] = useState(null);
  const [cambioPassword, setCambioPassword] = useState(null);
  const [nuevaPassword, setNuevaPassword] = useState('');
  const esquema = useMemo(() => usuarioEsquema(t, !editando?.id), [t, editando]);

  const cargar = useCallback(async () => {
    try {
      const [u, r, p] = await Promise.all([usuariosApi.listar(), usuariosApi.roles(), usuariosApi.permisos()]);
      setUsuarios(u);
      setRoles(r);
      setPermisos(p);
    } catch (e) {
      notificarError(e);
    }
  }, [notificarError]);

  useEffect(() => {
    cargar();
  }, [cargar]);

  const alternarPermiso = async (rol, codigo) => {
    const nuevos = rol.permisos.includes(codigo) ? rol.permisos.filter((x) => x !== codigo) : [...rol.permisos, codigo];
    try {
      await usuariosApi.asignarPermisos(rol.id, nuevos);
      cargar();
    } catch (e) {
      notificarError(e);
    }
  };

  const modulos = [...new Set(permisos.map((p) => p.modulo))];

  return (
    <>
      <EncabezadoPagina titulo={t('usuario.titulo')}>
        {pestana === 'usuarios' && <Button variant="contained" startIcon={<AddIcon />} onClick={() => setEditando(null)}>{t('usuario.nuevo')}</Button>}
      </EncabezadoPagina>
      <Tabs value={pestana} onChange={(_, v) => setPestana(v)} sx={{ mb: 2 }}>
        <Tab value="usuarios" label={t('usuario.usuarios')} />
        <Tab value="roles" label={t('usuario.rolesTab')} />
      </Tabs>

      {pestana === 'usuarios' && (
        <TablaDatos
          filas={usuarios}
          columnas={[
            { campo: 'username', titulo: t('usuario.username') },
            { campo: 'nombreCompleto', titulo: t('usuario.nombreCompleto') },
            { campo: 'correo', titulo: t('usuario.correo') },
            { campo: 'roles', titulo: t('usuario.roles'), render: (u) => <Stack direction="row" gap={0.5} flexWrap="wrap">{u.roles.map((r) => <Chip key={r} size="small" label={r.replace('ROLE_', '')} />)}</Stack> },
            { campo: 'ultimoAcceso', titulo: t('usuario.ultimoAcceso'), render: (u) => fecha(u.ultimoAcceso) },
            { campo: 'activo', titulo: t('comun.activo'), render: (u) => <Switch size="small" checked={u.activo} onChange={(e) => usuariosApi.cambiarEstado(u.id, e.target.checked).then(cargar).catch(notificarError)} /> },
            {
              campo: 'acc', titulo: '', align: 'right',
              render: (u) => (
                <Stack direction="row" justifyContent="flex-end">
                  <Tooltip title={t('comun.editar')}><IconButton size="small" onClick={() => setEditando(u)}><EditIcon fontSize="small" /></IconButton></Tooltip>
                  <Tooltip title={t('usuario.cambiarPassword')}><IconButton size="small" onClick={() => { setNuevaPassword(''); setCambioPassword(u); }}><PasswordIcon fontSize="small" /></IconButton></Tooltip>
                  {u.roles.includes('ROLE_INTEGRATION') && (
                    <Tooltip title={t('usuario.apiKey')}>
                      <IconButton size="small" onClick={() => usuariosApi.generarApiKey(u.id).then((r) => setApiKey(r.apiKey)).catch(notificarError)}><KeyIcon fontSize="small" /></IconButton>
                    </Tooltip>
                  )}
                </Stack>
              ),
            },
          ]}
        />
      )}

      {pestana === 'roles' && (
        <Grid container spacing={2}>
          {roles.map((r) => (
            <Grid item xs={12} md={6} key={r.id}>
              <Alert severity="info" icon={false} sx={{ display: 'block' }}>
                <Typography fontWeight={700}>{r.nombre} <Chip size="small" label={r.codigo} /></Typography>
                <Typography variant="body2" mb={1}>{r.descripcion}</Typography>
                {modulos.map((m) => (
                  <FormGroup key={m} row>
                    <Typography variant="caption" sx={{ width: 120, pt: 1.2 }}>{m}</Typography>
                    {permisos.filter((p) => p.modulo === m).map((p) => (
                      <Tooltip key={p.codigo} title={p.descripcion}>
                        <FormControlLabel
                          control={<Checkbox size="small" checked={r.permisos.includes(p.codigo)} disabled={r.codigo === 'ROLE_ADMIN'} onChange={() => alternarPermiso(r, p.codigo)} />}
                          label={<Typography variant="caption">{p.codigo}</Typography>}
                        />
                      </Tooltip>
                    ))}
                  </FormGroup>
                ))}
              </Alert>
            </Grid>
          ))}
        </Grid>
      )}

      <Dialog open={editando !== undefined} onClose={() => setEditando(undefined)} maxWidth="sm" fullWidth>
        <Formik
          enableReinitialize
          initialValues={{
            username: editando?.username ?? '', nombreCompleto: editando?.nombreCompleto ?? '', correo: editando?.correo ?? '',
            idioma: editando?.idioma ?? 'es', password: '', roles: editando?.roles ?? [],
          }}
          validationSchema={esquema}
          onSubmit={async (v, { setSubmitting }) => {
            try {
              const cuerpo = { ...v, password: v.password || null };
              if (editando?.id) await usuariosApi.actualizar(editando.id, cuerpo);
              else await usuariosApi.crear(cuerpo);
              notificar(t('comun.exito'));
              setEditando(undefined);
              cargar();
            } catch (e) {
              notificarError(e);
            } finally {
              setSubmitting(false);
            }
          }}
        >
          {({ values, setFieldValue, errors, touched, isSubmitting }) => (
            <Form noValidate>
              <DialogTitle>{editando ? t('comun.editar') : t('usuario.nuevo')}</DialogTitle>
              <DialogContent dividers>
                <Stack spacing={2}>
                  <CampoTexto name="username" label={t('usuario.username')} disabled={Boolean(editando?.id)} />
                  <CampoTexto name="nombreCompleto" label={t('usuario.nombreCompleto')} />
                  <CampoTexto name="correo" label={t('usuario.correo')} />
                  <CampoTexto name="idioma" label={t('comun.idioma')} opciones={[{ value: 'es', label: 'Español' }, { value: 'en', label: 'English' }]} />
                  {!editando?.id && <CampoTexto name="password" label={t('usuario.password')} type="password" />}
                  <Typography variant="subtitle2">{t('usuario.roles')}</Typography>
                  <FormGroup>
                    {roles.map((r) => (
                      <FormControlLabel key={r.codigo} label={`${r.nombre} (${r.codigo})`}
                        control={<Checkbox checked={values.roles.includes(r.codigo)}
                          onChange={(e) => setFieldValue('roles', e.target.checked ? [...values.roles, r.codigo] : values.roles.filter((x) => x !== r.codigo))} />} />
                    ))}
                  </FormGroup>
                  {touched.roles && errors.roles && <Typography color="error" variant="caption">{errors.roles}</Typography>}
                </Stack>
              </DialogContent>
              <DialogActions>
                <Button onClick={() => setEditando(undefined)}>{t('comun.cancelar')}</Button>
                <Button type="submit" variant="contained" disabled={isSubmitting}>{t('comun.guardar')}</Button>
              </DialogActions>
            </Form>
          )}
        </Formik>
      </Dialog>

      <Dialog open={Boolean(cambioPassword)} onClose={() => setCambioPassword(null)} maxWidth="xs" fullWidth>
        <DialogTitle>{t('usuario.cambiarPassword')} — {cambioPassword?.username}</DialogTitle>
        <DialogContent dividers>
          <TextField fullWidth size="small" type="password" label={t('usuario.password')} value={nuevaPassword}
            onChange={(e) => setNuevaPassword(e.target.value)} helperText={t('validacion.password')} />
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setCambioPassword(null)}>{t('comun.cancelar')}</Button>
          <Button variant="contained" disabled={!/^(?=.*[a-z])(?=.*[A-Z])(?=.*\d).{8,72}$/.test(nuevaPassword)}
            onClick={() => usuariosApi.cambiarPassword(cambioPassword.id, nuevaPassword)
              .then(() => { notificar(t('comun.exito')); setCambioPassword(null); }).catch(notificarError)}>
            {t('comun.guardar')}
          </Button>
        </DialogActions>
      </Dialog>

      <Dialog open={Boolean(apiKey)} onClose={() => setApiKey(null)} maxWidth="sm" fullWidth>
        <DialogTitle>{t('usuario.apiKey')}</DialogTitle>
        <DialogContent dividers>
          <Alert severity="warning" sx={{ mb: 2 }}>{t('usuario.apiKeyGenerada')}</Alert>
          <TextField fullWidth value={apiKey ?? ''} InputProps={{ readOnly: true }} onFocus={(e) => e.target.select()} />
        </DialogContent>
        <DialogActions><Button onClick={() => setApiKey(null)}>{t('comun.cerrar')}</Button></DialogActions>
      </Dialog>
    </>
  );
}
