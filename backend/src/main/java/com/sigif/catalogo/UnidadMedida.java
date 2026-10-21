/*
 * Nombre: UnidadMedida.java
 * Descripción: Entidad de unidad de medida usada en inventario y conversiones.
 * Fecha de creación: 21/10/2026
 * Última actualización: 21/10/2026
 * Autor: Equipo SIGIF
 */
package com.sigif.catalogo;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

/**
 * Unidad de medida (UND, MT, KG, LT...).
 */
@Entity
@Getter
@Setter
@Table(name = "unidad_medida")
public class UnidadMedida extends CatalogoBase {

    private String codigo;
}
