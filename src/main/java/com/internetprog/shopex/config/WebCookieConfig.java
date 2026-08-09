package com.internetprog.shopex.config;

import org.springframework.boot.web.server.servlet.CookieSameSiteSupplier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Session cookie hardening, following OWASP's session-management guidance.
 * The container already marks JSESSIONID HttpOnly; SameSite=Lax additionally
 * stops it from riding along on cross-site requests, which is a second line of
 * defence behind the CSRF tokens.
 */
@Configuration
public class WebCookieConfig {

    @Bean
    public CookieSameSiteSupplier laxSameSiteCookies() {
        return CookieSameSiteSupplier.ofLax();
    }
}
