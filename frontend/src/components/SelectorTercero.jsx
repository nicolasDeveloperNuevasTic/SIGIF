/**
 * @file components/SelectorTercero.jsx
 * @description Autocompletar de clientes o proveedores con búsqueda en el servidor (o caché local sin conexión).
 * @created 21/10/2026
 * @updated 21/10/2026
 * @author Equipo SIGIF
 */
import { useEffect, useState } from 'react';
import { Autocomplete, TextField } from '@mui/material';
import { tercerosApi } from '../api/servicios';
import { clientesLocales } from '../offline/db';

/**
 * @param {object} props propiedades
 * @param {'CLIENTE'|'PROVEEDOR'} props.rol rol a buscar
 * @param {string} props.label etiqueta
 * @param {object|null} props.valor tercero seleccionado {id, nombre, numeroDocumento}
 * @param {(t: object|null) => void} props.onChange cambio de selección
 * @param {string} [props.error] mensaje de error
 * @param {string} [props.testId] id para pruebas
 * @returns {JSX.Element} selector
 */
export default function SelectorTercero({ rol, label, valor, onChange, error, testId }) {
  const [texto, setTexto] = useState('');
  const [opciones, setOpciones] = useState([]);

  useEffect(() => {
    let vigente = true;
    const temporizador = setTimeout(async () => {
      try {
        if (!navigator.onLine && rol === 'CLIENTE') {
          const locales = await clientesLocales();
          const tx = texto.toLowerCase();
          if (vigente) {
            setOpciones(locales.filter((c) => c.nombre.toLowerCase().includes(tx) || c.numeroDocumento.includes(tx)).slice(0, 20));
          }
          return;
        }
        const r = await tercerosApi.buscar({ rol, q: texto, tamano: 20 });
        if (vigente) {
          setOpciones(r.contenido.filter((x) => x.activo).map((x) => ({ id: x.id, nombre: x.nombreMostrar, numeroDocumento: x.numeroDocumento })));
        }
      } catch {
        if (vigente) setOpciones([]);
      }
    }, 300);
    return () => {
      vigente = false;
      clearTimeout(temporizador);
    };
  }, [texto, rol]);

  return (
    <Autocomplete
      options={opciones}
      value={valor}
      filterOptions={(x) => x}
      getOptionLabel={(o) => (o ? `${o.numeroDocumento} - ${o.nombre}` : '')}
      isOptionEqualToValue={(a, b) => a.id === b.id}
      onChange={(_, v) => onChange(v)}
      onInputChange={(_, v) => setTexto(v)}
      renderInput={(params) => (
        <TextField {...params} label={label} size="small" error={Boolean(error)} helperText={error}
          inputProps={{ ...params.inputProps, 'data-testid': testId }} />
      )}
    />
  );
}
