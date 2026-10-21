/**
 * @file pages/inventario/ReservasPage.jsx
 * @description Gestión de reservas de unidades para clientes (estado RESERVADA con expiración automática).
 * @created 21/10/2026
 * @updated 21/10/2026
 * @author Equipo SIGIF
 */
import { useCallback, useEffect, useMemo, useState } from 'react';
import { Button, Chip, Dialog, DialogActions, DialogContent, DialogTitle, MenuItem, Stack, TextField, Typography } from '@mui/material';
import AddIcon from '@mui/icons-material/Add';
import { Form, Formik } from 'formik';
import { useTranslation } from 'react-i18next';
import { inventarioApi } from '../../api/servicios';
import { useNotificacion } from '../../context/NotificacionContext';
import EncabezadoPagina from '../../components/EncabezadoPagina';
import TablaDatos from '../../components/TablaDatos';
import CampoTexto from '../../components/CampoTexto';
import SelectorTercero from '../../components/SelectorTercero';
import BuscadorProducto from '../../components/BuscadorProducto';
import { reservaEsquema } from '../../validation/esquemas';
import { cantidad, fecha } from '../../utils/formato';

/**
 * @returns {JSX.Element} pantalla de reservas
 */
export default function ReservasPage() {
  const { t } = useTranslation();
  const { notificar, notificarError } = useNotificacion();
  const [estado, setEstado] = useState('ACTIVA');
  const [pagina, setPagina] = useState(0);
  const [tamano, setTamano] = useState(20);
  const [datos, setDatos] = useState({ contenido: [], totalElementos: 0 });
  const [abierto, setAbierto] = useState(false);
  const esquema = useMemo(() => reservaEsquema(t), [t]);

  const cargar = useCallback(() => {
    inventarioApi.reservas({ estado: estado || undefined, pagina, tamano }).then(setDatos).catch(notificarError);
  }, [estado, pagina, tamano, notificarError]);

  useEffect(cargar, [cargar]);

  return (
    <>
      <EncabezadoPagina titulo={t('reserva.titulo')}>
        <Button variant="contained" startIcon={<AddIcon />} onClick={() => setAbierto(true)}>{t('reserva.nueva')}</Button>
      </EncabezadoPagina>
      <TextField select size="small" label={t('comun.estado')} value={estado} onChange={(e) => setEstado(e.target.value)} sx={{ mb: 2, minWidth: 200 }}>
        <MenuItem value="">{t('comun.todos')}</MenuItem>
        {['ACTIVA', 'CONSUMIDA', 'EXPIRADA', 'CANCELADA'].map((e) => <MenuItem key={e} value={e}>{t(`reserva.${e}`)}</MenuItem>)}
      </TextField>
      <TablaDatos
        filas={datos.contenido}
        paginacion={{ pagina, tamano, total: datos.totalElementos, onPagina: setPagina, onTamano: setTamano }}
        columnas={[
          { campo: 'id', titulo: '#' },
          { campo: 'cliente', titulo: t('reserva.cliente') },
          { campo: 'producto', titulo: t('reserva.producto') },
          { campo: 'cantidad', titulo: t('comun.cantidad'), align: 'right', render: (r) => cantidad(r.cantidad) },
          { campo: 'cantidadPendiente', titulo: t('reserva.pendiente'), align: 'right', render: (r) => cantidad(r.cantidadPendiente) },
          { campo: 'fechaExpiracion', titulo: t('reserva.expira'), render: (r) => fecha(r.fechaExpiracion) },
          { campo: 'estado', titulo: t('comun.estado'), render: (r) => <Chip size="small" label={t(`reserva.${r.estado}`)} color={r.estado === 'ACTIVA' ? 'info' : 'default'} /> },
          {
            campo: 'acc', titulo: '', align: 'right',
            render: (r) => r.estado === 'ACTIVA' && (
              <Button size="small" color="error" onClick={() => inventarioApi.cancelarReserva(r.id).then(() => { notificar(t('comun.exito')); cargar(); }).catch(notificarError)}>
                {t('reserva.cancelar')}
              </Button>
            ),
          },
        ]}
      />
      <Dialog open={abierto} onClose={() => setAbierto(false)} maxWidth="sm" fullWidth>
        <Formik
          initialValues={{ cliente: null, clienteId: '', producto: null, productoId: '', cantidad: '', diasVigencia: 3, observaciones: '' }}
          validationSchema={esquema}
          onSubmit={async (v, { setSubmitting, resetForm }) => {
            try {
              await inventarioApi.crearReserva({
                clienteId: v.clienteId, productoId: v.productoId, cantidad: Number(v.cantidad),
                diasVigencia: Number(v.diasVigencia), observaciones: v.observaciones || null,
              });
              notificar(t('comun.exito'));
              resetForm();
              setAbierto(false);
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
              <DialogTitle>{t('reserva.nueva')}</DialogTitle>
              <DialogContent dividers>
                <Stack spacing={2}>
                  <SelectorTercero rol="CLIENTE" label={t('reserva.cliente')} valor={values.cliente}
                    error={touched.clienteId && errors.clienteId}
                    onChange={(c) => { setFieldValue('cliente', c); setFieldValue('clienteId', c?.id ?? ''); }} />
                  <BuscadorProducto capturarLector={false} onSeleccionar={(p) => { setFieldValue('producto', p); setFieldValue('productoId', p.id); }} />
                  {values.producto && (
                    <Typography variant="body2">
                      {values.producto.nombre} — {t('inventario.DISPONIBLE')}: {cantidad(values.producto.disponible)} {values.producto.unidadBase}
                    </Typography>
                  )}
                  {touched.productoId && errors.productoId && <Typography color="error" variant="caption">{errors.productoId}</Typography>}
                  <CampoTexto name="cantidad" label={t('comun.cantidad')} type="number" />
                  <CampoTexto name="diasVigencia" label={t('reserva.dias')} type="number" />
                  <CampoTexto name="observaciones" label={t('comun.observaciones')} />
                </Stack>
              </DialogContent>
              <DialogActions>
                <Button onClick={() => setAbierto(false)}>{t('comun.cancelar')}</Button>
                <Button type="submit" variant="contained" disabled={isSubmitting}>{t('comun.guardar')}</Button>
              </DialogActions>
            </Form>
          )}
        </Formik>
      </Dialog>
    </>
  );
}
