/**
 * @file pages/productos/ProductosPage.jsx
 * @description CRUD de productos: listado paginado con filtros, creación, edición, inactivación, conversiones e historial.
 * @created 21/10/2026
 * @updated 21/10/2026
 * @author Equipo SIGIF
 */
import { useCallback, useEffect, useState } from 'react';
import { Button, Chip, IconButton, MenuItem, Stack, TextField, Tooltip } from '@mui/material';
import AddIcon from '@mui/icons-material/Add';
import EditIcon from '@mui/icons-material/Edit';
import BlockIcon from '@mui/icons-material/Block';
import CheckIcon from '@mui/icons-material/CheckCircle';
import SwapIcon from '@mui/icons-material/SwapHoriz';
import TimelineIcon from '@mui/icons-material/Timeline';
import { useTranslation } from 'react-i18next';
import { productosApi } from '../../api/servicios';
import { useNotificacion } from '../../context/NotificacionContext';
import { useAuth } from '../../context/AuthContext';
import useCatalogos, { opciones } from '../../hooks/useCatalogos';
import useLectorCodigoBarras from '../../hooks/useLectorCodigoBarras';
import EncabezadoPagina from '../../components/EncabezadoPagina';
import TablaDatos from '../../components/TablaDatos';
import DialogoConfirmacion from '../../components/DialogoConfirmacion';
import ProductoFormDialog from './ProductoFormDialog';
import ConversionesDialog from './ConversionesDialog';
import HistorialPreciosDialog from './HistorialPreciosDialog';
import { cantidad, moneda } from '../../utils/formato';

/**
 * @returns {JSX.Element} pantalla de productos
 */
