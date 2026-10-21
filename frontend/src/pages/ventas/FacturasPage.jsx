/**
 * @file pages/ventas/FacturasPage.jsx
 * @description Listado de facturas emitidas con acceso a impresión.
 * @created 21/10/2026
 * @updated 21/10/2026
 * @author Equipo SIGIF
 */
import { useCallback, useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { Chip, IconButton, Stack, TextField, Tooltip } from '@mui/material';
import PrintIcon from '@mui/icons-material/Print';
import { useTranslation } from 'react-i18next';
import { ventasApi } from '../../api/servicios';
import { useNotificacion } from '../../context/NotificacionContext';
import EncabezadoPagina from '../../components/EncabezadoPagina';
import TablaDatos from '../../components/TablaDatos';
import { fecha, moneda } from '../../utils/formato';

/**
 * @returns {JSX.Element} pantalla de facturas
 */
export default function FacturasPage() {
  const { t } = useTranslation();
  const { notificarError } = useNotificacion();
  const navigate = useNavigate();
  const [numero, setNumero] = useState('');
  const [pagina, setPagina] = useState(0);
  const [tamano, setTamano] = useState(20);
  const [datos, setDatos] = useState({ contenido: [], totalElementos: 0 });

  const cargar = useCallback(() => {
    ventasApi.listar({ numero: numero || undefined, pagina, tamano }).then(setDatos).catch(notificarError);
  }, [numero, pagina, tamano, notificarError]);

  useEffect(() => {
    const tm = setTimeout(cargar, 300);
    return () => clearTimeout(tm);
  }, [cargar]);

  return (
    <>
      <EncabezadoPagina titulo={t('venta.facturas')} />
      <Stack direction="row" mb={2}>
        <TextField size="small" label={`${t('comun.buscar')} FV-`} value={numero} onChange={(e) => setNumero(e.target.value)} />
      </Stack>
      <TablaDatos
        filas={datos.contenido}
        paginacion={{ pagina, tamano, total: datos.totalElementos, onPagina: setPagina, onTamano: setTamano }}
        columnas={[
          { campo: 'numero', titulo: t('venta.factura') },
          { campo: 'fecha', titulo: t('comun.fecha'), render: (f) => fecha(f.fecha) },
          { campo: 'cliente', titulo: t('venta.cliente'), render: (f) => `${f.cliente.numeroDocumento} - ${f.cliente.nombre}` },
          { campo: 'origen', titulo: t('venta.origen'), render: (f) => <Chip size="small" label={f.origen} color={f.origen === 'OFFLINE' ? 'warning' : 'default'} /> },
          { campo: 'total', titulo: t('comun.total'), align: 'right', render: (f) => moneda(f.total) },
          { campo: 'usuario', titulo: t('comun.usuario') },
          {
            campo: 'acc', titulo: '', align: 'right',
            render: (f) => <Tooltip title={t('comun.imprimir')}><IconButton size="small" onClick={() => navigate(`/imprimir/factura/${f.id}`)}><PrintIcon fontSize="small" /></IconButton></Tooltip>,
          },
        ]}
      />
    </>
  );
}
