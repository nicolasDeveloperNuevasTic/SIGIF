/**
 * @file pages/compras/ComprasPage.jsx
 * @description Listado de Documentos de Ingreso generados por entradas por compra.
 * @created 21/10/2026
 * @updated 21/10/2026
 * @author Equipo SIGIF
 */
import { useCallback, useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { Button, IconButton, Stack, TextField, Tooltip } from '@mui/material';
import AddIcon from '@mui/icons-material/Add';
import PrintIcon from '@mui/icons-material/Print';
import { useTranslation } from 'react-i18next';
import { comprasApi } from '../../api/servicios';
import { useNotificacion } from '../../context/NotificacionContext';
import { useAuth } from '../../context/AuthContext';
import EncabezadoPagina from '../../components/EncabezadoPagina';
import TablaDatos from '../../components/TablaDatos';
import { fecha, moneda } from '../../utils/formato';

/**
 * @returns {JSX.Element} pantalla de documentos de ingreso
 */
export default function ComprasPage() {
  const { t } = useTranslation();
  const { notificarError } = useNotificacion();
  const { tienePermiso } = useAuth();
  const navigate = useNavigate();
  const [numero, setNumero] = useState('');
  const [pagina, setPagina] = useState(0);
  const [tamano, setTamano] = useState(20);
  const [datos, setDatos] = useState({ contenido: [], totalElementos: 0 });

  const cargar = useCallback(() => {
    comprasApi.listar({ numero: numero || undefined, pagina, tamano }).then(setDatos).catch(notificarError);
  }, [numero, pagina, tamano, notificarError]);

  useEffect(() => {
    const tm = setTimeout(cargar, 300);
    return () => clearTimeout(tm);
  }, [cargar]);

  return (
    <>
      <EncabezadoPagina titulo={t('compra.documentos')}>
        {tienePermiso('COMPRA_REGISTRAR') && (
          <Button variant="contained" startIcon={<AddIcon />} onClick={() => navigate('/compras/nueva')} data-testid="nueva-compra">
            {t('compra.nueva')}
          </Button>
        )}
      </EncabezadoPagina>
      <Stack direction="row" mb={2}>
        <TextField size="small" label={`${t('comun.buscar')} DI-`} value={numero} onChange={(e) => setNumero(e.target.value)} />
      </Stack>
      <TablaDatos
        filas={datos.contenido}
        paginacion={{ pagina, tamano, total: datos.totalElementos, onPagina: setPagina, onTamano: setTamano }}
        columnas={[
          { campo: 'numero', titulo: t('compra.documento') },
          { campo: 'fecha', titulo: t('comun.fecha'), render: (d) => fecha(d.fecha) },
          { campo: 'proveedor', titulo: t('compra.proveedor'), render: (d) => `${d.proveedor.numeroDocumento} - ${d.proveedor.nombre}` },
          { campo: 'numeroFacturaProveedor', titulo: t('compra.facturaProveedor') },
          { campo: 'total', titulo: t('comun.total'), align: 'right', render: (d) => moneda(d.total) },
          { campo: 'usuario', titulo: t('compra.responsable') },
          {
            campo: 'acc', titulo: '', align: 'right',
            render: (d) => <Tooltip title={t('comun.imprimir')}><IconButton size="small" onClick={() => navigate(`/imprimir/ingreso/${d.id}`)}><PrintIcon fontSize="small" /></IconButton></Tooltip>,
          },
        ]}
      />
    </>
  );
}
