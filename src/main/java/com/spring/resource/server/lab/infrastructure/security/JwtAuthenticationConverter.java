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

@Component
public class JwtAuthenticationConverter implements Converter<Jwt, AbstractAuthenticationToken> {

    private static final String RESOURCE_ACCESS_CLAIM = "resource_access";
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
        Collection<GrantedAuthority> authorities = Stream.concat(
                jwtGrantedAuthoritiesConverter.convert(source).stream(),
                extractResourceRoles(source).stream()
        ).collect(Collectors.toSet());

        return new JwtAuthenticationToken(source, authorities, getPrincipalClaimName(source));
    }

    private String getPrincipalClaimName(Jwt jwt) {
        String claimName = jwtAuthConverterProperties.principalAttribute() != null ? jwtAuthConverterProperties.principalAttribute() : JwtClaimNames.SUB;
        return jwt.getClaim(claimName);
    }

    @SuppressWarnings("unchecked")
    private Collection<? extends GrantedAuthority> extractResourceRoles(Jwt jwt) {
        if (jwt.getClaim(RESOURCE_ACCESS_CLAIM) == null) {
            return Set.of();
        }

        Map<String, Object> resourceAccess = jwt.getClaimAsMap(RESOURCE_ACCESS_CLAIM);
        if (!resourceAccess.containsKey(jwtAuthConverterProperties.resourceId())) return Set.of();
    

        Map<String, Object> resource = castMapResourceId(resourceAccess);
        Collection<String> resourceRoles = (Collection<String>) resource.get(ROLES_CLAIM);
        if (resourceRoles == null) {
            return Set.of();
        }

        return resourceRoles.stream()
                .map(role -> new SimpleGrantedAuthority(ROLE_PREFIX.concat(role)))
                .collect(Collectors.toSet());
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> castMapResourceId(Map<String, Object> resourceAccess) {
        return objectMapper.convertValue(resourceAccess.get(jwtAuthConverterProperties.resourceId()), Map.class);
    }

}
