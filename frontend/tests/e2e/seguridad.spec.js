/**
 * @file tests/e2e/seguridad.spec.js
 * @description CP-008 (E2E): restricción de pantallas por rol (CA-09.2) e i18n de la interfaz.
 * @created 21/10/2026
 * @updated 21/10/2026
 * @author Equipo SIGIF
 */
import { expect, test } from '@playwright/test';
import { iniciarSesion } from './utilidades';

test('el cajero no ve ni accede a la administración de usuarios', async ({ page }) => {
  await iniciarSesion(page, 'cajero', 'Sigif123*');
  await expect(page.getByTestId('menu-ventas')).toBeVisible();
  await expect(page.getByTestId('menu-usuarios')).toHaveCount(0);
  await page.goto('/usuarios');
  await expect(page.getByTestId('sin-permiso')).toBeVisible();
});

test('credenciales inválidas muestran el error del servidor', async ({ page }) => {
  await page.goto('/login');
  await page.getByTestId('campo-username').fill('admin');
  await page.getByTestId('campo-password').fill('incorrecta');
  await page.getByTestId('ingresar').click();
  await expect(page.getByTestId('error-login')).toContainText('Usuario o contraseña incorrectos');
});

test('la interfaz cambia a inglés', async ({ page }) => {
  await iniciarSesion(page);
  await page.getByLabel('Idioma').click();
  await page.getByRole('option', { name: 'EN' }).click();
  await expect(page.getByTestId('menu-productos')).toContainText('Products');
});
