package com.spring.resource.server.lab.infrastructure.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

import com.spring.resource.server.lab.infrastructure.security.JwtAuthenticationConverter;

/// The application's only security configuration.
///
/// Authorization is decided at **two levels, on purpose**:
///
/// 1. **Here, in the filter chain** — the coarse gate. Which URLs need a token at all. It runs
///    before the request ever reaches a controller, and it is the only place that can say
///    "anonymous is fine" ([#PUBLIC_PATH]).
/// 2. **In the controllers, with `@PreAuthorize`** — which role each individual operation needs.
///    Reading the list of users and deleting one are not the same privilege, and expressing that
///    with URL matchers would mean encoding the HTTP verb into the security config.
///
/// `@EnableMethodSecurity` is what turns level 2 on. **Without it every `@PreAuthorize` in the
/// codebase is silently ignored** — the annotations still compile, the endpoints still answer, and
/// nothing warns you. That silence is why the annotation lives here, next to the chain, instead of
/// on a configuration class of its own.
///
/// CSRF is disabled and sessions are `STATELESS`: authentication travels in the `Authorization`
/// header on every request, so there is no session cookie for an attacker to ride.
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    /// The only route that answers without a token.
    private static final String PUBLIC_PATH = "/api/public";

    private final JwtAuthenticationConverter jwtAuthenticationConverter;

    public SecurityConfig(JwtAuthenticationConverter jwtAuthenticationConverter) {
        this.jwtAuthenticationConverter = jwtAuthenticationConverter;
    }

    /// Builds the only filter chain: [#PUBLIC_PATH] is open, everything else needs a valid token.
    ///
    /// Nothing here mentions a role. That is deliberate — the role each operation requires is
    /// declared on the operation itself with `@PreAuthorize`, so a reader of a controller method
    /// can see its own policy without opening this file, and adding an endpoint cannot leave a
    /// matcher behind.
    ///
    /// @param http the builder Spring Security hands in
    /// @return SecurityFilterChain the configured chain
    /// @throws Exception if the chain cannot be built
    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers(PUBLIC_PATH).permitAll()
                        .anyRequest().authenticated())
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .oauth2ResourceServer(oauth2 -> oauth2
                        .jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter)));
        return http.build();
    }

}
