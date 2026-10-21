/**
 * @file pages/CierresPage.jsx
 * @description Cierres mensuales: consulta, prevalidación (alertas de sincronización) y ejecución manual.
 * @created 21/10/2026
 * @updated 21/10/2026
 * @author Equipo SIGIF
 */
import { useCallback, useEffect, useMemo, useState } from 'react';
import {
  Alert, Button, Dialog, DialogActions, DialogContent, DialogTitle, IconButton, Stack, Tooltip,
} from '@mui/material';
import PlayIcon from '@mui/icons-material/PlayArrow';
import VisibilityIcon from '@mui/icons-material/Visibility';
import { Form, Formik } from 'formik';
import { useTranslation } from 'react-i18next';
import { cierresApi } from '../api/servicios';
import { useNotificacion } from '../context/NotificacionContext';
import { useAuth } from '../context/AuthContext';
import EncabezadoPagina from '../components/EncabezadoPagina';
import TablaDatos from '../components/TablaDatos';
import CampoTexto from '../components/CampoTexto';
import CampoCheck from '../components/CampoCheck';
import { cierreEsquema } from '../validation/esquemas';
import { ventasPendientes } from '../offline/db';
import { cantidad, fecha, moneda } from '../utils/formato';

/**
 * Convierte la clave de alerta del Back-End (ej. "cierre.ya.existe") en clave i18n del Front-End.
 * @param {string} clave clave del servidor
 * @returns {string} clave en locales/*.json
 */
const claveAlerta = (clave) => `cierre.alertas.${clave.replace(/^cierre\./, '').replace(/\./g, '_')}`;

/**
 * @returns {string} periodo AAAAMM del mes anterior
 */
function periodoAnterior() {
  const d = new Date();
  d.setDate(1);
  d.setMonth(d.getMonth() - 1);
  return `${d.getFullYear()}${String(d.getMonth() + 1).padStart(2, '0')}`;
}

/**
 * @returns {JSX.Element} pantalla de cierres
 */
