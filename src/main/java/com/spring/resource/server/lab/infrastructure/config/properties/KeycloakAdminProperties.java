package com.spring.resource.server.lab.infrastructure.config.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.constraints.NotBlank;

/// Credentials and coordinates of the Keycloak Admin REST client.
///
/// `appClientId` is the `clientId` of *this* resource server inside the target realm — the client
/// whose roles are assigned when an account is registered. It is bound from the same environment
/// variable as `jwt.auth.converter.resource-id`, and that is deliberate: the adapter writes the
/// roles into a client and the converter reads them back out of that same client, so the two
/// naming different clients would mean every user silently ends up with no authorities.
///
/// @param host the Keycloak host, scheme included
/// @param port the Keycloak port
/// @param realmMaster the realm the admin credentials belong to
/// @param realmName the target realm this application manages
/// @param adminCli the client id used to obtain the admin token
/// @param user the admin username
/// @param password the admin password
/// @param clientSecret the secret of the admin client
/// @param appClientId the clientId of this resource server in the target realm
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
        String clientSecret,
        @NotBlank(message = "El clientId de la aplicación no puede estar vacío") String appClientId
    ) {

}
