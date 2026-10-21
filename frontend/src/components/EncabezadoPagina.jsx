/**
 * @file components/EncabezadoPagina.jsx
 * @description Título de página con zona de acciones a la derecha.
 * @created 21/10/2026
 * @updated 21/10/2026
 * @author Equipo SIGIF
 */
import { Stack, Typography } from '@mui/material';

/**
 * @param {{titulo: string, children?: import('react').ReactNode}} props título y acciones
 * @returns {JSX.Element} encabezado
 */
export default function EncabezadoPagina({ titulo, children }) {
  return (
    <Stack direction={{ xs: 'column', sm: 'row' }} justifyContent="space-between" alignItems={{ sm: 'center' }} spacing={1} mb={2}>
      <Typography variant="h5" component="h1" fontWeight={600}>{titulo}</Typography>
      <Stack direction="row" spacing={1} flexWrap="wrap">{children}</Stack>
    </Stack>
  );
}
