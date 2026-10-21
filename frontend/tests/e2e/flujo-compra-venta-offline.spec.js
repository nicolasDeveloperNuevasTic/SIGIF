/**
 * @file tests/e2e/flujo-compra-venta-offline.spec.js
 * @description CP-007 y CP-010: compra con Documento de Ingreso, venta con Factura y venta sin internet
 *              con sincronización al reconectar (CA-05.1, CA-06.1, CA-12.1).
 * @created 21/10/2026
 * @updated 21/10/2026
 * @author Equipo SIGIF
 */
import { expect, test } from '@playwright/test';
import { iniciarSesion, unico } from './utilidades';

/**
 * Crea un proveedor y un producto vía API usando el token de la sesión del navegador.
 * @param {import('@playwright/test').Page} page página autenticada
 * @returns {Promise<{codigo: string, productoId: number, proveedorId: number}>} datos creados
 */
async function prepararDatos(page) {
  const token = await page.evaluate(() => JSON.parse(localStorage.getItem('sigif.sesion')).token);
  const headers = { Authorization: `Bearer ${token}` };
  const codigo = unico('79');
  const proveedor = await (await page.request.post('/api/v1/terceros', {
    headers,
    data: {
      tipoPersona: 'JURIDICA', tipoDocumento: 'NIT', numeroDocumento: unico('9'), razonSocial: 'Proveedor E2E SAS',
      direccion: 'Calle 1', telefono: '6015551234', correo: 'e2e@proveedor.co', esCliente: false, esProveedor: true,
    },
  })).json();
  const producto = await (await page.request.post('/api/v1/productos', {
    headers,
    data: {
      nombre: `Martillo E2E ${codigo}`, categoriaId: 1, codigoBarras: codigo, marcaId: 1, presentacionId: 1, unidadBaseId: 1,
      permiteFraccion: false, precioCompra: 10000, porcentajeIncremento: 50, porcentajeIva: 0,
      tieneGarantia: true, mesesGarantia: 6, tieneVencimiento: false,
    },
  })).json();
  return { codigo, productoId: producto.id, proveedorId: proveedor.id, proveedor: proveedor.razonSocial };
}

test('compra genera Documento de Ingreso y venta genera Factura', async ({ page }) => {
  await iniciarSesion(page);
  const datos = await prepararDatos(page);

  await page.goto('/compras/nueva');
  await page.getByTestId('selector-proveedor').fill('Proveedor E2E');
  await page.getByRole('option').first().click();
  await page.getByTestId('buscador-producto').fill(datos.codigo);
  await page.getByTestId('buscador-producto').press('Enter');
  await page.locator('input[name="lineas.0.cantidad"]').fill('5');
  await page.getByTestId('registrar-compra').click();
  await expect(page.getByTestId('numero-documento')).toContainText('DI-');

  await page.goto('/ventas');
  await page.getByTestId('buscador-producto').fill(datos.codigo);
  await page.getByTestId('buscador-producto').press('Enter');
  await page.getByTestId('cantidad-0').fill('2');
  await expect(page.getByTestId('total-venta')).toContainText('30.000');
  await page.getByTestId('facturar').click();
  await expect(page.getByTestId('numero-documento')).toContainText('FV-');
  await expect(page.getByText('Garantía: 6m')).toBeVisible();
});

test('venta sin internet se guarda localmente y se sincroniza al reconectar', async ({ page, context }) => {
  await iniciarSesion(page);
  const datos = await prepararDatos(page);
  const token = await page.evaluate(() => JSON.parse(localStorage.getItem('sigif.sesion')).token);
  await page.request.post('/api/v1/compras', {
    headers: { Authorization: `Bearer ${token}` },
    data: { proveedorId: datos.proveedorId, lineas: [{ productoId: datos.productoId, cantidad: 3, costoUnitario: 10000 }] },
  });

  await page.goto('/sincronizacion');
  await page.getByRole('button', { name: /Actualizar catálogo/ }).click();
  await expect(page.getByTestId('notificacion')).toBeVisible();

  await page.goto('/ventas');
  await context.setOffline(true);
  await expect(page.getByTestId('estado-conexion')).toContainText('Sin conexión');
  await page.getByTestId('buscador-producto').fill(datos.codigo);
  await page.getByTestId('buscador-producto').press('Enter');
  await page.getByTestId('facturar').click();
  await expect(page.getByTestId('venta-offline')).toBeVisible();
  await page.getByRole('button', { name: 'Cerrar' }).click();

  const sincronizacion = page.waitForResponse((r) => r.url().includes('/sincronizacion/ventas') && r.status() === 200);
  await context.setOffline(false);
  const respuesta = await (await sincronizacion).json();
  expect(respuesta[0].estado).toBe('APLICADA');
  expect(respuesta[0].numeroFactura).toMatch(/^FV-/);

  const saldo = await (await page.request.get(`/api/v1/productos/${datos.productoId}`, {
    headers: { Authorization: `Bearer ${token}` },
  })).json();
  expect(saldo.existencias.disponible).toBe(2);
});
