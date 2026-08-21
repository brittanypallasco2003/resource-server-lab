package com.spring.resource.server.lab.infrastructure.config.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "keycloak.admin")
public record KeycloakAdminProperties(
        String host,
        String port,
        String realmMaster,
        String realmName,
        String adminCli,
        String user,
        String password,
        String clientSecret
    ) {

}
