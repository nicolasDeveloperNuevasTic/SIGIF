/**
 * @file pages/AuditoriaPage.jsx
 * @description Consulta filtrada de la bitácora de auditoría generada por triggers de PostgreSQL (REQ-F-035).
 * @created 21/10/2026
 * @updated 21/10/2026
 * @author Equipo SIGIF
 */
import { useCallback, useEffect, useState } from 'react';
import { Box, Button, Dialog, DialogActions, DialogContent, DialogTitle, Grid, IconButton, MenuItem, Stack, TextField, Typography } from '@mui/material';
import VisibilityIcon from '@mui/icons-material/Visibility';
import { useTranslation } from 'react-i18next';
import { auditoriaApi } from '../api/servicios';
import { useNotificacion } from '../context/NotificacionContext';
import EncabezadoPagina from '../components/EncabezadoPagina';
import TablaDatos from '../components/TablaDatos';
import { fecha } from '../utils/formato';

const TABLAS = ['producto', 'inventario_saldo', 'movimiento_inventario', 'historial_precio', 'factura', 'documento_ingreso',
  'tercero', 'usuario', 'usuario_rol', 'rol_permiso', 'reserva', 'lote', 'cierre_mensual', 'sincronizacion_operacion', 'SEGURIDAD'];
const OPERACIONES = ['INSERT', 'UPDATE', 'DELETE', 'LOGIN', 'LOGIN_FALLIDO', 'ACCESO_DENEGADO_401', 'ACCESO_DENEGADO_403'];

/**
 * @param {string|null} json texto JSON
 * @returns {string} JSON formateado
 */
const bonito = (json) => {
  if (!json) return '—';
  try {
    return JSON.stringify(JSON.parse(json), null, 2);
  } catch {
    return json;
  }
};

/**
 * @returns {JSX.Element} pantalla de auditoría
 */
export default function AuditoriaPage() {
  const { t } = useTranslation();
  const { notificarError } = useNotificacion();
  const [filtros, setFiltros] = useState({ tabla: '', operacion: '', usuario: '', registroId: '', desde: '', hasta: '' });
  const [pagina, setPagina] = useState(0);
  const [tamano, setTamano] = useState(50);
  const [datos, setDatos] = useState({ contenido: [], totalElementos: 0 });
  const [detalle, setDetalle] = useState(null);

  const cargar = useCallback(() => {
    const params = Object.fromEntries(Object.entries(filtros).filter(([, v]) => v));
    if (params.desde) params.desde = new Date(params.desde).toISOString();
    if (params.hasta) params.hasta = new Date(params.hasta).toISOString();
    auditoriaApi.buscar({ ...params, pagina, tamano }).then(setDatos).catch(notificarError);
  }, [filtros, pagina, tamano, notificarError]);

  useEffect(() => {
    const tm = setTimeout(cargar, 300);
    return () => clearTimeout(tm);
  }, [cargar]);

  const campo = (nombre) => ({ value: filtros[nombre], onChange: (e) => { setPagina(0); setFiltros({ ...filtros, [nombre]: e.target.value }); } });

  return (
    <>
      <EncabezadoPagina titulo={t('auditoria.titulo')} />
      <Stack direction={{ xs: 'column', md: 'row' }} spacing={1} mb={2}>
        <TextField select size="small" label={t('auditoria.tabla')} sx={{ minWidth: 200 }} {...campo('tabla')}>
          <MenuItem value="">{t('comun.todos')}</MenuItem>
          {TABLAS.map((x) => <MenuItem key={x} value={x}>{x}</MenuItem>)}
        </TextField>
        <TextField select size="small" label={t('auditoria.operacion')} sx={{ minWidth: 200 }} {...campo('operacion')}>
          <MenuItem value="">{t('comun.todos')}</MenuItem>
          {OPERACIONES.map((x) => <MenuItem key={x} value={x}>{x}</MenuItem>)}
        </TextField>
        <TextField size="small" label={t('comun.usuario')} {...campo('usuario')} />
        <TextField size="small" label={t('auditoria.registro')} {...campo('registroId')} />
        <TextField size="small" type="datetime-local" label={t('comun.desde')} InputLabelProps={{ shrink: true }} {...campo('desde')} />
        <TextField size="small" type="datetime-local" label={t('comun.hasta')} InputLabelProps={{ shrink: true }} {...campo('hasta')} />
      </Stack>
      <TablaDatos
        filas={datos.contenido}
        paginacion={{ pagina, tamano, total: datos.totalElementos, onPagina: setPagina, onTamano: setTamano }}
        columnas={[
          { campo: 'fecha', titulo: t('comun.fecha'), render: (a) => fecha(a.fecha) },
          { campo: 'tabla', titulo: t('auditoria.tabla') },
          { campo: 'operacion', titulo: t('auditoria.operacion') },
          { campo: 'registroId', titulo: t('auditoria.registro') },
          { campo: 'usuario', titulo: t('comun.usuario') },
          { campo: 'detalle', titulo: t('comun.detalle') },
          { campo: 'acc', titulo: '', render: (a) => (a.datosAnteriores || a.datosNuevos) && <IconButton size="small" onClick={() => setDetalle(a)}><VisibilityIcon fontSize="small" /></IconButton> },
        ]}
      />
      <Dialog open={Boolean(detalle)} onClose={() => setDetalle(null)} maxWidth="lg" fullWidth>
        <DialogTitle>{detalle?.tabla} #{detalle?.registroId} — {detalle?.operacion}</DialogTitle>
        <DialogContent dividers>
          <Grid container spacing={2}>
            {['datosAnteriores', 'datosNuevos'].map((k) => (
              <Grid item xs={12} md={6} key={k}>
                <Typography variant="subtitle2">{k === 'datosAnteriores' ? t('auditoria.anteriores') : t('auditoria.nuevos')}</Typography>
                <Box component="pre" sx={{ bgcolor: 'grey.100', p: 1, borderRadius: 1, fontSize: 12, overflow: 'auto', maxHeight: 400 }}>{bonito(detalle?.[k])}</Box>
              </Grid>
            ))}
          </Grid>
        </DialogContent>
        <DialogActions><Button onClick={() => setDetalle(null)}>{t('comun.cerrar')}</Button></DialogActions>
      </Dialog>
    </>
  );
}
