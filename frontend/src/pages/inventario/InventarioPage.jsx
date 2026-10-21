/**
 * @file pages/inventario/InventarioPage.jsx
 * @description Existencias por estado en la bodega única, kardex, lotes, alertas y cambios de estado auditados.
 * @created 21/10/2026
 * @updated 21/10/2026
 * @author Equipo SIGIF
 */
import { useCallback, useEffect, useMemo, useState } from 'react';
import {
  Button, Chip, Dialog, DialogActions, DialogContent, DialogTitle, FormControlLabel, Grid, IconButton, MenuItem,
  Stack, Switch, Tab, Tabs, TextField, Tooltip, Typography,
} from '@mui/material';
import SwapIcon from '@mui/icons-material/SwapVert';
import ListIcon from '@mui/icons-material/ListAlt';
import EventIcon from '@mui/icons-material/Event';
import { Form, Formik } from 'formik';
import { useTranslation } from 'react-i18next';
import { inventarioApi } from '../../api/servicios';
import { useNotificacion } from '../../context/NotificacionContext';
import { useAuth } from '../../context/AuthContext';
import EncabezadoPagina from '../../components/EncabezadoPagina';
import TablaDatos from '../../components/TablaDatos';
import CampoTexto from '../../components/CampoTexto';
import { transicionEsquema } from '../../validation/esquemas';
import { cantidad, fecha, moneda } from '../../utils/formato';

const ESTADOS = ['DISPONIBLE', 'DANADA'];

/**
 * @returns {JSX.Element} pantalla de inventario
 */
