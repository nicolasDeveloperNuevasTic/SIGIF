/**
 * @file components/TablaDatos.jsx
 * @description Tabla genérica de Material UI con paginación del lado del servidor y estado de carga.
 * @created 21/10/2026
 * @updated 21/10/2026
 * @author Equipo SIGIF
 */
import {
  LinearProgress, Paper, Table, TableBody, TableCell, TableContainer, TableHead, TablePagination, TableRow,
} from '@mui/material';
import { useTranslation } from 'react-i18next';

/**
 * @typedef {object} Columna
 * @property {string} campo clave del dato
 * @property {string} titulo encabezado
 * @property {(fila: object) => import('react').ReactNode} [render] renderizado personalizado
 * @property {'left'|'right'|'center'} [align] alineación
 */

/**
 * @param {object} props propiedades
 * @param {Columna[]} props.columnas columnas
 * @param {object[]} props.filas filas
 * @param {boolean} [props.cargando] indicador de carga
 * @param {{pagina: number, tamano: number, total: number, onPagina: Function, onTamano: Function}} [props.paginacion] paginación
 * @param {(fila: object) => string|number} [props.clave] clave de fila
 * @param {string} [props.testId] identificador para pruebas E2E
 * @returns {JSX.Element} tabla
 */
export default function TablaDatos({ columnas, filas, cargando, paginacion, clave = (f) => f.id, testId }) {
  const { t } = useTranslation();
  return (
    <Paper variant="outlined">
      {cargando && <LinearProgress />}
      <TableContainer>
        <Table size="small" data-testid={testId}>
          <TableHead>
            <TableRow>
              {columnas.map((c) => (
                <TableCell key={c.campo} align={c.align} sx={{ fontWeight: 600, whiteSpace: 'nowrap' }}>{c.titulo}</TableCell>
              ))}
            </TableRow>
          </TableHead>
          <TableBody>
            {filas.length === 0 && !cargando && (
              <TableRow>
                <TableCell colSpan={columnas.length} align="center">{t('comun.sinDatos')}</TableCell>
              </TableRow>
            )}
            {filas.map((f) => (
              <TableRow key={clave(f)} hover>
                {columnas.map((c) => (
                  <TableCell key={c.campo} align={c.align}>{c.render ? c.render(f) : f[c.campo]}</TableCell>
                ))}
              </TableRow>
            ))}
          </TableBody>
        </Table>
      </TableContainer>
      {paginacion && (
        <TablePagination
          component="div"
          count={paginacion.total}
          page={paginacion.pagina}
          rowsPerPage={paginacion.tamano}
          rowsPerPageOptions={[10, 20, 50, 100]}
          labelRowsPerPage={t('comun.filasPorPagina')}
          onPageChange={(_, p) => paginacion.onPagina(p)}
          onRowsPerPageChange={(e) => paginacion.onTamano(Number(e.target.value))}
        />
      )}
    </Paper>
  );
}
