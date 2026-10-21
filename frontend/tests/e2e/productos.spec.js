/**
 * @file tests/e2e/productos.spec.js
 * @description CP-003: validación Formik + Yup del formulario de productos y guardado exitoso (CA-01.3).
 * @created 21/10/2026
 * @updated 21/10/2026
 * @author Equipo SIGIF
 */
import { expect, test } from '@playwright/test';
import { iniciarSesion, unico } from './utilidades';

test.describe('Formulario de productos', () => {
  test.beforeEach(async ({ page }) => {
    await iniciarSesion(page);
    await page.getByTestId('menu-productos').click();
  });

  test('bloquea el envío cuando faltan campos obligatorios', async ({ page }) => {
    let peticiones = 0;
    page.on('request', (r) => {
      if (r.method() === 'POST' && r.url().includes('/api/v1/productos')) peticiones += 1;
    });
    await page.getByTestId('nuevo-producto').click();
    await page.getByTestId('guardar-producto').click();
    await expect(page.getByTestId('form-producto').getByText('Este campo es obligatorio').first()).toBeVisible();
    await page.getByTestId('campo-precioCompra').fill('0');
    await page.getByTestId('guardar-producto').click();
    await expect(page.getByText('Debe ser mayor que cero')).toBeVisible();
    expect(peticiones).toBe(0);
  });

  test('guarda un producto válido y calcula el precio de venta', async ({ page }) => {
    const codigo = unico('77');
    await page.getByTestId('nuevo-producto').click();
    await page.getByTestId('campo-nombre').fill(`Taladro E2E ${codigo}`);
    await page.getByTestId('campo-codigoBarras').fill(codigo);
    for (const campo of ['categoriaId', 'marcaId', 'presentacionId', 'unidadBaseId']) {
      await page.locator(`#${campo}`).click();
      await page.getByRole('option').first().click();
    }
    await page.getByTestId('campo-precioCompra').fill('250000');
    await page.getByTestId('campo-porcentajeIncremento').fill('30');
    await expect(page.getByTestId('precio-calculado')).toContainText('325');
    await page.getByTestId('guardar-producto').click();
    await expect(page.getByTestId('notificacion')).toContainText('éxito');
    await page.getByLabel(/Buscar/).fill(codigo);
    await expect(page.getByTestId('tabla-productos')).toContainText(codigo);
  });

  test('rechaza un código de barras duplicado con 409', async ({ page }) => {
    const codigo = unico('78');
    const crear = async () => {
      await page.getByTestId('nuevo-producto').click();
      await page.getByTestId('campo-nombre').fill(`Duplicado ${codigo}`);
      await page.getByTestId('campo-codigoBarras').fill(codigo);
      for (const campo of ['categoriaId', 'marcaId', 'presentacionId', 'unidadBaseId']) {
        await page.locator(`#${campo}`).click();
        await page.getByRole('option').first().click();
      }
      await page.getByTestId('campo-precioCompra').fill('1000');
      await page.getByTestId('campo-porcentajeIncremento').fill('10');
      await page.getByTestId('guardar-producto').click();
    };
    await crear();
    await expect(page.getByTestId('notificacion')).toContainText('éxito');
    await crear();
    await expect(page.getByTestId('notificacion')).toContainText(`Ya existe un producto con el código de barras ${codigo}`);
  });
});
