package com.otbs.apigw.config;

import com.otbs.apigw.security.JwtAuthWebFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.web.server.SecurityWebFiltersOrder;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {

    @Bean
    public SecurityWebFilterChain securityWebFilterChain(ServerHttpSecurity http,
                                                         JwtAuthWebFilter jwtFilter) {
        return http
                .authorizeExchange(exchanges -> exchanges
                        .pathMatchers("/api/v1/auth/**").permitAll()
                        .pathMatchers("/api/v1/leave/**").permitAll()
                        .pathMatchers("/api/v1/employee/**").permitAll()
                        .pathMatchers("/api/v1/medical-visits/**").permitAll()
                        .pathMatchers("/api/v1/trainings/**").permitAll()
                        .pathMatchers("/api/v1/invitations/**").permitAll()
                        .pathMatchers("/api/v1/appointments/**").permitAll()
                        .pathMatchers("/api/v1/notifications/**").permitAll()
                        .pathMatchers("/ws-notifications/**").permitAll()
                        .anyExchange().authenticated()
                )
                .addFilterAt(jwtFilter, SecurityWebFiltersOrder.AUTHENTICATION)
                .csrf(ServerHttpSecurity.CsrfSpec::disable)
                .formLogin(ServerHttpSecurity.FormLoginSpec::disable)
                .httpBasic(ServerHttpSecurity.HttpBasicSpec::disable)
                .build();
    }
}
