/**
 * @file pages/DocumentoImprimiblePage.jsx
 * @description Vista imprimible de Factura (salida por venta) y Documento de Ingreso (entrada por compra).
 * @created 21/10/2026
 * @updated 21/10/2026
 * @author Equipo SIGIF
 */
import { useEffect, useState } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import {
  Box, Button, Divider, LinearProgress, Paper, Stack, Table, TableBody, TableCell, TableHead, TableRow, Typography,
} from '@mui/material';
import PrintIcon from '@mui/icons-material/Print';
import { useTranslation } from 'react-i18next';
import { comprasApi, ventasApi } from '../api/servicios';
import { useNotificacion } from '../context/NotificacionContext';
import { cantidad, fecha, moneda } from '../utils/formato';

/**
 * @returns {JSX.Element} documento listo para imprimir
 */
export default function DocumentoImprimiblePage() {
  const { tipo, id } = useParams();
  const { t } = useTranslation();
  const { notificarError } = useNotificacion();
  const navigate = useNavigate();
  const [doc, setDoc] = useState(null);
  const esFactura = tipo === 'factura';

  useEffect(() => {
    (esFactura ? ventasApi.obtener(id) : comprasApi.obtener(id)).then(setDoc).catch(notificarError);
  }, [esFactura, id, notificarError]);

  if (!doc) return <LinearProgress />;
  const tercero = esFactura ? doc.cliente : doc.proveedor;

  return (
    <>
      <Stack direction="row" spacing={1} mb={2} className="no-imprimir">
        <Button variant="contained" startIcon={<PrintIcon />} onClick={() => window.print()} data-testid="imprimir">{t('comun.imprimir')}</Button>
        <Button onClick={() => navigate(esFactura ? '/ventas' : '/compras')}>{t('comun.cerrar')}</Button>
      </Stack>
      <Paper className="documento-imprimible" variant="outlined">
        <Stack direction="row" justifyContent="space-between">
          <Box>
            <Typography variant="h5" fontWeight={700}>{t('app.nombre')}</Typography>
            <Typography variant="body2">{t('app.subtitulo')}</Typography>
          </Box>
          <Box textAlign="right">
            <Typography variant="h6">{esFactura ? t('venta.factura') : t('compra.documento')}</Typography>
            <Typography variant="h5" fontWeight={700} data-testid="numero-documento">{doc.numero}</Typography>
            <Typography variant="body2">{fecha(doc.fecha)}</Typography>
          </Box>
        </Stack>
        <Divider sx={{ my: 2 }} />
        <Typography variant="body2"><b>{esFactura ? t('venta.cliente') : t('compra.proveedor')}:</b> {tercero.nombre} — {tercero.numeroDocumento}</Typography>
        <Typography variant="body2">{tercero.direccion} · {tercero.telefono} · {tercero.correo}</Typography>
        {!esFactura && doc.numeroFacturaProveedor && <Typography variant="body2"><b>{t('compra.facturaProveedor')}:</b> {doc.numeroFacturaProveedor}</Typography>}
        <Table size="small" sx={{ mt: 2 }}>
          <TableHead>
            <TableRow>
              <TableCell>{t('producto.codigoBarras')}</TableCell>
              <TableCell>{t('reserva.producto')}</TableCell>
              <TableCell align="right">{t('comun.cantidad')}</TableCell>
              <TableCell align="right">{esFactura ? t('venta.precio') : t('compra.costoUnitario')}</TableCell>
              {esFactura && <TableCell align="right">{t('comun.descuento')}</TableCell>}
              <TableCell align="right">{t('comun.iva')}</TableCell>
              <TableCell align="right">{t('comun.total')}</TableCell>
            </TableRow>
          </TableHead>
          <TableBody>
            {doc.detalles.map((d, i) => (
              <TableRow key={i}>
                <TableCell>{d.codigoBarras}</TableCell>
                <TableCell>
                  {d.producto}
                  {esFactura && d.fechaFinGarantia && (
                    <Typography variant="caption" display="block">{t('venta.garantia')}: {d.mesesGarantia}m, {t('venta.hasta')} {fecha(d.fechaFinGarantia, false)}{d.numeroSerie ? ` · ${t('venta.serial')} ${d.numeroSerie}` : ''}</Typography>
                  )}
                  {!esFactura && d.codigoLote && <Typography variant="caption" display="block">{t('inventario.lote')} {d.codigoLote} · {fecha(d.fechaVencimiento, false)}</Typography>}
                </TableCell>
                <TableCell align="right">{cantidad(d.cantidad)} {d.unidad}</TableCell>
                <TableCell align="right">{moneda(esFactura ? d.precioUnitario : d.costoUnitario)}</TableCell>
                {esFactura && <TableCell align="right">{moneda(d.valorDescuento)}</TableCell>}
                <TableCell align="right">{moneda(d.valorIva)}</TableCell>
                <TableCell align="right">{moneda(d.total)}</TableCell>
              </TableRow>
            ))}
          </TableBody>
        </Table>
        <Stack alignItems="flex-end" mt={2} spacing={0.5}>
          <Typography>{t('comun.subtotal')}: {moneda(doc.subtotal)}</Typography>
          {esFactura && <Typography>{t('comun.descuento')}: -{moneda(doc.totalDescuento)}</Typography>}
          <Typography>{t('comun.iva')}: {moneda(doc.totalIva)}</Typography>
          <Typography variant="h6" fontWeight={700}>{t('comun.total')}: {moneda(doc.total)}</Typography>
        </Stack>
        <Divider sx={{ my: 2 }} />
        <Typography variant="caption" color="text.secondary">
          {esFactura ? t('venta.origen') : t('compra.responsable')}: {esFactura ? `${doc.origen} · ` : ''}{doc.usuario}
          {doc.observaciones ? ` · ${doc.observaciones}` : ''}
        </Typography>
      </Paper>
    </>
  );
}
