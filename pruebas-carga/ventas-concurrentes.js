/**
 * @file pruebas-carga/ventas-concurrentes.js
 * @description CP-006 (k6): ventas concurrentes de alto volumen sobre stock limitado. Verifica cero saldos
 *              negativos y p95 <= 300 ms en búsquedas por código de barras (RNF-001, CA-04.1, CA-06.1).
 *              Ejecutar: k6 run -e BASE_URL=https://localhost pruebas-carga/ventas-concurrentes.js
 * @created 21/10/2026
 * @updated 21/10/2026
 * @author Equipo SIGIF
 */
import http from 'k6/http';
import { check } from 'k6';
import { Counter } from 'k6/metrics';

const BASE = `${__ENV.BASE_URL || 'https://localhost'}/api/v1`;
const STOCK = Number(__ENV.STOCK || 50);
const exitosas = new Counter('ventas_exitosas');
const rechazadas = new Counter('ventas_rechazadas_409');

export const options = {
  insecureSkipTLSVerify: true,
  scenarios: {
    busquedas: { executor: 'constant-vus', vus: 20, duration: '30s', exec: 'buscar' },
    ventas: { executor: 'shared-iterations', vus: 30, iterations: STOCK * 3, exec: 'vender', startTime: '2s' },
  },
  thresholds: {
    'http_req_duration{escenario:busqueda}': ['p(95)<300'],
    http_req_failed: ['rate<0.70'],
  },
};

const json = (token) => ({ headers: { 'Content-Type': 'application/json', Authorization: `Bearer ${token}` } });

/**
 * Prepara un producto con stock limitado.
 * @returns {{token: string, productoId: number, codigo: string}} datos compartidos
 */
export function setup() {
  const login = http.post(`${BASE}/auth/login`, JSON.stringify({ username: __ENV.USUARIO || 'admin', password: __ENV.PASSWORD || 'Admin123*' }),
    { headers: { 'Content-Type': 'application/json' } }).json();
  const token = login.token;
  const codigo = `K6${Date.now()}`;
  const proveedor = http.post(`${BASE}/terceros`, JSON.stringify({
    tipoPersona: 'JURIDICA', tipoDocumento: 'NIT', numeroDocumento: `8${Date.now()}`.slice(0, 12), razonSocial: 'Proveedor k6',
    direccion: 'Calle k6', telefono: '6010000000', correo: 'k6@sigif.local', esCliente: false, esProveedor: true,
  }), json(token)).json();
  const producto = http.post(`${BASE}/productos`, JSON.stringify({
    nombre: `Producto carga ${codigo}`, categoriaId: 1, codigoBarras: codigo, marcaId: 1, presentacionId: 1, unidadBaseId: 1,
    permiteFraccion: false, precioCompra: 1000, porcentajeIncremento: 20, tieneGarantia: false, tieneVencimiento: false,
  }), json(token)).json();
  http.post(`${BASE}/compras`, JSON.stringify({
    proveedorId: proveedor.id, lineas: [{ productoId: producto.id, cantidad: STOCK, costoUnitario: 1000 }],
  }), json(token));
  return { token, productoId: producto.id, codigo };
}

/** Búsqueda por código de barras (p95 <= 300 ms). */
export function buscar(datos) {
  const r = http.get(`${BASE}/productos/codigo-barras/${datos.codigo}`, { ...json(datos.token), tags: { escenario: 'busqueda' } });
  check(r, { 'búsqueda 200': (x) => x.status === 200 });
}

/** Venta de 1 unidad compitiendo por el mismo stock. */
export function vender(datos) {
  const r = http.post(`${BASE}/ventas`, JSON.stringify({ lineas: [{ productoId: datos.productoId, cantidad: 1 }] }), json(datos.token));
  if (r.status === 201) exitosas.add(1);
  if (r.status === 409) rechazadas.add(1);
  check(r, { 'venta 201 o 409': (x) => x.status === 201 || x.status === 409 });
}

/** Verifica que el saldo final sea exactamente 0 y nunca negativo. */
export function teardown(datos) {
  const p = http.get(`${BASE}/productos/${datos.productoId}`, json(datos.token)).json();
  check(p, { 'saldo final = 0 (sin negativos)': (x) => Number(x.existencias.disponible) === 0 });
}
