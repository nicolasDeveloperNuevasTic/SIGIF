/**
 * @file pages/compras/NuevaCompraPage.jsx
 * @description Registro de entradas por compra con lector de código de barras y generación del Documento de Ingreso.
 * @created 21/10/2026
 * @updated 21/10/2026
 * @author Equipo SIGIF
 */
import { useMemo, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import {
  Alert, Button, Card, CardContent, Grid, IconButton, MenuItem, Stack, Table, TableBody, TableCell, TableHead, TableRow, TextField, Typography,
} from '@mui/material';
import DeleteIcon from '@mui/icons-material/Delete';
import { FieldArray, Form, Formik, getIn } from 'formik';
import { useTranslation } from 'react-i18next';
import { comprasApi, productosApi } from '../../api/servicios';
import { useNotificacion } from '../../context/NotificacionContext';
import EncabezadoPagina from '../../components/EncabezadoPagina';
import SelectorTercero from '../../components/SelectorTercero';
import BuscadorProducto from '../../components/BuscadorProducto';
import { compraEsquema } from '../../validation/esquemas';
import { moneda, redondear } from '../../utils/formato';

/**
 * @returns {JSX.Element} formulario de compra
 */
export default function NuevaCompraPage() {
  const { t } = useTranslation();
  const { notificar, notificarError } = useNotificacion();
  const navigate = useNavigate();
  const esquema = useMemo(() => compraEsquema(t), [t]);
  const [proveedor, setProveedor] = useState(null);

  return (
    <>
      <EncabezadoPagina titulo={t('compra.nueva')} />
      <Formik
        initialValues={{ proveedorId: '', numeroFacturaProveedor: '', observaciones: '', lineas: [] }}
        validationSchema={esquema}
        onSubmit={async (v, { setSubmitting }) => {
          try {
            const doc = await comprasApi.registrar({
              proveedorId: v.proveedorId,
              numeroFacturaProveedor: v.numeroFacturaProveedor || null,
              observaciones: v.observaciones || null,
              lineas: v.lineas.map((l) => ({
                productoId: l.productoId,
                unidadId: l.unidadId || null,
                cantidad: Number(l.cantidad),
                costoUnitario: Number(l.costoUnitario),
                porcentajeIva: Number(l.porcentajeIva || 0),
                codigoLote: l.tieneVencimiento ? l.codigoLote : null,
                fechaVencimiento: l.tieneVencimiento ? l.fechaVencimiento : null,
              })),
            });
            notificar(t('compra.generado', { numero: doc.numero }));
            navigate(`/imprimir/ingreso/${doc.id}`);
          } catch (e) {
            notificarError(e);
          } finally {
            setSubmitting(false);
          }
        }}
      >
        {({ values, errors, touched, setFieldValue, handleChange, isSubmitting, submitCount }) => {
          const subtotal = values.lineas.reduce((s, l) => s + Number(l.cantidad || 0) * Number(l.costoUnitario || 0), 0);
          const iva = values.lineas.reduce((s, l) => s + redondear(Number(l.cantidad || 0) * Number(l.costoUnitario || 0) * Number(l.porcentajeIva || 0) / 100), 0);
          return (
            <Form noValidate>
              <FieldArray name="lineas">
                {({ push, remove }) => (
                  <Card variant="outlined">
                    <CardContent>
                      <Grid container spacing={2} mb={2}>
                        <Grid item xs={12} md={6}>
                          <SelectorTercero rol="PROVEEDOR" label={t('compra.proveedor')} valor={proveedor} testId="selector-proveedor"
                            error={touched.proveedorId || submitCount ? errors.proveedorId : undefined}
                            onChange={(p) => { setProveedor(p); setFieldValue('proveedorId', p?.id ?? ''); }} />
                        </Grid>
                        <Grid item xs={12} md={3}>
                          <TextField name="numeroFacturaProveedor" label={t('compra.facturaProveedor')} size="small" fullWidth
                            value={values.numeroFacturaProveedor} onChange={handleChange} />
                        </Grid>
                        <Grid item xs={12} md={3}>
                          <TextField name="observaciones" label={t('comun.observaciones')} size="small" fullWidth
                            value={values.observaciones} onChange={handleChange} />
                        </Grid>
                        <Grid item xs={12}>
                          <BuscadorProducto onSeleccionar={async (p) => {
                            let conversiones = [];
                            try {
                              conversiones = (await productosApi.conversiones(p.id)).filter((c) => c.usoCompra);
                            } catch { /* sin conversiones */ }
                            push({
                              productoId: p.id, nombre: p.nombre, unidadBase: p.unidadBase, conversiones, unidadId: '',
                              cantidad: 1, costoUnitario: p.precioCompra ?? '', porcentajeIva: 0,
                              tieneVencimiento: p.tieneVencimiento, codigoLote: '', fechaVencimiento: '',
                            });
                          }} />
                        </Grid>
                      </Grid>
                      {typeof errors.lineas === 'string' && submitCount > 0 && <Alert severity="error" sx={{ mb: 1 }}>{errors.lineas}</Alert>}
                      <Table size="small">
                        <TableHead>
                          <TableRow>
                            <TableCell>{t('reserva.producto')}</TableCell>
                            <TableCell>{t('comun.unidad')}</TableCell>
                            <TableCell>{t('comun.cantidad')}</TableCell>
                            <TableCell>{t('compra.costoUnitario')}</TableCell>
                            <TableCell>% {t('comun.iva')}</TableCell>
                            <TableCell>{t('compra.lote')}</TableCell>
                            <TableCell>{t('compra.vence')}</TableCell>
                            <TableCell align="right">{t('comun.subtotal')}</TableCell>
                            <TableCell />
                          </TableRow>
                        </TableHead>
                        <TableBody>
                          {values.lineas.map((l, i) => {
                            const err = (campo) => (submitCount > 0 || getIn(touched, `lineas.${i}.${campo}`)) && getIn(errors, `lineas.${i}.${campo}`);
                            return (
                              <TableRow key={`${l.productoId}-${i}`}>
                                <TableCell>{l.nombre}</TableCell>
                                <TableCell>
                                  <TextField select size="small" value={l.unidadId} onChange={(e) => setFieldValue(`lineas.${i}.unidadId`, e.target.value)} sx={{ minWidth: 110 }}>
                                    <MenuItem value="">{l.unidadBase}</MenuItem>
                                    {l.conversiones.map((c) => <MenuItem key={c.unidadId} value={c.unidadId}>{c.unidadCodigo} (×{Number(c.factor)})</MenuItem>)}
                                  </TextField>
                                </TableCell>
                                <TableCell><TextField size="small" type="number" name={`lineas.${i}.cantidad`} value={l.cantidad} onChange={handleChange} error={Boolean(err('cantidad'))} helperText={err('cantidad')} sx={{ width: 100 }} /></TableCell>
                                <TableCell><TextField size="small" type="number" name={`lineas.${i}.costoUnitario`} value={l.costoUnitario} onChange={handleChange} error={Boolean(err('costoUnitario'))} helperText={err('costoUnitario')} sx={{ width: 130 }} /></TableCell>
                                <TableCell><TextField size="small" type="number" name={`lineas.${i}.porcentajeIva`} value={l.porcentajeIva} onChange={handleChange} sx={{ width: 80 }} /></TableCell>
                                <TableCell>
                                  {l.tieneVencimiento && <TextField size="small" name={`lineas.${i}.codigoLote`} value={l.codigoLote} onChange={handleChange} error={Boolean(err('codigoLote'))} helperText={err('codigoLote')} sx={{ width: 120 }} />}
                                </TableCell>
                                <TableCell>
                                  {l.tieneVencimiento && <TextField size="small" type="date" name={`lineas.${i}.fechaVencimiento`} value={l.fechaVencimiento} onChange={handleChange} error={Boolean(err('fechaVencimiento'))} helperText={err('fechaVencimiento')} />}
                                </TableCell>
                                <TableCell align="right">{moneda(Number(l.cantidad || 0) * Number(l.costoUnitario || 0))}</TableCell>
                                <TableCell><IconButton size="small" onClick={() => remove(i)}><DeleteIcon fontSize="small" /></IconButton></TableCell>
                              </TableRow>
                            );
                          })}
                        </TableBody>
                      </Table>
                      <Stack direction="row" justifyContent="flex-end" spacing={3} mt={2}>
                        <Typography>{t('comun.subtotal')}: <b>{moneda(subtotal)}</b></Typography>
                        <Typography>{t('comun.iva')}: <b>{moneda(iva)}</b></Typography>
                        <Typography variant="h6">{t('comun.total')}: {moneda(subtotal + iva)}</Typography>
                      </Stack>
                      <Stack direction="row" justifyContent="flex-end" spacing={1} mt={2}>
                        <Button onClick={() => navigate('/compras')}>{t('comun.cancelar')}</Button>
                        <Button type="submit" variant="contained" disabled={isSubmitting} data-testid="registrar-compra">{t('compra.registrar')}</Button>
                      </Stack>
                    </CardContent>
                  </Card>
                )}
              </FieldArray>
            </Form>
          );
        }}
      </Formik>
    </>
  );
}
