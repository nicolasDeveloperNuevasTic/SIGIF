/*
 * Nombre: I18nConfig.java
 * Descripción: Configuración de internacionalización (Accept-Language) y validación i18n.
 * Fecha de creación: 21/10/2026
 * Última actualización: 21/10/2026
 * Autor: Equipo SIGIF
 */
package com.sigif.config;

import java.util.List;
import java.util.Locale;
import org.springframework.context.MessageSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;
import org.springframework.web.servlet.LocaleResolver;
import org.springframework.web.servlet.i18n.AcceptHeaderLocaleResolver;

/**
 * Resuelve el idioma desde la cabecera Accept-Language (español por defecto, inglés soportado)
 * y conecta los mensajes de Bean Validation con {@code i18n/messages}.
 */
@Configuration
public class I18nConfig {

    /** Idioma por defecto del sistema. */
    public static final Locale ESPANOL = Locale.forLanguageTag("es");

    /**
     * @return resolvedor por cabecera Accept-Language
     */
    @Bean
    public LocaleResolver localeResolver() {
        AcceptHeaderLocaleResolver resolver = new AcceptHeaderLocaleResolver();
        resolver.setSupportedLocales(List.of(ESPANOL, Locale.ENGLISH));
        resolver.setDefaultLocale(ESPANOL);
        return resolver;
    }

    /**
     * @param messageSource fuente de mensajes de la aplicación
     * @return validador que traduce las claves {@code {validacion.*}}
     */
    @Bean
    public LocalValidatorFactoryBean validator(MessageSource messageSource) {
        LocalValidatorFactoryBean bean = new LocalValidatorFactoryBean();
        bean.setValidationMessageSource(messageSource);
        return bean;
    }
}
