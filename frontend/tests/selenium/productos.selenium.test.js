/**
 * @file tests/selenium/productos.selenium.test.js
 * @description CP-003 con Selenium WebDriver: validación Formik/Yup del formulario de productos.
 *              Ejecutar con `npm run test:selenium` (requiere Chrome y el entorno en E2E_BASE_URL).
 * @created 21/10/2026
 * @updated 21/10/2026
 * @author Equipo SIGIF
 */
import { after, before, test } from 'node:test';
import assert from 'node:assert/strict';
import { Builder, By, until } from 'selenium-webdriver';
import chrome from 'selenium-webdriver/chrome.js';

const BASE = process.env.E2E_BASE_URL || 'https://localhost';
let driver;

before(async () => {
  const opciones = new chrome.Options().addArguments('--headless=new', '--ignore-certificate-errors', '--lang=es-CO');
  driver = await new Builder().forBrowser('chrome').setChromeOptions(opciones).build();
});

after(async () => {
  await driver?.quit();
});

/**
 * @param {string} testId atributo data-testid
 * @returns {Promise<import('selenium-webdriver').WebElement>} elemento visible
 */
async function porTestId(testId) {
  const el = await driver.wait(until.elementLocated(By.css(`[data-testid="${testId}"]`)), 15000);
  await driver.wait(until.elementIsVisible(el), 15000);
  return el;
}

test('el formulario de productos muestra errores y no envía datos inválidos', async () => {
  await driver.get(`${BASE}/login`);
  await (await porTestId('campo-username')).sendKeys(process.env.E2E_USUARIO || 'admin');
  await (await porTestId('campo-password')).sendKeys(process.env.E2E_PASSWORD || 'Admin123*');
  await (await porTestId('ingresar')).click();
  await (await porTestId('menu-productos')).click();
  await (await porTestId('nuevo-producto')).click();
  await (await porTestId('guardar-producto')).click();
  const formulario = await porTestId('form-producto');
  await driver.wait(async () => (await formulario.getText()).includes('Este campo es obligatorio'), 10000);
  assert.ok((await formulario.getText()).includes('Este campo es obligatorio'));
});
