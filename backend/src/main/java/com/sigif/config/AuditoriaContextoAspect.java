/*
 * Nombre: AuditoriaContextoAspect.java
 * Descripción: Propaga el usuario de aplicación a PostgreSQL para los triggers de auditoría.
 * Fecha de creación: 21/10/2026
 * Última actualización: 21/10/2026
 * Autor: Equipo SIGIF
 */
package com.sigif.config;

import com.sigif.common.util.UsuarioActual;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * Antes de ejecutar cualquier método de servicio dentro de una transacción de escritura,
 * fija la variable de sesión {@code sigif.usuario} (local a la transacción) que lee la
 * función {@code fn_usuario_actual()} usada por los triggers (CA-10.1).
 */
@Aspect
@Component
@Order(Ordered.LOWEST_PRECEDENCE)
public class AuditoriaContextoAspect {

    @PersistenceContext
    private EntityManager entityManager;

    /**
     * @param pjp punto de unión del servicio
     * @return resultado del método
     * @throws Throwable propagado del método
     */
    @Around("within(@org.springframework.stereotype.Service *)")
    public Object fijarUsuario(ProceedingJoinPoint pjp) throws Throwable {
        if (TransactionSynchronizationManager.isActualTransactionActive()
                && !TransactionSynchronizationManager.isCurrentTransactionReadOnly()) {
            entityManager.createNativeQuery("SELECT set_config('sigif.usuario', :u, true)")
                    .setParameter("u", UsuarioActual.nombre())
                    .getSingleResult();
        }
        return pjp.proceed();
    }
}
