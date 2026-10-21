/**
 * @file pages/MigracionPage.jsx
 * @description Carga de datos desde Excel (.xlsx) con informe de filas importadas y rechazadas (REQ-F-036/037).
 * @created 21/10/2026
 * @updated 21/10/2026
 * @author Equipo SIGIF
 */
import { useState } from 'react';
import { Alert, Button, Card, CardContent, LinearProgress, Stack, Typography } from '@mui/material';
import UploadIcon from '@mui/icons-material/UploadFile';
import DownloadIcon from '@mui/icons-material/Download';
import { useTranslation } from 'react-i18next';
import { migracionApi } from '../api/servicios';
import { useNotificacion } from '../context/NotificacionContext';
import EncabezadoPagina from '../components/EncabezadoPagina';
import TablaDatos from '../components/TablaDatos';

/**
 * @returns {JSX.Element} pantalla de migración
 */
export default function MigracionPage() {
  const { t } = useTranslation();
  const { notificarError } = useNotificacion();
  const [archivo, setArchivo] = useState(null);
  const [cargando, setCargando] = useState(false);
  const [resultado, setResultado] = useState(null);

  const descargarPlantilla = async () => {
    try {
      const blob = await migracionApi.plantilla();
      const url = URL.createObjectURL(blob);
      const a = document.createElement('a');
      a.href = url;
      a.download = 'plantilla-migracion-sigif.xlsx';
      a.click();
      URL.revokeObjectURL(url);
    } catch (e) {
      notificarError(e);
    }
  };

  const importar = async () => {
    setCargando(true);
    setResultado(null);
    try {
      setResultado(await migracionApi.importar(archivo));
    } catch (e) {
      notificarError(e);
    } finally {
      setCargando(false);
    }
  };

  return (
    <>
      <EncabezadoPagina titulo={t('migracion.titulo')}>
        <Button startIcon={<DownloadIcon />} onClick={descargarPlantilla}>{t('migracion.plantilla')}</Button>
      </EncabezadoPagina>
      <Card variant="outlined" sx={{ mb: 2 }}>
        <CardContent>
          <Alert severity="info" sx={{ mb: 2 }}>{t('migracion.instrucciones')}</Alert>
          <Stack direction={{ xs: 'column', sm: 'row' }} spacing={2} alignItems="center">
            <Button variant="outlined" component="label" startIcon={<UploadIcon />}>
              {t('migracion.seleccionar')}
              <input hidden type="file" accept=".xlsx,application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
                onChange={(e) => setArchivo(e.target.files?.[0] ?? null)} data-testid="archivo-excel" />
            </Button>
            <Typography>{archivo?.name}</Typography>
            <Button variant="contained" disabled={!archivo || cargando} onClick={importar}>{t('migracion.importar')}</Button>
          </Stack>
          {cargando && <LinearProgress sx={{ mt: 2 }} />}
        </CardContent>
      </Card>
      {resultado && (
        <Stack spacing={2}>
          <TablaDatos
            filas={resultado.hojas}
            clave={(h) => h.hoja}
            columnas={[
              { campo: 'hoja', titulo: t('migracion.hoja') },
              { campo: 'leidas', titulo: t('migracion.leidas'), align: 'right' },
              { campo: 'importadas', titulo: t('migracion.importadas'), align: 'right' },
              { campo: 'rechazadas', titulo: t('migracion.rechazadas'), align: 'right' },
            ]}
          />
          {resultado.errores.length > 0 && (
            <TablaDatos
              filas={resultado.errores}
              clave={(e) => `${e.hoja}-${e.fila}-${e.causa}`}
              columnas={[
                { campo: 'hoja', titulo: t('migracion.hoja') },
                { campo: 'fila', titulo: t('migracion.fila'), align: 'right' },
                { campo: 'causa', titulo: t('migracion.causa') },
              ]}
            />
          )}
        </Stack>
      )}
    </>
  );
}
