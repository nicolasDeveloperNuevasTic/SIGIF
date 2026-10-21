/**
 * @file components/CampoCheck.jsx
 * @description Casilla de verificación de Material UI enlazada a Formik.
 * @created 21/10/2026
 * @updated 21/10/2026
 * @author Equipo SIGIF
 */
import { useField } from 'formik';
import { Checkbox, FormControlLabel, FormHelperText } from '@mui/material';

/**
 * @param {{name: string, label: string}} props nombre Formik y etiqueta
 * @returns {JSX.Element} casilla
 */
export default function CampoCheck({ name, label }) {
  const [campo, meta, ayudas] = useField({ name, type: 'checkbox' });
  return (
    <>
      <FormControlLabel
        control={(
          <Checkbox
            checked={Boolean(campo.value)}
            onChange={(e) => ayudas.setValue(e.target.checked)}
            inputProps={{ 'data-testid': `check-${name}` }}
          />
        )}
        label={label}
      />
      {meta.touched && meta.error && <FormHelperText error>{meta.error}</FormHelperText>}
    </>
  );
}
