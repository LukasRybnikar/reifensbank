package com.task.reifensbank.config.security;

import com.task.reifensbank.security.JwtAuthenticationFilter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration(proxyBeanMethods = false)
@ConditionalOnProperty(prefix = "app.security", name = "enabled", havingValue = "true", matchIfMissing = true)
public class ProtocolsSecurity {

    private final JwtAuthenticationFilter jwt;

    public ProtocolsSecurity(JwtAuthenticationFilter jwt) {
        this.jwt = jwt;
    }

    @Bean(name = "protocolsChain")
    @Order(20)
    SecurityFilterChain protocolsChain(
            HttpSecurity http,
            @Value("${app.security.protocols:true}") boolean protectProtocols
    ) throws Exception {

        http
                .securityMatcher("/api/protocols/**")
                .csrf(csrf -> csrf.disable())
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> {
                    if (protectProtocols) {
                        auth.anyRequest().authenticated();
                    } else {
                        auth.anyRequest().permitAll();
                    }
                });

        if (protectProtocols) {
            http.addFilterBefore(jwt, UsernamePasswordAuthenticationFilter.class);
        }

        return http.build();
    }
}
