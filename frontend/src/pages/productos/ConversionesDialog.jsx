/**
 * @file pages/productos/ConversionesDialog.jsx
 * @description Gestión de factores de conversión entre unidades alternas y la unidad base (REQ-F-006).
 * @created 21/10/2026
 * @updated 21/10/2026
 * @author Equipo SIGIF
 */
import { useCallback, useEffect, useState } from 'react';
import {
  Button, Checkbox, Dialog, DialogActions, DialogContent, DialogTitle, FormControlLabel, IconButton, MenuItem, Stack, TextField,
} from '@mui/material';
import DeleteIcon from '@mui/icons-material/Delete';
import { useTranslation } from 'react-i18next';
import { productosApi } from '../../api/servicios';
import { useNotificacion } from '../../context/NotificacionContext';
import { useAuth } from '../../context/AuthContext';
import TablaDatos from '../../components/TablaDatos';

/**
 * @param {{producto: object|null, unidades: object[], onCerrar: Function}} props producto, unidades disponibles y cierre
 * @returns {JSX.Element} diálogo
 */
export default function ConversionesDialog({ producto, unidades, onCerrar }) {
  const { t } = useTranslation();
  const { notificar, notificarError } = useNotificacion();
  const { tienePermiso } = useAuth();
  const [lista, setLista] = useState([]);
  const [nueva, setNueva] = useState({ unidadId: '', factor: '', usoCompra: true, usoVenta: true });

  const cargar = useCallback(() => {
    if (producto) productosApi.conversiones(producto.id).then(setLista).catch(notificarError);
  }, [producto, notificarError]);

  useEffect(cargar, [cargar]);

  const agregar = async () => {
    try {
      await productosApi.agregarConversion(producto.id, { ...nueva, factor: Number(nueva.factor) });
      setNueva({ unidadId: '', factor: '', usoCompra: true, usoVenta: true });
      notificar(t('comun.exito'));
      cargar();
    } catch (e) {
      notificarError(e);
    }
  };

  const puedeEscribir = tienePermiso('CATALOGO_ESCRIBIR');

  return (
    <Dialog open={Boolean(producto)} onClose={onCerrar} maxWidth="sm" fullWidth>
      <DialogTitle>{t('producto.conversiones')} — {producto?.nombre} ({producto?.unidadBase})</DialogTitle>
      <DialogContent dividers>
        <TablaDatos
          filas={lista}
          columnas={[
            { campo: 'unidadCodigo', titulo: t('comun.unidad'), render: (c) => `${c.unidadCodigo} - ${c.unidadNombre}` },
            { campo: 'factor', titulo: t('producto.factor'), align: 'right', render: (c) => `1 = ${Number(c.factor)} ${producto?.unidadBase}` },
            { campo: 'uso', titulo: '', render: (c) => [c.usoCompra && t('producto.usoCompra'), c.usoVenta && t('producto.usoVenta')].filter(Boolean).join(' / ') },
            {
              campo: 'acc', titulo: '', align: 'right',
              render: (c) => puedeEscribir && (
                <IconButton size="small" onClick={() => productosApi.eliminarConversion(producto.id, c.id).then(cargar).catch(notificarError)}>
                  <DeleteIcon fontSize="small" />
                </IconButton>
              ),
            },
          ]}
        />
        {puedeEscribir && (
          <Stack direction={{ xs: 'column', sm: 'row' }} spacing={1} mt={2} alignItems="center">
            <TextField select size="small" label={t('comun.unidad')} value={nueva.unidadId} sx={{ minWidth: 160 }}
              onChange={(e) => setNueva({ ...nueva, unidadId: e.target.value })}>
              {unidades.filter((u) => u.id !== producto?.unidadBaseId).map((u) => (
                <MenuItem key={u.id} value={u.id}>{u.codigo} - {u.nombre}</MenuItem>
              ))}
            </TextField>
            <TextField size="small" type="number" label={t('producto.factor')} value={nueva.factor}
              onChange={(e) => setNueva({ ...nueva, factor: e.target.value })} />
            <FormControlLabel control={<Checkbox checked={nueva.usoCompra} onChange={(e) => setNueva({ ...nueva, usoCompra: e.target.checked })} />} label={t('producto.usoCompra')} />
            <FormControlLabel control={<Checkbox checked={nueva.usoVenta} onChange={(e) => setNueva({ ...nueva, usoVenta: e.target.checked })} />} label={t('producto.usoVenta')} />
            <Button variant="contained" onClick={agregar} disabled={!nueva.unidadId || !(Number(nueva.factor) > 0)}>{t('comun.agregar')}</Button>
          </Stack>
        )}
      </DialogContent>
      <DialogActions><Button onClick={onCerrar}>{t('comun.cerrar')}</Button></DialogActions>
    </Dialog>
  );
}
