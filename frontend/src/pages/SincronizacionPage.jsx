/**
 * @file pages/SincronizacionPage.jsx
 * @description Estado del modo offline: ventas pendientes locales, sincronización manual, catálogo local y
 *              resolución de conflictos recibidos por el servidor.
 * @created 21/10/2026
 * @updated 21/10/2026
 * @author Equipo SIGIF
 */
import { useCallback, useEffect, useState } from 'react';
import { Button, Chip, MenuItem, Stack, TextField, Typography } from '@mui/material';
import SyncIcon from '@mui/icons-material/Sync';
import CloudDownloadIcon from '@mui/icons-material/CloudDownload';
import { useTranslation } from 'react-i18next';
import { sincronizacionApi } from '../api/servicios';
import { useNotificacion } from '../context/NotificacionContext';
import { useAuth } from '../context/AuthContext';
import EncabezadoPagina from '../components/EncabezadoPagina';
import TablaDatos from '../components/TablaDatos';
import { fechaCatalogo, ventasPendientes } from '../offline/db';
import { actualizarCatalogoOffline, sincronizarVentas } from '../offline/sincronizador';
import { fecha } from '../utils/formato';

/**
 * @returns {JSX.Element} pantalla de sincronización
 */
export default function SincronizacionPage() {
  const { t } = useTranslation();
  const { notificar, notificarError } = useNotificacion();
  const { tienePermiso } = useAuth();
  const [pendientes, setPendientes] = useState([]);
  const [catalogoEn, setCatalogoEn] = useState(null);
  const [estado, setEstado] = useState('CONFLICTO');
  const [operaciones, setOperaciones] = useState([]);
  const resolver = tienePermiso('SINCRONIZACION_RESOLVER');

  const cargar = useCallback(async () => {
    setPendientes(await ventasPendientes());
    setCatalogoEn(await fechaCatalogo());
    if (navigator.onLine) {
      sincronizacionApi.operaciones(estado || undefined).then(setOperaciones).catch(notificarError);
    }
  }, [estado, notificarError]);

  useEffect(() => {
    cargar();
  }, [cargar]);

  const accion = (fn) => async (...args) => {
    try {
      await fn(...args);
      notificar(t('comun.exito'));
    } catch (e) {
      notificarError(e);
    } finally {
      cargar();
    }
  };

  return (
    <>
      <EncabezadoPagina titulo={t('sincronizacion.titulo')}>
        <Button startIcon={<CloudDownloadIcon />} onClick={accion(actualizarCatalogoOffline)}>{t('sincronizacion.actualizarCatalogo')}</Button>
        <Button variant="contained" startIcon={<SyncIcon />} onClick={accion(async () => {
          const r = await sincronizarVentas();
          if (r) notificar(t('conexion.sincronizado', r), r.conflictos ? 'warning' : 'success');
        })}>{t('sincronizacion.sincronizarAhora')}</Button>
      </EncabezadoPagina>
      <Typography variant="body2" mb={1}>{t('sincronizacion.catalogoLocal')}: {fecha(catalogoEn) || '—'}</Typography>
      <Typography variant="subtitle1" fontWeight={600} mb={1}>{t('sincronizacion.pendientesLocales')} ({pendientes.length})</Typography>
      <TablaDatos
        filas={pendientes}
        clave={(p) => p.idLocal}
        columnas={[
          { campo: 'idLocal', titulo: 'ID', render: (p) => p.idLocal.slice(0, 8) },
          { campo: 'fechaLocal', titulo: t('comun.fecha'), render: (p) => fecha(p.fechaLocal) },
          { campo: 'dispositivo', titulo: t('sincronizacion.dispositivo') },
          { campo: 'lineas', titulo: t('comun.cantidad'), render: (p) => p.venta.lineas.length },
        ]}
      />
      <Stack direction="row" justifyContent="space-between" alignItems="center" mt={3} mb={1}>
        <Typography variant="subtitle1" fontWeight={600}>{t('sincronizacion.operaciones')}</Typography>
        <TextField select size="small" value={estado} onChange={(e) => setEstado(e.target.value)} sx={{ minWidth: 180 }}>
          <MenuItem value="">{t('comun.todos')}</MenuItem>
          {['APLICADA', 'CONFLICTO', 'RESUELTA', 'DESCARTADA'].map((e) => <MenuItem key={e} value={e}>{t(`sincronizacion.${e}`)}</MenuItem>)}
        </TextField>
      </Stack>
      <TablaDatos
        filas={operaciones}
        columnas={[
          { campo: 'fechaLocal', titulo: t('comun.fecha'), render: (o) => fecha(o.fechaLocal) },
          { campo: 'dispositivo', titulo: t('sincronizacion.dispositivo') },
          { campo: 'usuario', titulo: t('comun.usuario') },
          { campo: 'estado', titulo: t('comun.estado'), render: (o) => <Chip size="small" label={t(`sincronizacion.${o.estado}`)} color={o.estado === 'CONFLICTO' ? 'error' : o.estado === 'APLICADA' ? 'success' : 'default'} /> },
          { campo: 'numeroFactura', titulo: t('venta.factura') },
          { campo: 'mensaje', titulo: t('sincronizacion.mensaje') },
          {
            campo: 'acc', titulo: '', align: 'right',
            render: (o) => resolver && o.estado === 'CONFLICTO' && (
              <Stack direction="row" spacing={1} justifyContent="flex-end">
                <Button size="small" onClick={accion(() => sincronizacionApi.reintentar(o.id))}>{t('comun.reintentar')}</Button>
                <Button size="small" color="error" onClick={accion(() => sincronizacionApi.descartar(o.id))}>{t('comun.descartar')}</Button>
              </Stack>
            ),
          },
        ]}
      />
    </>
  );
}