export default function CierresPage() {
  const { t } = useTranslation();
  const { notificar, notificarError } = useNotificacion();
  const { tienePermiso } = useAuth();
  const [lista, setLista] = useState([]);
  const [detalle, setDetalle] = useState(null);
  const [abierto, setAbierto] = useState(false);
  const [prevalidacion, setPrevalidacion] = useState(null);
  const [pendientesLocales, setPendientesLocales] = useState(0);
  const esquema = useMemo(() => cierreEsquema(t), [t]);

  const cargar = useCallback(() => cierresApi.listar().then(setLista).catch(notificarError), [notificarError]);
  useEffect(() => {
    cargar();
    ventasPendientes().then((v) => setPendientesLocales(v.length));
  }, [cargar]);

  return (
    <>
      <EncabezadoPagina titulo={t('cierre.titulo')}>
        {tienePermiso('CIERRE_EJECUTAR') && (
          <Button variant="contained" startIcon={<PlayIcon />} onClick={() => { setPrevalidacion(null); setAbierto(true); }}>{t('cierre.ejecutar')}</Button>
        )}
      </EncabezadoPagina>
      <TablaDatos
        filas={lista}
        columnas={[
          { campo: 'periodo', titulo: t('cierre.periodo') },
          { campo: 'fechaEjecucion', titulo: t('comun.fecha'), render: (c) => fecha(c.fechaEjecucion) },
          { campo: 'tipoEjecucion', titulo: t('cierre.tipo'), render: (c) => t(`cierre.${c.tipoEjecucion}`) },
          { campo: 'valorInventario', titulo: t('cierre.valor'), align: 'right', render: (c) => moneda(c.valorInventario) },
          { campo: 'operacionesPendientes', titulo: t('cierre.pendientes'), align: 'right' },
          { campo: 'observaciones', titulo: t('comun.observaciones') },
          { campo: 'usuario', titulo: t('comun.usuario') },
          { campo: 'acc', titulo: '', render: (c) => <Tooltip title={t('comun.detalle')}><IconButton size="small" onClick={() => cierresApi.obtener(c.id).then(setDetalle).catch(notificarError)}><VisibilityIcon fontSize="small" /></IconButton></Tooltip> },
        ]}
      />

      <Dialog open={Boolean(detalle)} onClose={() => setDetalle(null)} maxWidth="xl" fullWidth>
        <DialogTitle>{t('cierre.titulo')} {detalle?.periodo} — {moneda(detalle?.valorInventario)}</DialogTitle>
        <DialogContent dividers>
          <TablaDatos
            filas={detalle?.detalles ?? []}
            clave={(d) => d.productoId}
            columnas={[
              { campo: 'producto', titulo: t('reserva.producto') },
              { campo: 'ini', titulo: `${t('cierre.inicial')} D/R/X`, align: 'right', render: (d) => `${cantidad(d.inicialDisponible)} / ${cantidad(d.inicialReservada)} / ${cantidad(d.inicialDanada)}` },
              { campo: 'compras', titulo: t('cierre.compras'), align: 'right', render: (d) => cantidad(d.compras) },
              { campo: 'ventas', titulo: t('cierre.ventas'), align: 'right', render: (d) => cantidad(d.ventas) },
              { campo: 'fin', titulo: `${t('cierre.final')} D/R/X`, align: 'right', render: (d) => `${cantidad(d.finalDisponible)} / ${cantidad(d.finalReservada)} / ${cantidad(d.finalDanada)}` },
              { campo: 'costoPromedio', titulo: t('inventario.costo'), align: 'right', render: (d) => moneda(d.costoPromedio) },
              { campo: 'valorInventario', titulo: t('inventario.valor'), align: 'right', render: (d) => moneda(d.valorInventario) },
            ]}
          />
        </DialogContent>
        <DialogActions><Button onClick={() => setDetalle(null)}>{t('comun.cerrar')}</Button></DialogActions>
      </Dialog>

      <Dialog open={abierto} onClose={() => setAbierto(false)} maxWidth="sm" fullWidth>
        <Formik
          initialValues={{ periodo: periodoAnterior(), forzar: false }}
          validationSchema={esquema}
          onSubmit={async (v, { setSubmitting }) => {
            try {
              await cierresApi.ejecutar({ periodo: Number(v.periodo), forzar: v.forzar });
              notificar(t('comun.exito'));
              setAbierto(false);
              cargar();
            } catch (e) {
              notificarError(e);
            } finally {
              setSubmitting(false);
            }
          }}
        >
          {({ values, isSubmitting, isValid }) => (
            <Form noValidate>
              <DialogTitle>{t('cierre.ejecutar')}</DialogTitle>
              <DialogContent dividers>
                <Stack spacing={2}>
                  {pendientesLocales > 0 && <Alert severity="warning">{t('cierre.pendientesLocales', { n: pendientesLocales })}</Alert>}
                  <Stack direction="row" spacing={1}>
                    <CampoTexto name="periodo" label={t('cierre.periodo')} />
                    <Button variant="outlined" disabled={!isValid}
                      onClick={() => cierresApi.prevalidar(Number(values.periodo)).then(setPrevalidacion).catch(notificarError)}>
                      {t('cierre.prevalidar')}
                    </Button>
                  </Stack>
                  {prevalidacion?.alertas.map((a) => (
                    <Alert key={a} severity={a === 'cierre.reservas.activas' ? 'info' : 'warning'}>{t(claveAlerta(a))}</Alert>
                  ))}
                  {prevalidacion && !prevalidacion.alertas.length && <Alert severity="success">OK</Alert>}
                  <CampoCheck name="forzar" label={t('cierre.forzar')} />
                </Stack>
              </DialogContent>
              <DialogActions>
                <Button onClick={() => setAbierto(false)}>{t('comun.cancelar')}</Button>
                <Button type="submit" variant="contained" color="warning" disabled={isSubmitting}>{t('cierre.ejecutar')}</Button>
              </DialogActions>
            </Form>
          )}
        </Formik>
      </Dialog>
    </>
  );
}
