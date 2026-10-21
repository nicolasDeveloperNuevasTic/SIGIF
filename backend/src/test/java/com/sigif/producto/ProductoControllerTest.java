/*
 * Nombre: ProductoControllerTest.java
 * Descripción: Pruebas del contrato REST de productos: 201, 409 i18n (es/en), 400, 401 y 403 (CP-001, CP-002, CP-008).
 * Fecha de creación: 21/10/2026
 * Última actualización: 21/10/2026
 * Autor: Equipo SIGIF
 */
package com.sigif.producto;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.sigif.auditoria.AuditoriaService;
import com.sigif.common.excepcion.ConflictoException;
import com.sigif.common.excepcion.RecursoNoEncontradoException;
import com.sigif.common.util.Mensajes;
import com.sigif.config.I18nConfig;
import com.sigif.producto.ProductoDtos.Existencias;
import com.sigif.producto.ProductoDtos.ProductoResponse;
import com.sigif.security.JwtService;
import com.sigif.security.ManejadorErroresSeguridad;
import com.sigif.security.SecurityConfig;
import com.sigif.usuario.UsuarioRepository;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Prueba la capa web con seguridad real (filtros JWT/API Key, RBAC por permisos) y mensajes i18n.
 */
@WebMvcTest(ProductoController.class)
@Import({SecurityConfig.class, ManejadorErroresSeguridad.class, JwtService.class, Mensajes.class, I18nConfig.class})
class ProductoControllerTest {

    private static final String JSON_VALIDO = """
            {"nombre":"Taladro Percutor Industrial 1/2 pulgada 850W","categoriaId":1,
             "descripcion":"Taladro percutor velocidad variable reversible con maletín.",
             "codigoBarras":"7701234567890","marcaId":1,"presentacionId":1,"unidadBaseId":1,
             "permiteFraccion":false,"precioCompra":250000.00,"porcentajeIncremento":30.00,
             "tieneGarantia":true,"mesesGarantia":12,"tieneVencimiento":false}
            """;

    @Autowired
    private MockMvc mvc;

    @MockBean
    private ProductoService productoService;
    @MockBean
    private ConversionService conversionService;
    @MockBean
    private AuditoriaService auditoriaService;
    @MockBean
    private UsuarioRepository usuarioRepository;

    @Test
    @WithMockUser(authorities = "PRODUCTO_ESCRIBIR")
    void creaProductoYResponde201() throws Exception {
        when(productoService.crear(any(ProductoDtos.ProductoRequest.class))).thenReturn(respuesta());
        mvc.perform(post("/api/v1/productos").contentType(MediaType.APPLICATION_JSON).content(JSON_VALIDO))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1001))
                .andExpect(jsonPath("$.precioVentaCalculado").value(325000.00))
                .andExpect(jsonPath("$.estado").value("ACTIVO"));
    }

    @Test
    @WithMockUser(authorities = "PRODUCTO_ESCRIBIR")
    void codigoDuplicadoResponde409EnEspanol() throws Exception {
        when(productoService.crear(any(ProductoDtos.ProductoRequest.class)))
                .thenThrow(new ConflictoException("producto.codigo.duplicado", "7701234567890"));
        mvc.perform(post("/api/v1/productos").header("Accept-Language", "es")
                        .contentType(MediaType.APPLICATION_JSON).content(JSON_VALIDO))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.codigo").value("producto.codigo.duplicado"))
                .andExpect(jsonPath("$.mensaje").value("Ya existe un producto con el código de barras 7701234567890."));
    }

    @Test
    @WithMockUser(authorities = "PRODUCTO_ESCRIBIR")
    void codigoDuplicadoResponde409EnIngles() throws Exception {
        when(productoService.crear(any(ProductoDtos.ProductoRequest.class)))
                .thenThrow(new ConflictoException("producto.codigo.duplicado", "7701234567890"));
        mvc.perform(post("/api/v1/productos").header("Accept-Language", "en")
                        .contentType(MediaType.APPLICATION_JSON).content(JSON_VALIDO))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.mensaje").value("A product with barcode 7701234567890 already exists."));
    }

    @Test
    @WithMockUser(authorities = "PRODUCTO_ESCRIBIR")
    void camposObligatoriosVaciosResponden400() throws Exception {
        mvc.perform(post("/api/v1/productos").header("Accept-Language", "es")
                        .contentType(MediaType.APPLICATION_JSON).content("{\"nombre\":\"\",\"precioCompra\":0}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo").value("error.validacion"))
                .andExpect(jsonPath("$.errores[?(@.campo=='nombre')].mensaje").value("Este campo es obligatorio."))
                .andExpect(jsonPath("$.errores[?(@.campo=='precioCompra')].mensaje").value("Debe ser mayor que cero."));
    }

    @Test
    @WithMockUser(authorities = "PRODUCTO_LEER")
    void consultaPorIdInexistenteResponde404() throws Exception {
        when(productoService.obtener(99L)).thenThrow(new RecursoNoEncontradoException("recurso.producto", 99L));
        mvc.perform(get("/api/v1/productos/99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.mensaje").value("No se encontró el producto con identificador 99."));
    }

    @Test
    @WithMockUser(authorities = "PRODUCTO_LEER")
    void consultaPorCodigoDeBarras() throws Exception {
        when(productoService.porCodigoBarras("7701234567890")).thenReturn(respuesta());
        mvc.perform(get("/api/v1/productos/codigo-barras/7701234567890"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.codigoBarras").value("7701234567890"));
    }

    @Test
    @WithMockUser(authorities = "PRODUCTO_ESCRIBIR")
    void eliminarResponde204() throws Exception {
        mvc.perform(delete("/api/v1/productos/1001")).andExpect(status().isNoContent());
        verify(productoService).eliminar(1001L);
    }

    @Test
    void sinAutenticacionResponde401() throws Exception {
        mvc.perform(get("/api/v1/productos")).andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.codigo").value("error.no.autenticado"));
    }

    @Test
    @WithMockUser(authorities = "PRODUCTO_LEER")
    void rolSinPermisoDeEscrituraResponde403() throws Exception {
        mvc.perform(post("/api/v1/productos").contentType(MediaType.APPLICATION_JSON).content(JSON_VALIDO))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.codigo").value("error.acceso.denegado"));
        verify(auditoriaService).registrarEventoSeguridad(any(), any(), any());
    }

    private static ProductoResponse respuesta() {
        return new ProductoResponse(1001L, "Taladro Percutor Industrial 1/2 pulgada 850W", "Herramientas Eléctricas",
                "Taladro percutor", "ACTIVO", "7701234567890", "DeWalt", "Unidad", false, new BigDecimal("250000.00"),
                new BigDecimal("30.00"), new BigDecimal("325000.00"), true, 12, false, null, 1L, 1L, 1L, 1L, "UND",
                BigDecimal.ZERO, 30, BigDecimal.ZERO,
                new Existencias(BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO), null);
    }
}
