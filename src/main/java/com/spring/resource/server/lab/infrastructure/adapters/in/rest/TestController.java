package com.spring.resource.server.lab.infrastructure.adapters.in.rest;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/// Three endpoints with no business domain behind them, kept as a bench for the authorization
/// rules.
///
/// Each one exercises a different level of the policy, so a token can be checked against all three
/// in a row: no token at all, any valid token, and a token carrying `ROLE_ADMIN`. That is the
/// cheapest way to confirm that
/// [com.spring.resource.server.lab.infrastructure.security.JwtAuthenticationConverter] really
/// produced the authority `@PreAuthorize` is looking for.
@RestController
@RequestMapping("/test")
public class TestController {

    /// Open to anyone: `SecurityConfig` lets this path through without a token.
    /// @return String a fixed greeting
    @GetMapping("/public")
    public String publicEndpoint() {
        return "Hello World!";
    }

    /// Requires a valid token and nothing else — no role is checked.
    /// @return String a fixed greeting
    @GetMapping("/private")
    public String privateEndpoint() {
        return "Hello private";
    }

    /// Requires the `ADMIN` role.
    ///
    /// Uppercase, because that is how the domain stores roles and how the converter emits them;
    /// `hasRole('admin')` would look for `ROLE_admin` and never match.
    ///
    /// @return String a fixed greeting
    @GetMapping("/private/admin")
    @PreAuthorize("hasRole('ADMIN')")
    public String adminEndpoint() {
        return "Hello admin";
    }

}
