/**
 * @file pages/ventas/PuntoVentaPage.jsx
 * @description Punto de venta: lector de código de barras, ventas por unidad o fracción, reservas, garantías,
 *              facturación en línea y registro sin internet con sincronización posterior (REQ-F-021..024, 038..040).
 * @created 21/10/2026
 * @updated 21/10/2026
 * @author Equipo SIGIF
 */
import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import {
  Alert, Button, Card, CardContent, Dialog, DialogActions, DialogContent, DialogTitle, Grid, IconButton, MenuItem,
  Stack, Table, TableBody, TableCell, TableHead, TableRow, TextField, Typography,
} from '@mui/material';
import DeleteIcon from '@mui/icons-material/Delete';
import { useTranslation } from 'react-i18next';
import { productosApi, ventasApi } from '../../api/servicios';
import { esErrorDeRed } from '../../api/cliente';
import { useNotificacion } from '../../context/NotificacionContext';
import EncabezadoPagina from '../../components/EncabezadoPagina';
import SelectorTercero from '../../components/SelectorTercero';
import BuscadorProducto from '../../components/BuscadorProducto';
import { guardarVentaPendiente } from '../../offline/db';
import { idDispositivo } from '../../offline/sincronizador';
import { cantidad as fmtCantidad, moneda, redondear } from '../../utils/formato';

const DECIMALES_MAX = 3;

/**
 * Calcula los valores de una línea igual que el Back-End (precio por unidad base × cantidad base).
 * @param {object} l línea del carrito
 * @returns {{base: number, subtotal: number, descuento: number, iva: number, total: number}} valores
 */
function calcular(l) {
  const base = Number(l.cantidad || 0) * Number(l.factor || 1);
  const subtotal = redondear(l.precioBase * base);
  const descuento = redondear(subtotal * Number(l.descuento || 0) / 100);
  const iva = redondear((subtotal - descuento) * l.porcentajeIva / 100);
  return { base, subtotal, descuento, iva, total: redondear(subtotal - descuento + iva) };
}

/**
 * @param {object} l línea
 * @param {Function} t traducción
 * @returns {string|null} error de la línea o null
 */
function validarLinea(l, t) {
  const c = Number(l.cantidad);
  if (!(c > 0)) return t('validacion.positivo');
  const decimales = (String(l.cantidad).split('.')[1] || '').length;
  if (decimales > DECIMALES_MAX) return t('validacion.decimales', { n: DECIMALES_MAX });
  if (!l.permiteFraccion && !Number.isInteger(c * Number(l.factor || 1))) return t('validacion.entero');
  return null;
}

/**
 * @returns {JSX.Element} punto de venta
 */
