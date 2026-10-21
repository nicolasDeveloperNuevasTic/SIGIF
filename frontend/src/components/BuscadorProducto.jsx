/**
 * @file components/BuscadorProducto.jsx
 * @description Campo para escanear código de barras o buscar productos por nombre (en línea o desde IndexedDB).
 * @created 21/10/2026
 * @updated 21/10/2026
 * @author Equipo SIGIF
 */
import { useEffect, useRef, useState } from 'react';
import { Autocomplete, InputAdornment, TextField } from '@mui/material';
import QrCodeScannerIcon from '@mui/icons-material/QrCodeScanner';
import { useTranslation } from 'react-i18next';
import { productosApi } from '../api/servicios';
import { buscarProductos, productoPorCodigo } from '../offline/db';
import useLectorCodigoBarras from '../hooks/useLectorCodigoBarras';
import { useNotificacion } from '../context/NotificacionContext';

/**
 * Normaliza un producto (del API o de la caché offline) al formato usado por las pantallas.
 * @param {object} p producto
 * @returns {object} producto normalizado
 */
function normalizar(p) {
  return {
    id: p.id,
    codigoBarras: p.codigoBarras,
    nombre: p.nombre,
    unidadBaseId: p.unidadBaseId,
    unidadBase: p.unidadBase,
    permiteFraccion: p.permiteFraccion,
    precioVenta: Number(p.precioVentaCalculado ?? p.precioVenta),
    precioCompra: p.precioCompra != null ? Number(p.precioCompra) : null,
    porcentajeIva: Number(p.porcentajeIva ?? 0),
    disponible: Number(p.existencias?.disponible ?? p.disponible ?? 0),
    tieneVencimiento: Boolean(p.tieneVencimiento),
    tieneGarantia: Boolean(p.tieneGarantia),
    mesesGarantia: p.mesesGarantia,
    conversiones: p.conversiones,
  };
}

/**
 * @param {object} props propiedades
 * @param {(producto: object) => void} props.onSeleccionar producto elegido o escaneado
 * @param {boolean} [props.capturarLector=true] escuchar el lector aunque el foco no esté en el campo
 * @returns {JSX.Element} buscador
 */
export default function BuscadorProducto({ onSeleccionar, capturarLector = true }) {
  const { t } = useTranslation();
  const { notificar } = useNotificacion();
  const [texto, setTexto] = useState('');
  const [opciones, setOpciones] = useState([]);
  const entrada = useRef(null);

  const porCodigo = async (codigo) => {
    try {
      const p = navigator.onLine ? await productosApi.porCodigo(codigo) : await productoPorCodigo(codigo);
      if (!p) throw new Error('no encontrado');
      onSeleccionar(normalizar(p));
      setTexto('');
    } catch (e) {
      // Sin red en medio de la consulta: intentar con la caché local.
      const local = !e?.response ? await productoPorCodigo(codigo) : null;
      if (local) {
        onSeleccionar(normalizar(local));
        setTexto('');
      } else {
        notificar(`${t('producto.noEncontrado')}: ${codigo}`, 'warning');
      }
    }
  };

  useLectorCodigoBarras(porCodigo, capturarLector);

  useEffect(() => {
    if (texto.trim().length < 2) {
      setOpciones([]);
      return undefined;
    }
    let vigente = true;
    const temporizador = setTimeout(async () => {
      try {
        const lista = navigator.onLine
          ? (await productosApi.listar({ nombre: texto, estado: 'ACTIVO', tamano: 15 })).contenido
          : await buscarProductos(texto, 15);
        if (vigente) setOpciones(lista.map(normalizar));
      } catch {
        if (vigente) setOpciones((await buscarProductos(texto, 15)).map(normalizar));
      }
    }, 250);
    return () => {
      vigente = false;
      clearTimeout(temporizador);
    };
  }, [texto]);

  return (
    <Autocomplete
      freeSolo
      options={opciones}
      inputValue={texto}
      filterOptions={(x) => x}
      getOptionLabel={(o) => (typeof o === 'string' ? o : `${o.codigoBarras} - ${o.nombre}`)}
      onInputChange={(_, v, razon) => razon !== 'reset' && setTexto(v)}
      onChange={(_, v) => {
        if (v && typeof v === 'object') {
          onSeleccionar(v);
          setTexto('');
        }
      }}
      renderInput={(params) => (
        <TextField
          {...params}
          inputRef={entrada}
          label={t('producto.escanear')}
          size="small"
          autoFocus
          onKeyDown={(e) => {
            if (e.key === 'Enter' && texto.trim() && !opciones.length) {
              e.preventDefault();
              porCodigo(texto.trim());
            }
          }}
          InputProps={{
            ...params.InputProps,
            startAdornment: <InputAdornment position="start"><QrCodeScannerIcon /></InputAdornment>,
          }}
          inputProps={{ ...params.inputProps, 'data-testid': 'buscador-producto' }}
        />
      )}
    />
  );
}
