package com.spring.resource.server.lab.infrastructure.security;

import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimNames;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.stereotype.Component;

import com.spring.resource.server.lab.infrastructure.config.properties.JwtAuthConverterProperties;

import tools.jackson.databind.ObjectMapper;

/// Turns a validated JWT into the authentication this application works with.
///
/// It merges three sources of authority into one [JwtAuthenticationToken]:
///
/// 1. The scopes, through Spring's own `JwtGrantedAuthoritiesConverter` (`SCOPE_…`).
/// 2. The **client** roles in `resource_access.<resource-id>.roles`.
/// 3. The **realm** roles in `realm_access.roles`.
///
/// Reading both role claims is deliberate. The Keycloak adapter writes client roles, so source 2
/// is the one that carries the roles this API itself grants; source 3 exists so that somebody made
/// an admin directly in the Keycloak console — at realm level, which is what a human naturally
/// reaches for — is also an admin here, instead of silently getting a 403.
///
/// Every role is uppercased before the `ROLE_` prefix goes on, matching what
/// [com.spring.resource.server.lab.domain.validation.DomainValidator#normalizeSet(Set, String)]
/// does on the way in. That is what makes `hasRole("ADMIN")` work whether the role was written
/// `admin`, `Admin` or `ADMIN` in Keycloak. Without it, a casing mismatch is a 403 with no
/// explanation anywhere.
@Component
public class JwtAuthenticationConverter implements Converter<Jwt, AbstractAuthenticationToken> {

    private static final String RESOURCE_ACCESS_CLAIM = "resource_access";
    private static final String REALM_ACCESS_CLAIM = "realm_access";
    private static final String ROLES_CLAIM = "roles";
    private static final String ROLE_PREFIX = "ROLE_";

    private final JwtGrantedAuthoritiesConverter jwtGrantedAuthoritiesConverter;
    private final ObjectMapper objectMapper;

    private final JwtAuthConverterProperties jwtAuthConverterProperties;


    public JwtAuthenticationConverter(JwtGrantedAuthoritiesConverter jwtGrantedAuthoritiesConverter, ObjectMapper objectMapper, JwtAuthConverterProperties jwtAuthConverterProperties) {
        this.jwtGrantedAuthoritiesConverter = jwtGrantedAuthoritiesConverter;
        this.objectMapper = objectMapper;
        this.jwtAuthConverterProperties = jwtAuthConverterProperties;
    }

    @Override
    public AbstractAuthenticationToken convert(Jwt source) {
        Collection<GrantedAuthority> authorities = Stream.of(
                jwtGrantedAuthoritiesConverter.convert(source).stream(),
                extractResourceRoles(source).stream(),
                extractRealmRoles(source).stream()
        ).flatMap(stream -> stream).collect(Collectors.toSet());

        return new JwtAuthenticationToken(source, authorities, getPrincipalClaimName(source));
    }

    private String getPrincipalClaimName(Jwt jwt) {
        String claimName = jwtAuthConverterProperties.principalAttribute() != null ? jwtAuthConverterProperties.principalAttribute() : JwtClaimNames.SUB;
        return jwt.getClaim(claimName);
    }

    /// Client roles: `resource_access.<resource-id>.roles`.
    ///
    /// These are the ones the Keycloak adapter writes when an account is registered, so they are
    /// the roles this API itself grants. A token issued for a different client simply has no entry
    /// under this resource id, and contributes nothing.
    @SuppressWarnings("unchecked")
    private Collection<? extends GrantedAuthority> extractResourceRoles(Jwt jwt) {
        if (jwt.getClaim(RESOURCE_ACCESS_CLAIM) == null) {
            return Set.of();
        }

        Map<String, Object> resourceAccess = jwt.getClaimAsMap(RESOURCE_ACCESS_CLAIM);
        if (!resourceAccess.containsKey(jwtAuthConverterProperties.resourceId())) {
            return Set.of();
        }

        Map<String, Object> resource = castMapResourceId(resourceAccess);
        return toAuthorities((Collection<String>) resource.get(ROLES_CLAIM));
    }

    /// Realm roles: `realm_access.roles`.
    ///
    /// A role granted at realm level applies across every client of the realm, so it is honoured
    /// here too. Keycloak's own built-ins (`offline_access`, `uma_authorization`,
    /// `default-roles-…`) come through as authorities as well; nothing checks them, and filtering
    /// them out would mean this converter keeping a list of Keycloak's internals up to date.
    @SuppressWarnings("unchecked")
    private Collection<? extends GrantedAuthority> extractRealmRoles(Jwt jwt) {
        if (jwt.getClaim(REALM_ACCESS_CLAIM) == null) {
            return Set.of();
        }

        Map<String, Object> realmAccess = jwt.getClaimAsMap(REALM_ACCESS_CLAIM);
        return toAuthorities((Collection<String>) realmAccess.get(ROLES_CLAIM));
    }

    /// Prefixes each role with `ROLE_` and uppercases it, which is the form `hasRole(...)` looks
    /// for and the same casing the domain applies when it stores a role.
    ///
    /// @param roles the raw role names read from a claim; may be null
    /// @return the authorities, or an empty set when there are no roles
    private Collection<? extends GrantedAuthority> toAuthorities(Collection<String> roles) {
        if (roles == null) {
            return Set.of();
        }

        return roles.stream()
                .filter(role -> role != null && !role.isBlank())
                .map(role -> new SimpleGrantedAuthority(ROLE_PREFIX.concat(role.trim().toUpperCase())))
                .collect(Collectors.toSet());
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> castMapResourceId(Map<String, Object> resourceAccess) {
        return objectMapper.convertValue(resourceAccess.get(jwtAuthConverterProperties.resourceId()), Map.class);
    }

}
