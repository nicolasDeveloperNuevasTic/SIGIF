/**
 * @file pages/TercerosPage.jsx
 * @description CRUD de clientes y proveedores (persona natural o jurídica) con validación Formik + Yup.
 * @created 21/10/2026
 * @updated 21/10/2026
 * @author Equipo SIGIF
 */
import { useCallback, useEffect, useMemo, useState } from 'react';
import {
  Button, Chip, Dialog, DialogActions, DialogContent, DialogTitle, Grid, IconButton, MenuItem, Stack, Switch, TextField, Tooltip,
} from '@mui/material';
import AddIcon from '@mui/icons-material/Add';
import EditIcon from '@mui/icons-material/Edit';
import { Form, Formik } from 'formik';
import { useTranslation } from 'react-i18next';
import { tercerosApi } from '../api/servicios';
import { useNotificacion } from '../context/NotificacionContext';
import { useAuth } from '../context/AuthContext';
import EncabezadoPagina from '../components/EncabezadoPagina';
import TablaDatos from '../components/TablaDatos';
import CampoTexto from '../components/CampoTexto';
import CampoCheck from '../components/CampoCheck';
import { terceroEsquema } from '../validation/esquemas';

const INICIAL = {
  tipoPersona: 'NATURAL', tipoDocumento: 'CC', numeroDocumento: '', digitoVerificacion: '', nombre: '', apellido: '',
  razonSocial: '', direccion: '', telefono: '', correo: '', esCliente: true, esProveedor: false,
};

/**
 * @returns {JSX.Element} pantalla de terceros
 */
