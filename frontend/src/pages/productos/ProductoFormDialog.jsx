/**
 * @file pages/productos/ProductoFormDialog.jsx
 * @description Formulario de creación/edición de productos (Formik + Yup) con cálculo del precio de venta.
 * @created 21/10/2026
 * @updated 21/10/2026
 * @author Equipo SIGIF
 */
import { useMemo } from 'react';
import { Alert, Button, Dialog, DialogActions, DialogContent, DialogTitle, Grid } from '@mui/material';
import { Form, Formik } from 'formik';
import { useTranslation } from 'react-i18next';
import CampoTexto from '../../components/CampoTexto';
import CampoCheck from '../../components/CampoCheck';
import { productoEsquema } from '../../validation/esquemas';
import { opciones } from '../../hooks/useCatalogos';
import { moneda } from '../../utils/formato';

const INICIAL = {
  nombre: '', codigoBarras: '', categoriaId: '', marcaId: '', presentacionId: '', unidadBaseId: '', descripcion: '',
  permiteFraccion: false, precioCompra: '', porcentajeIncremento: '', porcentajeIva: 19, tieneGarantia: false,
  mesesGarantia: '', tieneVencimiento: false, diasAlertaVencimiento: 30, stockMinimo: 0,
};

/**
 * @param {object} props propiedades
 * @param {boolean} props.abierto visible
 * @param {object|null} props.producto producto a editar o null para crear
 * @param {object} props.catalogos catálogos para selects
 * @param {(valores: object) => Promise<void>} props.onGuardar guardar
 * @param {Function} props.onCerrar cerrar
 * @returns {JSX.Element} diálogo
 */
export default function ProductoFormDialog({ abierto, producto, catalogos, onGuardar, onCerrar }) {
  const { t } = useTranslation();
  const esquema = useMemo(() => productoEsquema(t), [t]);
  const inicial = producto ? { ...INICIAL, ...producto, descripcion: producto.descripcion ?? '', mesesGarantia: producto.mesesGarantia ?? '' } : INICIAL;

  return (
    <Dialog open={abierto} onClose={onCerrar} maxWidth="md" fullWidth>
      <Formik
        initialValues={inicial}
        validationSchema={esquema}
        enableReinitialize
        onSubmit={async (v, { setSubmitting }) => {
          try {
            await onGuardar({
              ...v,
              precioCompra: Number(v.precioCompra),
              porcentajeIncremento: Number(v.porcentajeIncremento),
              porcentajeIva: Number(v.porcentajeIva || 0),
              mesesGarantia: v.tieneGarantia ? Number(v.mesesGarantia) : null,
              stockMinimo: Number(v.stockMinimo || 0),
              diasAlertaVencimiento: v.diasAlertaVencimiento === '' ? null : Number(v.diasAlertaVencimiento),
            });
          } catch {
            // El error ya fue notificado por la página; el formulario permanece abierto.
          } finally {
            setSubmitting(false);
          }
        }}
      >
        {({ values, isSubmitting }) => {
          const pv = Number(values.precioCompra) * (1 + Number(values.porcentajeIncremento) / 100);
          return (
            <Form noValidate data-testid="form-producto">
              <DialogTitle>{producto ? t('producto.editar') : t('producto.nuevo')}</DialogTitle>
              <DialogContent dividers>
                <Grid container spacing={2}>
                  <Grid item xs={12} md={8}><CampoTexto name="nombre" label={t('producto.nombre')} /></Grid>
                  <Grid item xs={12} md={4}><CampoTexto name="codigoBarras" label={t('producto.codigoBarras')} /></Grid>
                  <Grid item xs={12} sm={6} md={3}><CampoTexto name="categoriaId" label={t('producto.categoria')} opciones={opciones(catalogos.categorias)} /></Grid>
                  <Grid item xs={12} sm={6} md={3}><CampoTexto name="marcaId" label={t('producto.marca')} opciones={opciones(catalogos.marcas)} /></Grid>
                  <Grid item xs={12} sm={6} md={3}><CampoTexto name="presentacionId" label={t('producto.presentacion')} opciones={opciones(catalogos.presentaciones)} /></Grid>
                  <Grid item xs={12} sm={6} md={3}><CampoTexto name="unidadBaseId" label={t('producto.unidadBase')} opciones={opciones(catalogos.unidades, true)} /></Grid>
                  <Grid item xs={12}><CampoTexto name="descripcion" label={t('producto.descripcion')} multiline minRows={2} /></Grid>
                  <Grid item xs={12} sm={4}><CampoTexto name="precioCompra" label={t('producto.precioCompra')} type="number" /></Grid>
                  <Grid item xs={12} sm={4}><CampoTexto name="porcentajeIncremento" label={t('producto.porcentajeIncremento')} type="number" /></Grid>
                  <Grid item xs={12} sm={4}><CampoTexto name="porcentajeIva" label={t('producto.porcentajeIva')} type="number" /></Grid>
                  {Number.isFinite(pv) && pv > 0 && (
                    <Grid item xs={12}>
                      <Alert severity="info" data-testid="precio-calculado">{t('producto.precioCalculado', { valor: moneda(pv) })}</Alert>
                    </Grid>
                  )}
                  <Grid item xs={12} sm={4}><CampoCheck name="permiteFraccion" label={t('producto.permiteFraccion')} /></Grid>
                  <Grid item xs={12} sm={4}><CampoCheck name="tieneGarantia" label={t('producto.tieneGarantia')} /></Grid>
                  <Grid item xs={12} sm={4}><CampoCheck name="tieneVencimiento" label={t('producto.tieneVencimiento')} /></Grid>
                  {values.tieneGarantia && (
                    <Grid item xs={12} sm={4}><CampoTexto name="mesesGarantia" label={t('producto.mesesGarantia')} type="number" /></Grid>
                  )}
                  {values.tieneVencimiento && (
                    <Grid item xs={12} sm={4}><CampoTexto name="diasAlertaVencimiento" label={t('producto.diasAlerta')} type="number" /></Grid>
                  )}
                  <Grid item xs={12} sm={4}><CampoTexto name="stockMinimo" label={t('producto.stockMinimo')} type="number" /></Grid>
                </Grid>
              </DialogContent>
              <DialogActions>
                <Button onClick={onCerrar}>{t('comun.cancelar')}</Button>
                <Button type="submit" variant="contained" disabled={isSubmitting} data-testid="guardar-producto">{t('comun.guardar')}</Button>
              </DialogActions>
            </Form>
          );
        }}
      </Formik>
    </Dialog>
  );
}
