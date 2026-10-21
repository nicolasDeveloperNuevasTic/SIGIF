/**
 * @file tests/e2e/utilidades.js
 * @description Funciones comunes de las pruebas E2E (inicio de sesión y datos únicos).
 * @created 21/10/2026
 * @updated 21/10/2026
 * @author Equipo SIGIF
 */
import { expect } from '@playwright/test';

/** Credenciales del administrador del entorno de pruebas. */
export const ADMIN = { usuario: process.env.E2E_USUARIO || 'admin', password: process.env.E2E_PASSWORD || 'Admin123*' };

/**
 * Inicia sesión desde la pantalla de login.
 * @param {import('@playwright/test').Page} page página
 * @param {string} [usuario] usuario
 * @param {string} [password] contraseña
 * @returns {Promise<void>}
 */
export async function iniciarSesion(page, usuario = ADMIN.usuario, password = ADMIN.password) {
  await page.goto('/login');
  await page.getByTestId('campo-username').fill(usuario);
  await page.getByTestId('campo-password').fill(password);
  await page.getByTestId('ingresar').click();
  await expect(page.getByTestId('salir')).toBeVisible();
}

/**
 * @param {string} prefijo prefijo
 * @returns {string} código único para datos de prueba
 */
export const unico = (prefijo) => `${prefijo}${Date.now().toString().slice(-9)}`;
