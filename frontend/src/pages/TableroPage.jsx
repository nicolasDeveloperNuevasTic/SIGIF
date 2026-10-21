/**
 * @file pages/TableroPage.jsx
 * @description Tablero con indicadores de inventario, ventas, alertas y ventas diarias.
 * @created 21/10/2026
 * @updated 21/10/2026
 * @author Equipo SIGIF
 */
import { useEffect, useState } from 'react';
import { Box, Card, CardContent, Grid, LinearProgress, Stack, Tooltip, Typography } from '@mui/material';
import { useTranslation } from 'react-i18next';
import { reportesApi } from '../api/servicios';
import { useNotificacion } from '../context/NotificacionContext';
import EncabezadoPagina from '../components/EncabezadoPagina';
import { moneda } from '../utils/formato';

/**
 * Tarjeta de indicador.
 * @param {{titulo: string, valor: string|number, color?: string}} props propiedades
 * @returns {JSX.Element} tarjeta
 */
function Indicador({ titulo, valor, color }) {
  return (
    <Card variant="outlined" sx={{ height: '100%' }}>
      <CardContent>
        <Typography variant="body2" color="text.secondary">{titulo}</Typography>
        <Typography variant="h5" fontWeight={700} color={color} sx={{ fontVariantNumeric: 'tabular-nums' }}>{valor}</Typography>
      </CardContent>
    </Card>
  );
}

/**
 * @returns {JSX.Element} tablero
 */
export default function TableroPage() {
  const { t } = useTranslation();
  const { notificarError } = useNotificacion();
  const [r, setR] = useState(null);

  useEffect(() => {
    reportesApi.resumen().then(setR).catch(notificarError);
  }, [notificarError]);

  if (!r) return <LinearProgress />;
  const maximo = Math.max(1, ...r.ventasUltimosDias.map((d) => Number(d.total)));

  return (
    <>
      <EncabezadoPagina titulo={t('menu.tablero')} />
      <Grid container spacing={2}>
        <Grid item xs={12} sm={6} md={3}><Indicador titulo={t('tablero.ventasHoy')} valor={moneda(r.ventasHoy)} /></Grid>
        <Grid item xs={12} sm={6} md={3}><Indicador titulo={t('tablero.facturasHoy')} valor={r.facturasHoy} /></Grid>
        <Grid item xs={12} sm={6} md={3}><Indicador titulo={t('tablero.ventasMes')} valor={moneda(r.ventasMes)} /></Grid>
        <Grid item xs={12} sm={6} md={3}><Indicador titulo={t('tablero.valorInventario')} valor={moneda(r.valorInventario)} /></Grid>
        <Grid item xs={12} sm={6} md={3}><Indicador titulo={t('tablero.productosActivos')} valor={r.productosActivos} /></Grid>
        <Grid item xs={12} sm={6} md={3}>
          <Indicador titulo={t('tablero.bajoStock')} valor={r.bajoStockMinimo} color={r.bajoStockMinimo ? 'warning.main' : undefined} />
        </Grid>
        <Grid item xs={12} sm={6} md={3}>
          <Indicador titulo={t('tablero.porVencer')} valor={r.lotesPorVencer} color={r.lotesPorVencer ? 'error.main' : undefined} />
        </Grid>
        <Grid item xs={12} sm={6} md={3}>
          <Indicador titulo={t('tablero.conflictos')} valor={r.conflictosSincronizacion} color={r.conflictosSincronizacion ? 'error.main' : undefined} />
        </Grid>
        <Grid item xs={12} sm={6} md={3}><Indicador titulo={t('tablero.reservasActivas')} valor={r.reservasActivas} /></Grid>
        <Grid item xs={12} sm={6} md={3}><Indicador titulo={t('tablero.ultimoCierre')} valor={r.ultimoCierre ?? '—'} /></Grid>
        <Grid item xs={12}>
          <Card variant="outlined">
            <CardContent>
              <Typography variant="subtitle1" fontWeight={600} mb={2}>{t('tablero.ventasDiarias')}</Typography>
              <Stack direction="row" alignItems="flex-end" spacing={1} sx={{ height: 160 }}>
                {r.ventasUltimosDias.map((d) => (
                  <Tooltip key={d.dia} title={`${d.dia}: ${moneda(d.total)} (${d.facturas})`}>
                    <Box sx={{ flex: 1, display: 'flex', flexDirection: 'column', alignItems: 'center', height: '100%', justifyContent: 'flex-end' }}>
                      <Box sx={{ width: '70%', maxWidth: 32, bgcolor: 'primary.main', borderRadius: '4px 4px 0 0', height: `${(Number(d.total) / maximo) * 100}%`, minHeight: 2 }} />
                      <Typography variant="caption" color="text.secondary">{d.dia.slice(8)}</Typography>
                    </Box>
                  </Tooltip>
                ))}
                {!r.ventasUltimosDias.length && <Typography color="text.secondary">{t('comun.sinDatos')}</Typography>}
              </Stack>
            </CardContent>
          </Card>
        </Grid>
      </Grid>
    </>
  );
}
