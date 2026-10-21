/**
 * @file components/CampoTexto.jsx
 * @description Campo de texto/selección de Material UI enlazado a Formik (muestra errores de Yup).
 * @created 21/10/2026
 * @updated 21/10/2026
 * @author Equipo SIGIF
 */
import { useField } from 'formik';
import { MenuItem, TextField } from '@mui/material';

/**
 * @param {object} props propiedades
 * @param {string} props.name nombre del campo en Formik
 * @param {string} props.label etiqueta visible
 * @param {{value: string|number, label: string}[]} [props.opciones] opciones si es un select
 * @returns {JSX.Element} campo
 */
export default function CampoTexto({ name, label, opciones, ...resto }) {
  const [campo, meta] = useField(name);
  const conError = Boolean(meta.touched && meta.error);
  return (
    <TextField
      {...campo}
      {...resto}
      value={campo.value ?? ''}
      id={name}
      label={label}
      select={Boolean(opciones)}
      error={conError}
      helperText={conError ? meta.error : resto.helperText}
      fullWidth
      size="small"
      inputProps={{ 'data-testid': `campo-${name}`, ...resto.inputProps }}
    >
      {opciones?.map((o) => (
        <MenuItem key={o.value} value={o.value}>{o.label}</MenuItem>
      ))}
    </TextField>
  );
}
