/**
 * @file validation/esquemas.js
 * @description Esquemas Yup de validación de formularios (Formik) con mensajes internacionalizados.
 *              Replican las reglas del Back-End para bloquear envíos inválidos (CA-01.3).
 * @created 21/10/2026
 * @updated 21/10/2026
 * @author Equipo SIGIF
 */
import * as yup from 'yup';

/**
 * @param {Function} t función de traducción
 * @returns {yup.ObjectSchema} esquema de inicio de sesión
 */
export const loginEsquema = (t) => yup.object({
  username: yup.string().trim().required(t('validacion.obligatorio')),
  password: yup.string().required(t('validacion.obligatorio')),
});

/**
 * @param {Function} t función de traducción
 * @returns {yup.ObjectSchema} esquema del formulario de producto (REQ-F-001..005)
 */
export const productoEsquema = (t) => yup.object({
  nombre: yup.string().trim().required(t('validacion.obligatorio')).max(200, t('validacion.maximo', { n: 200 })),
  codigoBarras: yup.string().trim().required(t('validacion.obligatorio'))
    .matches(/^[0-9A-Za-z-]{4,50}$/, t('validacion.codigoBarras')),
  categoriaId: yup.number().typeError(t('validacion.obligatorio')).required(t('validacion.obligatorio')),
  marcaId: yup.number().typeError(t('validacion.obligatorio')).required(t('validacion.obligatorio')),
  presentacionId: yup.number().typeError(t('validacion.obligatorio')).required(t('validacion.obligatorio')),
  unidadBaseId: yup.number().typeError(t('validacion.obligatorio')).required(t('validacion.obligatorio')),
  descripcion: yup.string().max(2000, t('validacion.maximo', { n: 2000 })).nullable(),
  precioCompra: yup.number().typeError(t('validacion.obligatorio')).required(t('validacion.obligatorio'))
    .moreThan(0, t('validacion.positivo')),
  porcentajeIncremento: yup.number().typeError(t('validacion.obligatorio')).required(t('validacion.obligatorio'))
    .min(0, t('validacion.noNegativo')),
  porcentajeIva: yup.number().typeError(t('validacion.noNegativo')).min(0, t('validacion.noNegativo'))
    .max(100, t('validacion.maximo100')),
  tieneGarantia: yup.boolean(),
  mesesGarantia: yup.number().nullable().transform((v, o) => (o === '' ? null : v))
    .when('tieneGarantia', {
      is: true,
      then: (s) => s.required(t('validacion.obligatorio')).integer(t('validacion.entero')).moreThan(0, t('validacion.positivo')),
    }),
  stockMinimo: yup.number().nullable().transform((v, o) => (o === '' ? null : v)).min(0, t('validacion.noNegativo')),
  diasAlertaVencimiento: yup.number().nullable().transform((v, o) => (o === '' ? null : v))
    .integer(t('validacion.entero')).min(0, t('validacion.noNegativo')),
});

/**
 * @param {Function} t función de traducción
 * @returns {yup.ObjectSchema} esquema de terceros (REQ-F-025/026)
 */
export const terceroEsquema = (t) => yup.object({
  tipoPersona: yup.string().oneOf(['NATURAL', 'JURIDICA']).required(t('validacion.obligatorio')),
  tipoDocumento: yup.string().required(t('validacion.obligatorio'))
    .when('tipoPersona', { is: 'JURIDICA', then: (s) => s.oneOf(['NIT'], t('validacion.documento')) }),
  numeroDocumento: yup.string().trim().required(t('validacion.obligatorio'))
    .matches(/^[0-9A-Za-z]{5,20}$/, t('validacion.documento'))
    .when('tipoDocumento', {
      is: (v) => v === 'CC' || v === 'NIT',
      then: (s) => s.matches(/^\d+$/, t('validacion.documento')),
    }),
  digitoVerificacion: yup.string().nullable().matches(/^[0-9]?$/, t('validacion.documento')),
  nombre: yup.string().nullable().when('tipoPersona', { is: 'NATURAL', then: (s) => s.trim().required(t('validacion.obligatorio')) }),
  apellido: yup.string().nullable().when('tipoPersona', { is: 'NATURAL', then: (s) => s.trim().required(t('validacion.obligatorio')) }),
  razonSocial: yup.string().nullable().when('tipoPersona', { is: 'JURIDICA', then: (s) => s.trim().required(t('validacion.obligatorio')) }),
  direccion: yup.string().trim().required(t('validacion.obligatorio')).max(200, t('validacion.maximo', { n: 200 })),
  telefono: yup.string().trim().required(t('validacion.obligatorio')).matches(/^[0-9+()\s-]{7,20}$/, t('validacion.telefono')),
  correo: yup.string().trim().required(t('validacion.obligatorio')).email(t('validacion.correo')),
  esCliente: yup.boolean(),
  esProveedor: yup.boolean().test('rol', t('validacion.rolTercero'), function validarRol(v) {
    return v || this.parent.esCliente;
  }),
});

