/**
 * @file components/DialogoConfirmacion.jsx
 * @description Diálogo modal de confirmación para acciones irreversibles.
 * @created 21/10/2026
 * @updated 21/10/2026
 * @author Equipo SIGIF
 */
import { Button, Dialog, DialogActions, DialogContent, DialogContentText, DialogTitle } from '@mui/material';
import { useTranslation } from 'react-i18next';

/**
 * @param {{abierto: boolean, titulo: string, mensaje: string, onConfirmar: Function, onCancelar: Function}} props propiedades
 * @returns {JSX.Element} diálogo
 */
export default function DialogoConfirmacion({ abierto, titulo, mensaje, onConfirmar, onCancelar }) {
  const { t } = useTranslation();
  return (
    <Dialog open={abierto} onClose={onCancelar}>
      <DialogTitle>{titulo}</DialogTitle>
      <DialogContent>
        <DialogContentText>{mensaje}</DialogContentText>
      </DialogContent>
      <DialogActions>
        <Button onClick={onCancelar}>{t('comun.cancelar')}</Button>
        <Button onClick={onConfirmar} variant="contained" color="error" data-testid="confirmar">{t('comun.confirmar')}</Button>
      </DialogActions>
    </Dialog>
  );
}
