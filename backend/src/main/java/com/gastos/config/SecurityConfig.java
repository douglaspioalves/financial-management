package com.gastos.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            // Desativa CSRF — API stateless, usa JWT
            .csrf(AbstractHttpConfigurer::disable)
            // Sessão stateless
            .sessionManagement(session ->
                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            )
            .authorizeHttpRequests(auth -> auth
                // Rotas públicas
                .requestMatchers("/api/health", "/api/auth/**").permitAll()
                // TODO: adicionar filtro JWT — por enquanto, permite tudo para desenvolvimento
                .anyRequest().permitAll()
            );

        return http.build();
    }
}
