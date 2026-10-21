/**
 * @file pages/productos/HistorialPreciosDialog.jsx
 * @description Historial cronológico de costos, porcentajes y precios de venta de un producto (REQ-F-010).
 * @created 21/10/2026
 * @updated 21/10/2026
 * @author Equipo SIGIF
 */
import { useEffect, useState } from 'react';
import { Button, Dialog, DialogActions, DialogContent, DialogTitle } from '@mui/material';
import { useTranslation } from 'react-i18next';
import { productosApi } from '../../api/servicios';
import { useNotificacion } from '../../context/NotificacionContext';
import TablaDatos from '../../components/TablaDatos';
import { fecha, moneda } from '../../utils/formato';

/**
 * @param {{producto: object|null, onCerrar: Function}} props producto y cierre
 * @returns {JSX.Element} diálogo
 */
export default function HistorialPreciosDialog({ producto, onCerrar }) {
  const { t } = useTranslation();
  const { notificarError } = useNotificacion();
  const [lista, setLista] = useState([]);

  useEffect(() => {
    if (producto) productosApi.historial(producto.id).then(setLista).catch(notificarError);
  }, [producto, notificarError]);

  return (
    <Dialog open={Boolean(producto)} onClose={onCerrar} maxWidth="lg" fullWidth>
      <DialogTitle>{t('producto.historial')} — {producto?.nombre}</DialogTitle>
      <DialogContent dividers>
        <TablaDatos
          filas={lista}
          columnas={[
            { campo: 'fecha', titulo: t('comun.fecha'), render: (h) => fecha(h.fecha) },
            { campo: 'motivo', titulo: t('producto.motivo') },
            { campo: 'referencia', titulo: t('producto.referencia') },
            { campo: 'pc', titulo: t('producto.precioCompra'), align: 'right', render: (h) => `${moneda(h.precioCompraAnterior)} → ${moneda(h.precioCompraNuevo)}` },
            { campo: 'pct', titulo: '%', align: 'right', render: (h) => `${h.porcentajeAnterior ?? ''} → ${h.porcentajeNuevo}` },
            { campo: 'pv', titulo: t('producto.precioVenta'), align: 'right', render: (h) => `${moneda(h.precioVentaAnterior)} → ${moneda(h.precioVentaNuevo)}` },
            { campo: 'usuario', titulo: t('comun.usuario') },
          ]}
        />
      </DialogContent>
      <DialogActions><Button onClick={onCerrar}>{t('comun.cerrar')}</Button></DialogActions>
    </Dialog>
  );
}
