/**
 * @file pages/CatalogosPage.jsx
 * @description Administración de categorías (con subcategorías), marcas, presentaciones y unidades de medida.
 * @created 21/10/2026
 * @updated 21/10/2026
 * @author Equipo SIGIF
 */
import { useCallback, useEffect, useMemo, useState } from 'react';
import { Button, Chip, Dialog, DialogActions, DialogContent, DialogTitle, IconButton, Stack, Tab, Tabs } from '@mui/material';
import AddIcon from '@mui/icons-material/Add';
import EditIcon from '@mui/icons-material/Edit';
import { Form, Formik } from 'formik';
import { useTranslation } from 'react-i18next';
import { catalogosApi } from '../api/servicios';
import { useNotificacion } from '../context/NotificacionContext';
import EncabezadoPagina from '../components/EncabezadoPagina';
import TablaDatos from '../components/TablaDatos';
import CampoTexto from '../components/CampoTexto';
import CampoCheck from '../components/CampoCheck';
import { catalogoEsquema } from '../validation/esquemas';

const TIPOS = ['categorias', 'marcas', 'presentaciones', 'unidades-medida'];

/**
 * @returns {JSX.Element} pantalla de catálogos
 */
export default function CatalogosPage() {
  const { t } = useTranslation();
  const { notificar, notificarError } = useNotificacion();
  const [tipo, setTipo] = useState(TIPOS[0]);
  const [lista, setLista] = useState([]);
  const [categorias, setCategorias] = useState([]);
  const [editando, setEditando] = useState(undefined);
  const esquema = useMemo(() => catalogoEsquema(t, tipo), [t, tipo]);

  const cargar = useCallback(async () => {
    try {
      const datos = await catalogosApi.listar(tipo);
      setLista(datos);
      if (tipo === 'categorias') setCategorias(datos);
    } catch (e) {
      notificarError(e);
    }
  }, [tipo, notificarError]);

  useEffect(() => {
    cargar();
  }, [cargar]);

  const nombrePadre = (id) => categorias.find((c) => c.id === id)?.nombre ?? '';

  return (
    <>
      <EncabezadoPagina titulo={t('catalogo.titulo')}>
        <Button variant="contained" startIcon={<AddIcon />} onClick={() => setEditando(null)}>{t('comun.nuevo')}</Button>
      </EncabezadoPagina>
      <Tabs value={tipo} onChange={(_, v) => setTipo(v)} sx={{ mb: 2 }} variant="scrollable">
        {TIPOS.map((x) => <Tab key={x} value={x} label={t(`catalogo.${x}`)} />)}
      </Tabs>
      <TablaDatos
        filas={lista}
        columnas={[
          ...(tipo === 'unidades-medida' ? [{ campo: 'codigo', titulo: t('catalogo.codigo') }] : []),
          { campo: 'nombre', titulo: t('catalogo.nombre') },
          ...(tipo === 'categorias' ? [{ campo: 'categoriaPadreId', titulo: t('catalogo.padre'), render: (c) => nombrePadre(c.categoriaPadreId) }] : []),
          { campo: 'descripcion', titulo: t('catalogo.descripcion') },
          { campo: 'activo', titulo: t('comun.estado'), render: (c) => <Chip size="small" label={c.activo ? t('comun.activo') : t('comun.inactivo')} color={c.activo ? 'success' : 'default'} /> },
          { campo: 'acc', titulo: '', align: 'right', render: (c) => <IconButton size="small" onClick={() => setEditando(c)}><EditIcon fontSize="small" /></IconButton> },
        ]}
      />
      <Dialog open={editando !== undefined} onClose={() => setEditando(undefined)} maxWidth="xs" fullWidth>
        <Formik
          enableReinitialize
          initialValues={{
            nombre: editando?.nombre ?? '', descripcion: editando?.descripcion ?? '', codigo: editando?.codigo ?? '',
            categoriaPadreId: editando?.categoriaPadreId ?? '', activo: editando?.activo ?? true,
          }}
          validationSchema={esquema}
          onSubmit={async (v, { setSubmitting }) => {
            const cuerpo = { ...v, categoriaPadreId: v.categoriaPadreId || null };
            try {
              if (editando?.id) await catalogosApi.actualizar(tipo, editando.id, cuerpo);
              else await catalogosApi.crear(tipo, cuerpo);
              notificar(t('comun.exito'));
              setEditando(undefined);
              cargar();
            } catch (e) {
              notificarError(e);
            } finally {
              setSubmitting(false);
            }
          }}
        >
          {({ isSubmitting }) => (
            <Form noValidate>
              <DialogTitle>{t(`catalogo.${tipo}`)}</DialogTitle>
              <DialogContent dividers>
                <Stack spacing={2}>
                  {tipo === 'unidades-medida' && <CampoTexto name="codigo" label={t('catalogo.codigo')} />}
                  <CampoTexto name="nombre" label={t('catalogo.nombre')} />
                  <CampoTexto name="descripcion" label={t('catalogo.descripcion')} />
                  {tipo === 'categorias' && (
                    <CampoTexto name="categoriaPadreId" label={t('catalogo.padre')}
                      opciones={[{ value: '', label: t('catalogo.ninguna') },
                        ...categorias.filter((c) => c.id !== editando?.id).map((c) => ({ value: c.id, label: c.nombre }))]} />
                  )}
                  <CampoCheck name="activo" label={t('comun.activo')} />
                </Stack>
              </DialogContent>
              <DialogActions>
                <Button onClick={() => setEditando(undefined)}>{t('comun.cancelar')}</Button>
                <Button type="submit" variant="contained" disabled={isSubmitting}>{t('comun.guardar')}</Button>
              </DialogActions>
            </Form>
          )}
        </Formik>
      </Dialog>
    </>
  );
}