export default function InventarioPage() {
  const { t } = useTranslation();
  const { notificar, notificarError } = useNotificacion();
  const { tienePermiso } = useAuth();
  const [pestana, setPestana] = useState('saldos');
  const [q, setQ] = useState('');
  const [soloConStock, setSoloConStock] = useState(false);
  const [pagina, setPagina] = useState(0);
  const [tamano, setTamano] = useState(20);
  const [saldos, setSaldos] = useState({ contenido: [], totalElementos: 0 });
  const [kardex, setKardex] = useState({ contenido: [], totalElementos: 0 });
  const [filtroKardex, setFiltroKardex] = useState({ productoId: null, nombre: '', tipo: '' });
  const [alertas, setAlertas] = useState({ vencimientos: [], stock: [] });
  const [lotes, setLotes] = useState(null);
  const [transicion, setTransicion] = useState(null);
  const [cargando, setCargando] = useState(false);
  const esquema = useMemo(() => transicionEsquema(t), [t]);

  const cargarSaldos = useCallback(async () => {
    setCargando(true);
    try {
      setSaldos(await inventarioApi.saldos({ q: q || undefined, soloConStock, pagina, tamano }));
    } catch (e) {
      notificarError(e);
    } finally {
      setCargando(false);
    }
  }, [q, soloConStock, pagina, tamano, notificarError]);

  const cargarKardex = useCallback(async () => {
    setCargando(true);
    try {
      setKardex(await inventarioApi.movimientos({
        productoId: filtroKardex.productoId || undefined, tipo: filtroKardex.tipo || undefined, pagina, tamano,
      }));
    } catch (e) {
      notificarError(e);
    } finally {
      setCargando(false);
    }
  }, [filtroKardex, pagina, tamano, notificarError]);

  useEffect(() => {
    if (pestana === 'saldos') {
      const tm = setTimeout(cargarSaldos, 300);
      return () => clearTimeout(tm);
    }
    if (pestana === 'kardex') cargarKardex();
    if (pestana === 'alertas') {
      Promise.all([inventarioApi.vencimientos(30), inventarioApi.stockMinimo()])
        .then(([vencimientos, stock]) => setAlertas({ vencimientos, stock }))
        .catch(notificarError);
    }
    return undefined;
  }, [pestana, cargarSaldos, cargarKardex, notificarError]);

  const paginacion = (total) => ({ pagina, tamano, total, onPagina: setPagina, onTamano: (n) => { setTamano(n); setPagina(0); } });
  const estadoChip = (e) => e && <Chip size="small" label={t(`inventario.${e}`)} color={e === 'DISPONIBLE' ? 'success' : e === 'RESERVADA' ? 'info' : 'error'} variant="outlined" />;

  const columnasLotes = [
    { campo: 'codigoLote', titulo: t('inventario.lote') },
    { campo: 'producto', titulo: t('reserva.producto') },
    { campo: 'fechaVencimiento', titulo: t('inventario.fechaVencimiento'), render: (l) => fecha(l.fechaVencimiento, false) },
    { campo: 'cantidadActual', titulo: t('comun.cantidad'), align: 'right', render: (l) => cantidad(l.cantidadActual) },
    {
      campo: 'diasParaVencer', titulo: t('inventario.diasParaVencer'), align: 'right',
      render: (l) => <Chip size="small" label={l.vencido ? t('inventario.vencido') : l.diasParaVencer} color={l.vencido ? 'error' : l.diasParaVencer <= 30 ? 'warning' : 'default'} />,
    },
  ];

  return (
    <>
      <EncabezadoPagina titulo={t('inventario.titulo')} />
      <Tabs value={pestana} onChange={(_, v) => { setPestana(v); setPagina(0); }} sx={{ mb: 2 }}>
        <Tab value="saldos" label={t('inventario.saldos')} />
        <Tab value="kardex" label={t('inventario.movimientos')} />
        <Tab value="alertas" label={t('inventario.alertas')} />
      </Tabs>

      {pestana === 'saldos' && (
        <>
          <Stack direction={{ xs: 'column', sm: 'row' }} spacing={2} mb={2}>
            <TextField size="small" label={t('comun.buscar')} value={q} onChange={(e) => { setQ(e.target.value); setPagina(0); }} sx={{ flex: 1 }} />
            <FormControlLabel control={<Switch checked={soloConStock} onChange={(e) => setSoloConStock(e.target.checked)} />} label={t('inventario.soloConStock')} />
          </Stack>
          <TablaDatos
            cargando={cargando}
            filas={saldos.contenido}
            clave={(s) => s.productoId}
            paginacion={paginacion(saldos.totalElementos)}
            columnas={[
              { campo: 'codigoBarras', titulo: t('producto.codigoBarras') },
              { campo: 'nombre', titulo: t('producto.nombre') },
              { campo: 'unidadBase', titulo: t('comun.unidad') },
              { campo: 'disponible', titulo: t('inventario.DISPONIBLE'), align: 'right', render: (s) => <b>{cantidad(s.disponible)}</b> },
              { campo: 'reservada', titulo: t('inventario.RESERVADA'), align: 'right', render: (s) => cantidad(s.reservada) },
              { campo: 'danada', titulo: t('inventario.DANADA'), align: 'right', render: (s) => cantidad(s.danada) },
              { campo: 'total', titulo: t('comun.total'), align: 'right', render: (s) => cantidad(s.total) },
              { campo: 'costoPromedio', titulo: t('inventario.costo'), align: 'right', render: (s) => moneda(s.costoPromedio) },
              { campo: 'valor', titulo: t('inventario.valor'), align: 'right', render: (s) => moneda(s.valor) },
              {
                campo: 'acc', titulo: t('comun.acciones'), align: 'right',
                render: (s) => (
                  <Stack direction="row" justifyContent="flex-end">
                    {tienePermiso('INVENTARIO_TRANSICION') && (
                      <Tooltip title={t('inventario.transicion')}><IconButton size="small" onClick={() => setTransicion(s)}><SwapIcon fontSize="small" /></IconButton></Tooltip>
                    )}
                    <Tooltip title={t('inventario.movimientos')}>
                      <IconButton size="small" onClick={() => { setFiltroKardex({ productoId: s.productoId, nombre: s.nombre, tipo: '' }); setPagina(0); setPestana('kardex'); }}>
                        <ListIcon fontSize="small" />
                      </IconButton>
                    </Tooltip>
                    <Tooltip title={t('inventario.lotes')}>
                      <IconButton size="small" onClick={() => inventarioApi.lotes(s.productoId).then((l) => setLotes({ nombre: s.nombre, lista: l })).catch(notificarError)}>
                        <EventIcon fontSize="small" />
                      </IconButton>
                    </Tooltip>
                  </Stack>
                ),
              },
            ]}
          />
        </>
      )}

      {pestana === 'kardex' && (
        <>
          <Stack direction={{ xs: 'column', sm: 'row' }} spacing={2} mb={2} alignItems="center">
            {filtroKardex.productoId && (
              <Chip label={filtroKardex.nombre} onDelete={() => setFiltroKardex({ ...filtroKardex, productoId: null, nombre: '' })} />
            )}
            <TextField select size="small" label={t('inventario.tipo')} value={filtroKardex.tipo} sx={{ minWidth: 220 }}
              onChange={(e) => { setFiltroKardex({ ...filtroKardex, tipo: e.target.value }); setPagina(0); }}>
              <MenuItem value="">{t('comun.todos')}</MenuItem>
              {['ENTRADA_COMPRA', 'SALIDA_VENTA', 'CAMBIO_ESTADO'].map((x) => <MenuItem key={x} value={x}>{t(`inventario.${x}`)}</MenuItem>)}
            </TextField>
          </Stack>
          <TablaDatos
            cargando={cargando}
            filas={kardex.contenido}
            paginacion={paginacion(kardex.totalElementos)}
            columnas={[
              { campo: 'fecha', titulo: t('comun.fecha'), render: (m) => fecha(m.fecha) },
              { campo: 'tipo', titulo: t('inventario.tipo'), render: (m) => t(`inventario.${m.tipo}`) },
              { campo: 'producto', titulo: t('reserva.producto') },
              { campo: 'cantidad', titulo: t('comun.cantidad'), align: 'right', render: (m) => cantidad(m.cantidad) },
              { campo: 'estadoOrigen', titulo: t('inventario.origen'), render: (m) => estadoChip(m.estadoOrigen) },
              { campo: 'estadoDestino', titulo: t('inventario.destino'), render: (m) => estadoChip(m.estadoDestino) },
              { campo: 'documento', titulo: t('inventario.documento') },
              { campo: 'lote', titulo: t('inventario.lote') },
              { campo: 'motivo', titulo: t('inventario.motivo') },
              { campo: 'usuario', titulo: t('comun.usuario') },
            ]}
          />
        </>
      )}

      {pestana === 'alertas' && (
        <Grid container spacing={2}>
          <Grid item xs={12} lg={7}>
            <Typography variant="subtitle1" fontWeight={600} mb={1}>{t('inventario.vencimientos')}</Typography>
            <TablaDatos filas={alertas.vencimientos} columnas={columnasLotes} />
          </Grid>
          <Grid item xs={12} lg={5}>
            <Typography variant="subtitle1" fontWeight={600} mb={1}>{t('inventario.stockMinimo')}</Typography>
            <TablaDatos
              filas={alertas.stock}
              clave={(s) => s.productoId}
              columnas={[
                { campo: 'nombre', titulo: t('producto.nombre') },
                { campo: 'disponible', titulo: t('inventario.DISPONIBLE'), align: 'right', render: (s) => cantidad(s.disponible) },
                { campo: 'stockMinimo', titulo: t('producto.stockMinimo'), align: 'right', render: (s) => cantidad(s.stockMinimo) },
              ]}
            />
          </Grid>
        </Grid>
      )}

      <Dialog open={Boolean(lotes)} onClose={() => setLotes(null)} maxWidth="md" fullWidth>
        <DialogTitle>{t('inventario.lotes')} — {lotes?.nombre}</DialogTitle>
        <DialogContent dividers><TablaDatos filas={lotes?.lista ?? []} columnas={columnasLotes} /></DialogContent>
        <DialogActions><Button onClick={() => setLotes(null)}>{t('comun.cerrar')}</Button></DialogActions>
      </Dialog>

      <Dialog open={Boolean(transicion)} onClose={() => setTransicion(null)} maxWidth="xs" fullWidth>
        <Formik
          enableReinitialize
          initialValues={{ cantidad: '', estadoOrigen: 'DISPONIBLE', estadoDestino: 'DANADA', motivo: '' }}
          validationSchema={esquema}
          onSubmit={async (v, { setSubmitting }) => {
            try {
              await inventarioApi.transicion({ ...v, cantidad: Number(v.cantidad), productoId: transicion.productoId });
              notificar(t('comun.exito'));
              setTransicion(null);
              cargarSaldos();
            } catch (e) {
              notificarError(e);
            } finally {
              setSubmitting(false);
            }
          }}
        >
          {({ isSubmitting }) => (
            <Form noValidate>
              <DialogTitle>{t('inventario.transicion')} — {transicion?.nombre}</DialogTitle>
              <DialogContent dividers>
                <Stack spacing={2}>
                  <Typography variant="body2">
                    {t('inventario.DISPONIBLE')}: {cantidad(transicion?.disponible)} · {t('inventario.DANADA')}: {cantidad(transicion?.danada)}
                  </Typography>
                  <CampoTexto name="estadoOrigen" label={t('inventario.origen')} opciones={ESTADOS.map((e) => ({ value: e, label: t(`inventario.${e}`) }))} />
                  <CampoTexto name="estadoDestino" label={t('inventario.destino')} opciones={ESTADOS.map((e) => ({ value: e, label: t(`inventario.${e}`) }))} />
                  <CampoTexto name="cantidad" label={t('comun.cantidad')} type="number" />
                  <CampoTexto name="motivo" label={t('inventario.motivo')} multiline minRows={2} />
                </Stack>
              </DialogContent>
              <DialogActions>
                <Button onClick={() => setTransicion(null)}>{t('comun.cancelar')}</Button>
                <Button type="submit" variant="contained" disabled={isSubmitting}>{t('comun.guardar')}</Button>
              </DialogActions>
            </Form>
          )}
        </Formik>
      </Dialog>
    </>
  );
}