export default function ProductosPage() {
  const { t } = useTranslation();
  const { notificar, notificarError } = useNotificacion();
  const { tienePermiso } = useAuth();
  const catalogos = useCatalogos();
  const [filtros, setFiltros] = useState({ nombre: '', categoriaId: '', estado: 'ACTIVO' });
  const [pagina, setPagina] = useState(0);
  const [tamano, setTamano] = useState(20);
  const [datos, setDatos] = useState({ contenido: [], totalElementos: 0 });
  const [cargando, setCargando] = useState(false);
  const [editando, setEditando] = useState(undefined);
  const [conversiones, setConversiones] = useState(null);
  const [historial, setHistorial] = useState(null);
  const [inactivar, setInactivar] = useState(null);

  const cargar = useCallback(async () => {
    setCargando(true);
    try {
      setDatos(await productosApi.listar({
        nombre: filtros.nombre || undefined,
        categoriaId: filtros.categoriaId || undefined,
        estado: filtros.estado || undefined,
        pagina,
        tamano,
      }));
    } catch (e) {
      notificarError(e);
    } finally {
      setCargando(false);
    }
  }, [filtros, pagina, tamano, notificarError]);

  useEffect(() => {
    const temporizador = setTimeout(cargar, 300);
    return () => clearTimeout(temporizador);
  }, [cargar]);

  // Escanear con el lector posiciona el filtro en el producto (CA-13.1).
  useLectorCodigoBarras((codigo) => setFiltros((f) => ({ ...f, nombre: codigo })));

  const guardar = async (valores) => {
    try {
      if (editando?.id) {
        await productosApi.actualizar(editando.id, valores);
      } else {
        await productosApi.crear(valores);
      }
      notificar(t('comun.exito'));
      setEditando(undefined);
      cargar();
    } catch (e) {
      notificarError(e);
      throw e;
    }
  };

  const cambiarEstado = async (p, estado) => {
    try {
      await productosApi.cambiarEstado(p.id, estado);
      notificar(t('comun.exito'));
      cargar();
    } catch (e) {
      notificarError(e);
    }
  };

  const escribir = tienePermiso('PRODUCTO_ESCRIBIR');

  return (
    <>
      <EncabezadoPagina titulo={t('producto.titulo')}>
        {escribir && (
          <Button variant="contained" startIcon={<AddIcon />} onClick={() => setEditando(null)} data-testid="nuevo-producto">
            {t('producto.nuevo')}
          </Button>
        )}
      </EncabezadoPagina>
      <Stack direction={{ xs: 'column', md: 'row' }} spacing={1} mb={2}>
        <TextField size="small" label={`${t('comun.buscar')} (${t('producto.nombre')} / ${t('producto.codigoBarras')})`}
          value={filtros.nombre} onChange={(e) => { setPagina(0); setFiltros({ ...filtros, nombre: e.target.value }); }} sx={{ flex: 1 }} />
        <TextField select size="small" label={t('producto.categoria')} value={filtros.categoriaId} sx={{ minWidth: 200 }}
          onChange={(e) => { setPagina(0); setFiltros({ ...filtros, categoriaId: e.target.value }); }}>
          <MenuItem value="">{t('comun.todos')}</MenuItem>
          {opciones(catalogos.categorias).map((o) => <MenuItem key={o.value} value={o.value}>{o.label}</MenuItem>)}
        </TextField>
        <TextField select size="small" label={t('comun.estado')} value={filtros.estado} sx={{ minWidth: 150 }}
          onChange={(e) => { setPagina(0); setFiltros({ ...filtros, estado: e.target.value }); }}>
          <MenuItem value="">{t('comun.todos')}</MenuItem>
          <MenuItem value="ACTIVO">{t('comun.activo')}</MenuItem>
          <MenuItem value="INACTIVO">{t('comun.inactivo')}</MenuItem>
        </TextField>
      </Stack>
      <TablaDatos
        testId="tabla-productos"
        cargando={cargando}
        filas={datos.contenido}
        paginacion={{ pagina, tamano, total: datos.totalElementos, onPagina: setPagina, onTamano: (n) => { setTamano(n); setPagina(0); } }}
        columnas={[
          { campo: 'codigoBarras', titulo: t('producto.codigoBarras') },
          { campo: 'nombre', titulo: t('producto.nombre') },
          { campo: 'categoria', titulo: t('producto.categoria') },
          { campo: 'marca', titulo: t('producto.marca'), render: (p) => `${p.marca} / ${p.presentacion}` },
          { campo: 'precioCompra', titulo: t('producto.precioCompra'), align: 'right', render: (p) => moneda(p.precioCompra) },
          { campo: 'porcentajeIncremento', titulo: '%', align: 'right' },
          { campo: 'precioVentaCalculado', titulo: t('producto.precioVenta'), align: 'right', render: (p) => moneda(p.precioVentaCalculado) },
          {
            campo: 'existencias', titulo: t('producto.existencias'), align: 'right',
            render: (p) => (
              <Tooltip title={`${t('producto.disponible')}: ${cantidad(p.existencias.disponible)} | ${t('producto.reservada')}: ${cantidad(p.existencias.reservada)} | ${t('producto.danada')}: ${cantidad(p.existencias.danada)}`}>
                <span>{cantidad(p.existencias.disponible)} {p.unidadBase}</span>
              </Tooltip>
            ),
          },
          {
            campo: 'estado', titulo: t('comun.estado'),
            render: (p) => <Chip size="small" label={p.estado === 'ACTIVO' ? t('comun.activo') : t('comun.inactivo')} color={p.estado === 'ACTIVO' ? 'success' : 'default'} />,
          },
          {
            campo: 'acciones', titulo: t('comun.acciones'), align: 'right',
            render: (p) => (
              <Stack direction="row" justifyContent="flex-end">
                {escribir && (
                  <Tooltip title={t('comun.editar')}><IconButton size="small" onClick={() => setEditando(p)}><EditIcon fontSize="small" /></IconButton></Tooltip>
                )}
                <Tooltip title={t('producto.conversiones')}><IconButton size="small" onClick={() => setConversiones(p)}><SwapIcon fontSize="small" /></IconButton></Tooltip>
                {tienePermiso('PRECIO_HISTORIAL_LEER') && (
                  <Tooltip title={t('producto.historial')}><IconButton size="small" onClick={() => setHistorial(p)}><TimelineIcon fontSize="small" /></IconButton></Tooltip>
                )}
                {escribir && p.estado === 'ACTIVO' && (
                  <Tooltip title={t('producto.inactivar')}><IconButton size="small" color="error" onClick={() => setInactivar(p)}><BlockIcon fontSize="small" /></IconButton></Tooltip>
                )}
                {escribir && p.estado !== 'ACTIVO' && (
                  <Tooltip title={t('producto.activar')}><IconButton size="small" color="success" onClick={() => cambiarEstado(p, 'ACTIVO')}><CheckIcon fontSize="small" /></IconButton></Tooltip>
                )}
              </Stack>
            ),
          },
        ]}
      />
      <ProductoFormDialog abierto={editando !== undefined} producto={editando} catalogos={catalogos}
        onGuardar={guardar} onCerrar={() => setEditando(undefined)} />
      <ConversionesDialog producto={conversiones} unidades={catalogos.unidades} onCerrar={() => setConversiones(null)} />
      <HistorialPreciosDialog producto={historial} onCerrar={() => setHistorial(null)} />
      <DialogoConfirmacion
        abierto={Boolean(inactivar)}
        titulo={t('producto.inactivar')}
        mensaje={t('producto.confirmarInactivar', { nombre: inactivar?.nombre })}
        onCancelar={() => setInactivar(null)}
        onConfirmar={async () => {
          try {
            await productosApi.eliminar(inactivar.id);
            notificar(t('comun.exito'));
            cargar();
          } catch (e) {
            notificarError(e);
          } finally {
            setInactivar(null);
          }
        }}
      />
    </>
  );
}
