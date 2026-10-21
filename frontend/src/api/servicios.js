/**
 * @file api/servicios.js
 * @description Funciones de acceso a cada recurso del contrato REST JSON del Back-End.
 * @created 21/10/2026
 * @updated 21/10/2026
 * @author Equipo SIGIF
 */
import cliente from './cliente';

const datos = (p) => p.then((r) => r.data);

/** Autenticación. */
export const authApi = {
  /** @param {{username: string, password: string}} c credenciales */
  login: (c) => datos(cliente.post('/auth/login', c)),
  perfil: () => datos(cliente.get('/auth/me')),
};

/** Productos (sección 9.3 del SRS). */
export const productosApi = {
  listar: (params) => datos(cliente.get('/productos', { params })),
  obtener: (id) => datos(cliente.get(`/productos/${id}`)),
  porCodigo: (codigo) => datos(cliente.get(`/productos/codigo-barras/${encodeURIComponent(codigo)}`)),
  crear: (p) => datos(cliente.post('/productos', p)),
  actualizar: (id, p) => datos(cliente.put(`/productos/${id}`, p)),
  cambiarEstado: (id, estado) => datos(cliente.patch(`/productos/${id}/estado`, { estado })),
  eliminar: (id) => cliente.delete(`/productos/${id}`),
  actualizarPrecio: (id, p) => datos(cliente.patch(`/productos/${id}/precio`, p)),
  historial: (id) => datos(cliente.get(`/productos/${id}/historial-precios`)),
  conversiones: (id) => datos(cliente.get(`/productos/${id}/conversiones`)),
  agregarConversion: (id, c) => datos(cliente.post(`/productos/${id}/conversiones`, c)),
  eliminarConversion: (id, cid) => cliente.delete(`/productos/${id}/conversiones/${cid}`),
};

/** Catálogos maestros: tipo = categorias | marcas | presentaciones | unidades-medida. */
export const catalogosApi = {
  listar: (tipo) => datos(cliente.get(`/catalogos/${tipo}`)),
  crear: (tipo, c) => datos(cliente.post(`/catalogos/${tipo}`, c)),
  actualizar: (tipo, id, c) => datos(cliente.put(`/catalogos/${tipo}/${id}`, c)),
};

/** Inventario y reservas. */
export const inventarioApi = {
  saldos: (params) => datos(cliente.get('/inventario/saldos', { params })),
  movimientos: (params) => datos(cliente.get('/inventario/movimientos', { params })),
  lotes: (productoId) => datos(cliente.get('/inventario/lotes', { params: { productoId } })),
  vencimientos: (dias = 30) => datos(cliente.get('/inventario/alertas/vencimientos', { params: { dias } })),
  stockMinimo: () => datos(cliente.get('/inventario/alertas/stock-minimo')),
  transicion: (t) => datos(cliente.post('/inventario/transiciones', t)),
  reservas: (params) => datos(cliente.get('/reservas', { params })),
  crearReserva: (r) => datos(cliente.post('/reservas', r)),
  cancelarReserva: (id) => datos(cliente.post(`/reservas/${id}/cancelar`)),
};

/** Compras / Documentos de Ingreso. */
export const comprasApi = {
  registrar: (c) => datos(cliente.post('/compras', c)),
  listar: (params) => datos(cliente.get('/compras', { params })),
  obtener: (id) => datos(cliente.get(`/compras/${id}`)),
};

/** Ventas / Facturas. */
export const ventasApi = {
  registrar: (v) => datos(cliente.post('/ventas', v)),
  listar: (params) => datos(cliente.get('/ventas', { params })),
  obtener: (id) => datos(cliente.get(`/ventas/${id}`)),
};

/** Terceros. */
export const tercerosApi = {
  buscar: (params) => datos(cliente.get('/terceros', { params })),
  crear: (t) => datos(cliente.post('/terceros', t)),
  actualizar: (id, t) => datos(cliente.put(`/terceros/${id}`, t)),
  cambiarEstado: (id, activo) => datos(cliente.patch(`/terceros/${id}/estado`, { activo })),
};

/** Cierres mensuales. */
export const cierresApi = {
  listar: () => datos(cliente.get('/cierres')),
  obtener: (id) => datos(cliente.get(`/cierres/${id}`)),
  prevalidar: (periodo) => datos(cliente.get('/cierres/prevalidacion', { params: { periodo } })),
  ejecutar: (c) => datos(cliente.post('/cierres', c)),
};

/** Seguridad RBAC. */
export const usuariosApi = {
  listar: () => datos(cliente.get('/usuarios')),
  crear: (u) => datos(cliente.post('/usuarios', u)),
  actualizar: (id, u) => datos(cliente.put(`/usuarios/${id}`, u)),
  cambiarEstado: (id, activo) => datos(cliente.patch(`/usuarios/${id}/estado`, { activo })),
  cambiarPassword: (id, password) => cliente.patch(`/usuarios/${id}/password`, { password }),
  generarApiKey: (id) => datos(cliente.post(`/usuarios/${id}/api-key`)),
  roles: () => datos(cliente.get('/roles')),
  permisos: () => datos(cliente.get('/permisos')),
  asignarPermisos: (rolId, permisos) => datos(cliente.put(`/roles/${rolId}/permisos`, { permisos })),
};

/** Auditoría. */
export const auditoriaApi = {
  buscar: (params) => datos(cliente.get('/auditoria', { params })),
};

/** Migración Excel. */
export const migracionApi = {
  importar: (archivo) => {
    const form = new FormData();
    form.append('archivo', archivo);
    return datos(cliente.post('/migracion/excel', form, {
      headers: { 'Content-Type': 'multipart/form-data' },
      timeout: 300000,
    }));
  },
  plantilla: () => cliente.get('/migracion/plantilla', { responseType: 'blob' }).then((r) => r.data),
};

/** Sincronización offline. */
export const sincronizacionApi = {
  catalogo: () => datos(cliente.get('/sincronizacion/catalogo', { timeout: 60000 })),
  enviarVentas: (ventas) => datos(cliente.post('/sincronizacion/ventas', { ventas }, { timeout: 120000 })),
  operaciones: (estado) => datos(cliente.get('/sincronizacion/operaciones', { params: { estado } })),
  reintentar: (id) => datos(cliente.post(`/sincronizacion/operaciones/${id}/reintentar`)),
  descartar: (id) => datos(cliente.post(`/sincronizacion/operaciones/${id}/descartar`)),
};

/** Reportes. */
export const reportesApi = {
  resumen: () => datos(cliente.get('/reportes/resumen')),
};
