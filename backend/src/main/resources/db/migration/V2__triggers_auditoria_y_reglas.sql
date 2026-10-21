-- =============================================================================
-- Nombre: V2__triggers_auditoria_y_reglas.sql
-- Descripción: Triggers PostgreSQL de auditoría universal (RN-014), bloqueo de
--              periodos cerrados (RN-011) e inmutabilidad de movimientos.
-- Fecha de creación: 21/10/2026
-- Última actualización: 21/10/2026
-- Autor: Equipo SIGIF
-- =============================================================================

-- Usuario de aplicación propagado por el backend con set_config('sigif.usuario', ..., true)
CREATE OR REPLACE FUNCTION fn_usuario_actual() RETURNS VARCHAR AS $$
BEGIN
    RETURN coalesce(nullif(current_setting('sigif.usuario', true), ''), session_user);
END;
$$ LANGUAGE plpgsql STABLE;

-- Calcula el periodo contable (AAAAMM) en la zona horaria del negocio.
CREATE OR REPLACE FUNCTION fn_periodo(p_fecha TIMESTAMPTZ) RETURNS INTEGER AS $$
BEGIN
    RETURN to_char(p_fecha AT TIME ZONE 'America/Bogota', 'YYYYMM')::INTEGER;
END;
$$ LANGUAGE plpgsql IMMUTABLE;

-- -----------------------------------------------------------------------------
-- Auditoría genérica: guarda usuario, fecha/hora, operación, valores anteriores
-- y nuevos. Nunca persiste hashes de contraseñas ni de API keys.
-- TG_ARGV[0] (opcional) indica la columna que identifica el registro.
-- -----------------------------------------------------------------------------
CREATE OR REPLACE FUNCTION fn_auditoria() RETURNS TRIGGER AS $$
DECLARE
    v_anterior JSONB;
    v_nuevo    JSONB;
    v_id       TEXT;
BEGIN
    IF TG_OP IN ('UPDATE', 'DELETE') THEN
        v_anterior := to_jsonb(OLD) - 'password_hash' - 'api_key_hash';
    END IF;
    IF TG_OP IN ('INSERT', 'UPDATE') THEN
        v_nuevo := to_jsonb(NEW) - 'password_hash' - 'api_key_hash';
    END IF;
    IF TG_OP = 'UPDATE' AND v_anterior = v_nuevo THEN
        RETURN NEW;
    END IF;
    v_id := coalesce(v_nuevo, v_anterior) ->> coalesce(TG_ARGV[0], 'id');

    INSERT INTO auditoria (tabla, operacion, registro_id, usuario, datos_anteriores, datos_nuevos)
    VALUES (TG_TABLE_NAME, TG_OP, v_id, fn_usuario_actual(), v_anterior, v_nuevo);

    RETURN coalesce(NEW, OLD);
END;
$$ LANGUAGE plpgsql;

DO $$
DECLARE
    t TEXT;
BEGIN
    FOREACH t IN ARRAY ARRAY['categoria', 'marca', 'presentacion', 'unidad_medida', 'producto',
                             'conversion_unidad', 'historial_precio', 'tercero', 'usuario', 'rol',
                             'documento_ingreso', 'detalle_ingreso', 'factura', 'detalle_factura',
                             'lote', 'reserva', 'movimiento_inventario', 'cierre_mensual',
                             'sincronizacion_operacion']
    LOOP
        EXECUTE format('CREATE TRIGGER trg_auditoria_%1$s AFTER INSERT OR UPDATE OR DELETE ON %1$s
                        FOR EACH ROW EXECUTE FUNCTION fn_auditoria()', t);
    END LOOP;
END $$;

CREATE TRIGGER trg_auditoria_inventario_saldo AFTER INSERT OR UPDATE OR DELETE ON inventario_saldo
    FOR EACH ROW EXECUTE FUNCTION fn_auditoria('producto_id');
CREATE TRIGGER trg_auditoria_usuario_rol AFTER INSERT OR UPDATE OR DELETE ON usuario_rol
    FOR EACH ROW EXECUTE FUNCTION fn_auditoria('usuario_id');
CREATE TRIGGER trg_auditoria_rol_permiso AFTER INSERT OR UPDATE OR DELETE ON rol_permiso
    FOR EACH ROW EXECUTE FUNCTION fn_auditoria('rol_id');
CREATE TRIGGER trg_auditoria_consecutivo AFTER INSERT OR UPDATE OR DELETE ON consecutivo
    FOR EACH ROW EXECUTE FUNCTION fn_auditoria('codigo');

-- -----------------------------------------------------------------------------
-- La bitácora de auditoría es de solo inserción (no repudio).
-- -----------------------------------------------------------------------------
CREATE OR REPLACE FUNCTION fn_rechazar_modificacion() RETURNS TRIGGER AS $$
BEGIN
    RAISE EXCEPTION '%', TG_ARGV[0] USING ERRCODE = 'P0001';
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_auditoria_inmutable BEFORE UPDATE OR DELETE ON auditoria
    FOR EACH ROW EXECUTE FUNCTION fn_rechazar_modificacion('SIGIF_AUDITORIA_INMUTABLE');

-- Los movimientos de inventario no se modifican ni se eliminan.
CREATE TRIGGER trg_movimiento_inmutable BEFORE UPDATE OR DELETE ON movimiento_inventario
    FOR EACH ROW EXECUTE FUNCTION fn_rechazar_modificacion('SIGIF_MOVIMIENTO_INMUTABLE');

-- Un cierre mensual no se puede modificar ni eliminar una vez registrado.
CREATE TRIGGER trg_cierre_inmutable BEFORE UPDATE OR DELETE ON cierre_mensual
    FOR EACH ROW EXECUTE FUNCTION fn_rechazar_modificacion('SIGIF_CIERRE_INMUTABLE');

-- -----------------------------------------------------------------------------
-- Asignación de periodo y bloqueo de periodos cerrados (REQ-F-028, CA-08.1)
-- -----------------------------------------------------------------------------
CREATE OR REPLACE FUNCTION fn_bloquear_periodo_cerrado() RETURNS TRIGGER AS $$
DECLARE
    v_periodo INTEGER;
BEGIN
    IF TG_OP = 'DELETE' THEN
        v_periodo := OLD.periodo;
    ELSE
        NEW.periodo := fn_periodo(NEW.fecha);
        v_periodo := NEW.periodo;
    END IF;

    IF EXISTS (SELECT 1 FROM cierre_mensual WHERE periodo = v_periodo)
       OR (TG_OP = 'UPDATE' AND EXISTS (SELECT 1 FROM cierre_mensual WHERE periodo = OLD.periodo)) THEN
        RAISE EXCEPTION 'SIGIF_PERIODO_CERRADO:%', v_periodo USING ERRCODE = 'P0001';
    END IF;

    IF TG_OP = 'DELETE' THEN
        RETURN OLD;
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_periodo_movimiento BEFORE INSERT OR UPDATE OR DELETE ON movimiento_inventario
    FOR EACH ROW EXECUTE FUNCTION fn_bloquear_periodo_cerrado();
CREATE TRIGGER trg_periodo_factura BEFORE INSERT OR UPDATE OR DELETE ON factura
    FOR EACH ROW EXECUTE FUNCTION fn_bloquear_periodo_cerrado();
CREATE TRIGGER trg_periodo_documento_ingreso BEFORE INSERT OR UPDATE OR DELETE ON documento_ingreso
    FOR EACH ROW EXECUTE FUNCTION fn_bloquear_periodo_cerrado();