/**
 * @param {Function} t función de traducción
 * @param {boolean} nuevo true en creación (contraseña obligatoria)
 * @returns {yup.ObjectSchema} esquema de usuario
 */
export const usuarioEsquema = (t, nuevo) => yup.object({
  username: yup.string().trim().required(t('validacion.obligatorio')).min(3).max(60),
  nombreCompleto: yup.string().trim().required(t('validacion.obligatorio')),
  correo: yup.string().trim().required(t('validacion.obligatorio')).email(t('validacion.correo')),
  password: nuevo
    ? yup.string().required(t('validacion.obligatorio')).matches(/^(?=.*[a-z])(?=.*[A-Z])(?=.*\d).{8,72}$/, t('validacion.password'))
    : yup.string().nullable(),
  roles: yup.array().min(1, t('validacion.obligatorio')),
});

/**
 * @param {Function} t función de traducción
 * @param {string} tipo tipo de catálogo
 * @returns {yup.ObjectSchema} esquema de catálogo
 */
export const catalogoEsquema = (t, tipo) => yup.object({
  nombre: yup.string().trim().required(t('validacion.obligatorio')).max(120, t('validacion.maximo', { n: 120 })),
  codigo: tipo === 'unidades-medida'
    ? yup.string().trim().required(t('validacion.obligatorio')).max(10, t('validacion.maximo', { n: 10 }))
    : yup.string().nullable(),
});

/**
 * @param {Function} t función de traducción
 * @returns {yup.ObjectSchema} esquema de cambio de estado de inventario
 */
export const transicionEsquema = (t) => yup.object({
  cantidad: yup.number().typeError(t('validacion.obligatorio')).required(t('validacion.obligatorio')).moreThan(0, t('validacion.positivo')),
  estadoOrigen: yup.string().required(t('validacion.obligatorio')),
  estadoDestino: yup.string().required(t('validacion.obligatorio'))
    .notOneOf([yup.ref('estadoOrigen')], t('validacion.obligatorio')),
  motivo: yup.string().trim().required(t('validacion.obligatorio')).max(300, t('validacion.maximo', { n: 300 })),
});

/**
 * @param {Function} t función de traducción
 * @returns {yup.ObjectSchema} esquema de reserva
 */
export const reservaEsquema = (t) => yup.object({
  clienteId: yup.number().typeError(t('validacion.obligatorio')).required(t('validacion.obligatorio')),
  productoId: yup.number().typeError(t('validacion.obligatorio')).required(t('validacion.obligatorio')),
  cantidad: yup.number().typeError(t('validacion.obligatorio')).required(t('validacion.obligatorio')).moreThan(0, t('validacion.positivo')),
  diasVigencia: yup.number().integer(t('validacion.entero')).min(1).max(60),
});

/**
 * @param {Function} t función de traducción
 * @returns {yup.ObjectSchema} esquema de compra (REQ-F-020)
 */
export const compraEsquema = (t) => yup.object({
  proveedorId: yup.number().typeError(t('validacion.obligatorio')).required(t('validacion.obligatorio')),
  lineas: yup.array().min(1, t('validacion.lineas')).of(yup.object({
    cantidad: yup.number().typeError(t('validacion.obligatorio')).required(t('validacion.obligatorio')).moreThan(0, t('validacion.positivo')),
    costoUnitario: yup.number().typeError(t('validacion.obligatorio')).required(t('validacion.obligatorio')).moreThan(0, t('validacion.positivo')),
    porcentajeIva: yup.number().min(0, t('validacion.noNegativo')).max(100, t('validacion.maximo100')),
    codigoLote: yup.string().nullable().when('tieneVencimiento', { is: true, then: (s) => s.required(t('validacion.obligatorio')) }),
    fechaVencimiento: yup.string().nullable().when('tieneVencimiento', { is: true, then: (s) => s.required(t('validacion.obligatorio')) }),
  })),
});

/**
 * @param {Function} t función de traducción
 * @returns {yup.ObjectSchema} esquema de cierre manual
 */
export const cierreEsquema = (t) => yup.object({
  periodo: yup.string().required(t('validacion.obligatorio')).matches(/^\d{4}(0[1-9]|1[0-2])$/, t('validacion.periodo')),
  forzar: yup.boolean(),
});
