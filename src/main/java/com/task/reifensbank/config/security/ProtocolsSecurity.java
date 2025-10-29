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
                .securityMatcher("/protocols/**")
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> {
                    auth.requestMatchers(HttpMethod.POST, "/protocols").hasAuthority(AuthorityEnum.CREATE_PROTOCOL.name());
                    auth.requestMatchers(HttpMethod.GET, "/protocols/*").hasAuthority(AuthorityEnum.VIEW_PROTOCOL.name());
                    auth.requestMatchers(HttpMethod.PUT, "/protocols/*").hasAuthority(AuthorityEnum.EDIT_PROTOCOL.name());
                    auth.requestMatchers(HttpMethod.PATCH, "/protocols/*/state").hasAuthority(AuthorityEnum.EDIT_PROTOCOL.name());

                    if (protectProtocols) {
                        auth.anyRequest().denyAll();
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
