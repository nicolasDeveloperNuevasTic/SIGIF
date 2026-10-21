/**
 * @file hooks/useCatalogos.js
 * @description Carga categorías, marcas, presentaciones y unidades de medida para formularios.
 * @created 21/10/2026
 * @updated 21/10/2026
 * @author Equipo SIGIF
 */
import { useCallback, useEffect, useState } from 'react';
import { catalogosApi } from '../api/servicios';

const VACIO = { categorias: [], marcas: [], presentaciones: [], unidades: [] };

/**
 * @returns {{categorias: object[], marcas: object[], presentaciones: object[], unidades: object[], recargar: Function}} catálogos
 */
export default function useCatalogos() {
  const [catalogos, setCatalogos] = useState(VACIO);

  const recargar = useCallback(async () => {
    try {
      const [categorias, marcas, presentaciones, unidades] = await Promise.all([
        catalogosApi.listar('categorias'),
        catalogosApi.listar('marcas'),
        catalogosApi.listar('presentaciones'),
        catalogosApi.listar('unidades-medida'),
      ]);
      setCatalogos({ categorias, marcas, presentaciones, unidades });
    } catch {
      setCatalogos(VACIO);
    }
  }, []);

  useEffect(() => {
    recargar();
  }, [recargar]);

  return { ...catalogos, recargar };
}

/**
 * @param {object[]} lista elementos de catálogo
 * @param {boolean} [conCodigo=false] mostrar el código (unidades)
 * @returns {{value: number, label: string}[]} opciones para selects
 */
export function opciones(lista, conCodigo = false) {
  return lista.filter((x) => x.activo).map((x) => ({ value: x.id, label: conCodigo ? `${x.codigo} - ${x.nombre}` : x.nombre }));
}
