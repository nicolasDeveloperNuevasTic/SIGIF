/*
 * Nombre: TerceroDtos.java
 * Descripción: DTOs JSON de terceros (clientes y proveedores) con validaciones i18n.
 * Fecha de creación: 21/10/2026
 * Última actualización: 21/10/2026
 * Autor: Equipo SIGIF
 */
package com.sigif.tercero;

import com.sigif.tercero.Tercero.TipoDocumento;
import com.sigif.tercero.Tercero.TipoPersona;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Registros de entrada y salida del módulo de terceros.
 */
public final class TerceroDtos {

    private TerceroDtos() {
    }

    /**
     * @param tipoPersona        NATURAL o JURIDICA
     * @param tipoDocumento      CC, CE, NIT o PAS
     * @param numeroDocumento    número (solo dígitos para CC/NIT)
     * @param digitoVerificacion DV del NIT
     * @param nombre             nombre (obligatorio persona natural)
     * @param apellido           apellido (obligatorio persona natural)
     * @param razonSocial        razón social (obligatoria persona jurídica)
     * @param direccion          dirección
     * @param telefono           teléfono
     * @param correo             correo
     * @param esCliente          es cliente
     * @param esProveedor        es proveedor
     */
    public record TerceroRequest(
            @NotNull(message = "{validacion.obligatorio}") TipoPersona tipoPersona,
            @NotNull(message = "{validacion.obligatorio}") TipoDocumento tipoDocumento,
            @NotBlank(message = "{validacion.obligatorio}")
            @Pattern(regexp = "^[0-9A-Za-z]{5,20}$", message = "{validacion.documento}") String numeroDocumento,
            @Pattern(regexp = "^[0-9]?$", message = "{validacion.dv}") String digitoVerificacion,
            @Size(max = 100, message = "{validacion.longitud}") String nombre,
            @Size(max = 100, message = "{validacion.longitud}") String apellido,
            @Size(max = 200, message = "{validacion.longitud}") String razonSocial,
            @NotBlank(message = "{validacion.obligatorio}") @Size(max = 200, message = "{validacion.longitud}")
            String direccion,
            @NotBlank(message = "{validacion.obligatorio}")
            @Pattern(regexp = "^[0-9+()\\s-]{7,20}$", message = "{validacion.telefono}") String telefono,
            @NotBlank(message = "{validacion.obligatorio}") @Email(message = "{validacion.correo}")
            @Size(max = 150, message = "{validacion.longitud}") String correo,
            boolean esCliente,
            boolean esProveedor) {
    }

    /**
     * @param id                 id
     * @param tipoPersona        tipo de persona
     * @param tipoDocumento      tipo de documento
     * @param numeroDocumento    número
     * @param digitoVerificacion DV
     * @param nombre             nombre
     * @param apellido           apellido
     * @param razonSocial        razón social
     * @param nombreMostrar      nombre para mostrar
     * @param direccion          dirección
     * @param telefono           teléfono
     * @param correo             correo
     * @param esCliente          cliente
     * @param esProveedor        proveedor
     * @param esGenerico         tercero genérico del sistema
     * @param activo             activo
     */
    public record TerceroResponse(Long id, TipoPersona tipoPersona, TipoDocumento tipoDocumento,
                                  String numeroDocumento, String digitoVerificacion, String nombre, String apellido,
                                  String razonSocial, String nombreMostrar, String direccion, String telefono,
                                  String correo, boolean esCliente, boolean esProveedor, boolean esGenerico,
                                  boolean activo) {

        /**
         * @param t entidad
         * @return DTO
         */
        public static TerceroResponse de(Tercero t) {
            return new TerceroResponse(t.getId(), t.getTipoPersona(), t.getTipoDocumento(), t.getNumeroDocumento(),
                    t.getDigitoVerificacion(), t.getNombre(), t.getApellido(), t.getRazonSocial(), t.nombreMostrar(),
                    t.getDireccion(), t.getTelefono(), t.getCorreo(), t.isEsCliente(), t.isEsProveedor(),
                    t.isEsGenerico(), t.isActivo());
        }
    }

    /**
     * Resumen de tercero embebido en documentos.
     *
     * @param id              id
     * @param numeroDocumento documento
     * @param nombre          nombre para mostrar
     * @param direccion       dirección
     * @param telefono        teléfono
     * @param correo          correo
     */
    public record TerceroResumen(Long id, String numeroDocumento, String nombre, String direccion, String telefono,
                                 String correo) {

        /**
         * @param t entidad
         * @return resumen
         */
        public static TerceroResumen de(Tercero t) {
            String doc = t.getDigitoVerificacion() == null || t.getDigitoVerificacion().isBlank()
                    ? t.getNumeroDocumento() : t.getNumeroDocumento() + "-" + t.getDigitoVerificacion();
            return new TerceroResumen(t.getId(), t.getTipoDocumento() + " " + doc, t.nombreMostrar(),
                    t.getDireccion(), t.getTelefono(), t.getCorreo());
        }
    }
}