export default function PuntoVentaPage() {
  const { t } = useTranslation();
  const { notificar, notificarError } = useNotificacion();
  const navigate = useNavigate();
  const [cliente, setCliente] = useState(null);
  const [lineas, setLineas] = useState([]);
  const [enviando, setEnviando] = useState(false);
  const [reciboOffline, setReciboOffline] = useState(null);

  const agregar = async (p) => {
    let conversiones = p.conversiones;
    if (!conversiones && navigator.onLine) {
      try {
        conversiones = (await productosApi.conversiones(p.id)).filter((c) => c.usoVenta)
          .map((c) => ({ unidadId: c.unidadId, codigo: c.unidadCodigo, factor: c.factor }));
      } catch {
        conversiones = [];
      }
    }
    setLineas((ls) => {
      const existente = ls.findIndex((l) => l.productoId === p.id && !l.unidadId && !l.reservaId);
      if (existente >= 0) {
        return ls.map((l, i) => (i === existente ? { ...l, cantidad: Number(l.cantidad) + 1 } : l));
      }
      return [...ls, {
        clave: crypto.randomUUID(), productoId: p.id, nombre: p.nombre, codigoBarras: p.codigoBarras, unidadBase: p.unidadBase,
        conversiones: conversiones || [], unidadId: '', factor: 1, cantidad: 1, precioBase: p.precioVenta,
        porcentajeIva: p.porcentajeIva, descuento: 0, reservaId: '', permiteFraccion: p.permiteFraccion,
        disponible: p.disponible, tieneGarantia: p.tieneGarantia, mesesGarantia: p.mesesGarantia,
      }];
    });
  };

  const actualizar = (clave, cambios) => setLineas((ls) => ls.map((l) => (l.clave === clave ? { ...l, ...cambios } : l)));

  const totales = lineas.map(calcular).reduce((a, c) => ({
    subtotal: a.subtotal + c.subtotal, descuento: a.descuento + c.descuento, iva: a.iva + c.iva, total: a.total + c.total,
  }), { subtotal: 0, descuento: 0, iva: 0, total: 0 });
  const errores = lineas.map((l) => validarLinea(l, t));

  const solicitud = () => ({
    clienteId: cliente?.id ?? null,
    observaciones: null,
    lineas: lineas.map((l) => ({
      productoId: l.productoId,
      unidadId: l.unidadId || null,
      cantidad: Number(l.cantidad),
      porcentajeDescuento: Number(l.descuento || 0),
      reservaId: l.reservaId ? Number(l.reservaId) : null,
      numeroSerie: l.numeroSerie || null,
    })),
  });

  const guardarOffline = async (venta) => {
    const pendiente = { idLocal: crypto.randomUUID(), fechaLocal: new Date().toISOString(), dispositivo: idDispositivo(), venta };
    await guardarVentaPendiente(pendiente, lineas.map((l) => ({ productoId: l.productoId, cantidadBase: calcular(l).base })));
    window.dispatchEvent(new Event('sigif:pendientes-cambiaron'));
    setReciboOffline({ ...pendiente, lineas, totales, cliente });
    setLineas([]);
    notificar(t('venta.guardadaOffline'), 'warning');
  };

  const facturar = async () => {
    if (!lineas.length || errores.some(Boolean)) {
      notificar(t('validacion.lineas'), 'error');
      return;
    }
    setEnviando(true);
    const venta = solicitud();
    try {
      if (!navigator.onLine) {
        await guardarOffline(venta);
        return;
      }
      const factura = await ventasApi.registrar(venta);
      notificar(t('venta.generada', { numero: factura.numero }));
      setLineas([]);
      navigate(`/imprimir/factura/${factura.id}`);
    } catch (e) {
      if (esErrorDeRed(e)) {
        await guardarOffline(venta);
      } else {
        notificarError(e);
      }
    } finally {
      setEnviando(false);
    }
  };

  return (
    <>
      <EncabezadoPagina titulo={t('venta.titulo')} />
      <Grid container spacing={2}>
        <Grid item xs={12} lg={8}>
          <Card variant="outlined">
            <CardContent>
              <Stack spacing={2}>
                <BuscadorProducto onSeleccionar={agregar} />
                {!lineas.length && <Alert severity="info">{t('venta.carritoVacio')}</Alert>}
                {lineas.length > 0 && (
                  <Table size="small" data-testid="carrito">
                    <TableHead>
                      <TableRow>
                        <TableCell>{t('reserva.producto')}</TableCell>
                        <TableCell>{t('comun.unidad')}</TableCell>
                        <TableCell>{t('comun.cantidad')}</TableCell>
                        <TableCell align="right">{t('venta.precio')}</TableCell>
                        <TableCell>% {t('comun.descuento')}</TableCell>
                        <TableCell>{t('venta.reserva')}</TableCell>
                        <TableCell align="right">{t('comun.total')}</TableCell>
                        <TableCell />
                      </TableRow>
                    </TableHead>
                    <TableBody>
                      {lineas.map((l, i) => {
                        const c = calcular(l);
                        return (
                          <TableRow key={l.clave}>
                            <TableCell>
                              <Typography variant="body2" fontWeight={600}>{l.nombre}</Typography>
                              <Typography variant="caption" color={c.base > l.disponible ? 'error' : 'text.secondary'}>
                                {t('venta.disponibleLocal')}: {fmtCantidad(l.disponible)} {l.unidadBase}
                                {l.tieneGarantia ? ` · ${t('venta.garantia')}: ${l.mesesGarantia}m` : ''}
                              </Typography>
                            </TableCell>
                            <TableCell>
                              <TextField select size="small" value={l.unidadId} sx={{ minWidth: 90 }}
                                onChange={(e) => {
                                  const conv = l.conversiones.find((x) => x.unidadId === e.target.value);
                                  actualizar(l.clave, { unidadId: e.target.value, factor: conv ? Number(conv.factor) : 1 });
                                }}>
                                <MenuItem value="">{l.unidadBase}</MenuItem>
                                {l.conversiones.map((x) => <MenuItem key={x.unidadId} value={x.unidadId}>{x.codigo}</MenuItem>)}
                              </TextField>
                            </TableCell>
                            <TableCell>
                              <TextField size="small" type="number" value={l.cantidad} sx={{ width: 100 }}
                                inputProps={{ step: l.permiteFraccion ? '0.001' : '1', min: 0, 'data-testid': `cantidad-${i}` }}
                                error={Boolean(errores[i])} helperText={errores[i]}
                                onChange={(e) => actualizar(l.clave, { cantidad: e.target.value })} />
                            </TableCell>
                            <TableCell align="right">{moneda(l.precioBase * l.factor)}</TableCell>
                            <TableCell>
                              <TextField size="small" type="number" value={l.descuento} sx={{ width: 70 }}
                                onChange={(e) => actualizar(l.clave, { descuento: e.target.value })} />
                            </TableCell>
                            <TableCell>
                              <TextField size="small" type="number" value={l.reservaId} sx={{ width: 80 }} placeholder="#"
                                onChange={(e) => actualizar(l.clave, { reservaId: e.target.value })} />
                            </TableCell>
                            <TableCell align="right">{moneda(c.total)}</TableCell>
                            <TableCell>
                              <IconButton size="small" onClick={() => setLineas((ls) => ls.filter((x) => x.clave !== l.clave))}><DeleteIcon fontSize="small" /></IconButton>
                            </TableCell>
                          </TableRow>
                        );
                      })}
                    </TableBody>
                  </Table>
                )}
              </Stack>
            </CardContent>
          </Card>
        </Grid>
        <Grid item xs={12} lg={4}>
          <Card variant="outlined">
            <CardContent>
              <Stack spacing={2}>
                <SelectorTercero rol="CLIENTE" label={`${t('venta.cliente')} (${t('venta.consumidorFinal')})`} valor={cliente} onChange={setCliente} testId="selector-cliente" />
                <Stack direction="row" justifyContent="space-between"><span>{t('comun.subtotal')}</span><span>{moneda(totales.subtotal)}</span></Stack>
                <Stack direction="row" justifyContent="space-between"><span>{t('comun.descuento')}</span><span>-{moneda(totales.descuento)}</span></Stack>
                <Stack direction="row" justifyContent="space-between"><span>{t('comun.iva')}</span><span>{moneda(totales.iva)}</span></Stack>
                <Stack direction="row" justifyContent="space-between">
                  <Typography variant="h5">{t('comun.total')}</Typography>
                  <Typography variant="h5" fontWeight={700} data-testid="total-venta">{moneda(totales.total)}</Typography>
                </Stack>
                <Button variant="contained" size="large" onClick={facturar} disabled={enviando || !lineas.length} data-testid="facturar">
                  {t('venta.registrar')}
                </Button>
              </Stack>
            </CardContent>
          </Card>
        </Grid>
      </Grid>

      <Dialog open={Boolean(reciboOffline)} onClose={() => setReciboOffline(null)} maxWidth="xs" fullWidth>
        <DialogTitle>{t('venta.factura')} OFF-{reciboOffline?.idLocal.slice(0, 8)}</DialogTitle>
        <DialogContent dividers className="documento-imprimible">
          <Alert severity="warning" sx={{ mb: 1 }} data-testid="venta-offline">{t('venta.guardadaOffline')}</Alert>
          <Typography variant="body2">{t('venta.cliente')}: {reciboOffline?.cliente?.nombre ?? t('venta.consumidorFinal')}</Typography>
          {reciboOffline?.lineas.map((l) => (
            <Stack key={l.clave} direction="row" justifyContent="space-between">
              <Typography variant="body2">{l.cantidad} × {l.nombre}</Typography>
              <Typography variant="body2">{moneda(calcular(l).total)}</Typography>
            </Stack>
          ))}
          <Typography variant="h6" align="right" mt={1}>{t('comun.total')}: {moneda(reciboOffline?.totales.total)}</Typography>
        </DialogContent>
        <DialogActions>
          <Button onClick={() => window.print()}>{t('comun.imprimir')}</Button>
          <Button variant="contained" onClick={() => setReciboOffline(null)}>{t('comun.cerrar')}</Button>
        </DialogActions>
      </Dialog>
    </>
  );
}
