package com.task.reifensbank.config.security;

import com.task.reifensbank.enums.AuthorityEnum;
import com.task.reifensbank.security.JwtAuthenticationFilter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration(proxyBeanMethods = false)
@ConditionalOnProperty(prefix = "app.security", name = "enabled", havingValue = "true", matchIfMissing = true)
public class DocumentsSecurity {

    private final JwtAuthenticationFilter jwt;

    public DocumentsSecurity(JwtAuthenticationFilter jwt) {
        this.jwt = jwt;
    }

    @Bean(name = "documentsChain")
    @Order(10)
    SecurityFilterChain documentsChain(
            HttpSecurity http,
            @Value("${app.security.documents:true}") boolean protectDocs
    ) throws Exception {

        http
                .securityMatcher("/documents/**")
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> {
                    auth.requestMatchers(HttpMethod.POST, "/documents").hasAuthority(AuthorityEnum.CREATE_DOCUMENT.name());
                    auth.requestMatchers(HttpMethod.PATCH, "/documents/*").hasAuthority(AuthorityEnum.EDIT_DOCUMENT.name());
                    if (protectDocs) {
                        auth.anyRequest().denyAll();
                    } else {
                        auth.anyRequest().permitAll();
                    }
                });

        if (protectDocs) {
            http.addFilterBefore(jwt, UsernamePasswordAuthenticationFilter.class);
        }

        return http.build();
    }
}