export default function TercerosPage() {
  const { t } = useTranslation();
  const { notificar, notificarError } = useNotificacion();
  const { tienePermiso } = useAuth();
  const [rol, setRol] = useState('');
  const [q, setQ] = useState('');
  const [pagina, setPagina] = useState(0);
  const [tamano, setTamano] = useState(20);
  const [datos, setDatos] = useState({ contenido: [], totalElementos: 0 });
  const [editando, setEditando] = useState(undefined);
  const esquema = useMemo(() => terceroEsquema(t), [t]);
  const escribir = tienePermiso('TERCERO_ESCRIBIR');

  const cargar = useCallback(() => {
    tercerosApi.buscar({ rol: rol || undefined, q: q || undefined, pagina, tamano }).then(setDatos).catch(notificarError);
  }, [rol, q, pagina, tamano, notificarError]);

  useEffect(() => {
    const tm = setTimeout(cargar, 300);
    return () => clearTimeout(tm);
  }, [cargar]);

  const limpiar = (o) => Object.fromEntries(Object.entries(o).map(([k, v]) => [k, v === '' ? null : v]));

  return (
    <>
      <EncabezadoPagina titulo={t('tercero.titulo')}>
        {escribir && <Button variant="contained" startIcon={<AddIcon />} onClick={() => setEditando(null)} data-testid="nuevo-tercero">{t('tercero.nuevo')}</Button>}
      </EncabezadoPagina>
      <Stack direction={{ xs: 'column', sm: 'row' }} spacing={1} mb={2}>
        <TextField size="small" label={t('comun.buscar')} value={q} onChange={(e) => { setQ(e.target.value); setPagina(0); }} sx={{ flex: 1 }} />
        <TextField select size="small" label={t('tercero.rol')} value={rol} onChange={(e) => setRol(e.target.value)} sx={{ minWidth: 180 }}>
          <MenuItem value="">{t('comun.todos')}</MenuItem>
          <MenuItem value="CLIENTE">{t('tercero.CLIENTE')}</MenuItem>
          <MenuItem value="PROVEEDOR">{t('tercero.PROVEEDOR')}</MenuItem>
        </TextField>
      </Stack>
      <TablaDatos
        filas={datos.contenido}
        paginacion={{ pagina, tamano, total: datos.totalElementos, onPagina: setPagina, onTamano: setTamano }}
        columnas={[
          { campo: 'numeroDocumento', titulo: t('tercero.numeroDocumento'), render: (x) => `${x.tipoDocumento} ${x.numeroDocumento}${x.digitoVerificacion ? `-${x.digitoVerificacion}` : ''}` },
          { campo: 'nombreMostrar', titulo: t('tercero.nombre') },
          { campo: 'telefono', titulo: t('tercero.telefono') },
          { campo: 'correo', titulo: t('tercero.correo') },
          {
            campo: 'rol', titulo: t('tercero.rol'),
            render: (x) => (
              <Stack direction="row" spacing={0.5}>
                {x.esCliente && <Chip size="small" label={t('tercero.esCliente')} />}
                {x.esProveedor && <Chip size="small" color="secondary" label={t('tercero.esProveedor')} />}
              </Stack>
            ),
          },
          {
            campo: 'activo', titulo: t('comun.activo'),
            render: (x) => (
              <Switch size="small" checked={x.activo} disabled={!escribir || x.esGenerico}
                onChange={(e) => tercerosApi.cambiarEstado(x.id, e.target.checked).then(cargar).catch(notificarError)} />
            ),
          },
          {
            campo: 'acc', titulo: '', align: 'right',
            render: (x) => escribir && !x.esGenerico && (
              <Tooltip title={t('comun.editar')}><IconButton size="small" onClick={() => setEditando(x)}><EditIcon fontSize="small" /></IconButton></Tooltip>
            ),
          },
        ]}
      />
      <Dialog open={editando !== undefined} onClose={() => setEditando(undefined)} maxWidth="md" fullWidth>
        <Formik
          enableReinitialize
          initialValues={editando ? Object.fromEntries(Object.keys(INICIAL).map((k) => [k, editando[k] ?? ''])) : INICIAL}
          validationSchema={esquema}
          onSubmit={async (v, { setSubmitting }) => {
            try {
              if (editando?.id) await tercerosApi.actualizar(editando.id, limpiar(v));
              else await tercerosApi.crear(limpiar(v));
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
          {({ values, isSubmitting, setFieldValue }) => (
            <Form noValidate data-testid="form-tercero">
              <DialogTitle>{editando ? t('tercero.editar') : t('tercero.nuevo')}</DialogTitle>
              <DialogContent dividers>
                <Grid container spacing={2}>
                  <Grid item xs={12} sm={4}>
                    <CampoTexto name="tipoPersona" label={t('tercero.tipoPersona')}
                      opciones={['NATURAL', 'JURIDICA'].map((x) => ({ value: x, label: t(`tercero.${x}`) }))}
                      onChange={(e) => {
                        setFieldValue('tipoPersona', e.target.value);
                        setFieldValue('tipoDocumento', e.target.value === 'JURIDICA' ? 'NIT' : 'CC');
                      }} />
                  </Grid>
                  <Grid item xs={12} sm={2}>
                    <CampoTexto name="tipoDocumento" label={t('tercero.tipoDocumento')}
                      opciones={(values.tipoPersona === 'JURIDICA' ? ['NIT'] : ['CC', 'CE', 'NIT', 'PAS']).map((x) => ({ value: x, label: x }))} />
                  </Grid>
                  <Grid item xs={9} sm={4}><CampoTexto name="numeroDocumento" label={t('tercero.numeroDocumento')} /></Grid>
                  <Grid item xs={3} sm={2}><CampoTexto name="digitoVerificacion" label={t('tercero.dv')} /></Grid>
                  <Grid item xs={12} sm={4}><CampoTexto name="nombre" label={t('tercero.nombre')} /></Grid>
                  <Grid item xs={12} sm={4}><CampoTexto name="apellido" label={t('tercero.apellido')} /></Grid>
                  <Grid item xs={12} sm={4}><CampoTexto name="razonSocial" label={t('tercero.razonSocial')} /></Grid>
                  <Grid item xs={12} sm={6}><CampoTexto name="direccion" label={t('tercero.direccion')} /></Grid>
                  <Grid item xs={12} sm={3}><CampoTexto name="telefono" label={t('tercero.telefono')} /></Grid>
                  <Grid item xs={12} sm={3}><CampoTexto name="correo" label={t('tercero.correo')} type="email" /></Grid>
                  <Grid item xs={6}><CampoCheck name="esCliente" label={t('tercero.esCliente')} /></Grid>
                  <Grid item xs={6}><CampoCheck name="esProveedor" label={t('tercero.esProveedor')} /></Grid>
                </Grid>
              </DialogContent>
              <DialogActions>
                <Button onClick={() => setEditando(undefined)}>{t('comun.cancelar')}</Button>
                <Button type="submit" variant="contained" disabled={isSubmitting} data-testid="guardar-tercero">{t('comun.guardar')}</Button>
              </DialogActions>
            </Form>
          )}
        </Formik>
      </Dialog>
    </>
  );
}
