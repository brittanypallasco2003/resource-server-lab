package com.spring.resource.server.lab.infrastructure.adapters.out.keycloak;

import org.jboss.resteasy.client.jaxrs.internal.ResteasyClientBuilderImpl;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.KeycloakBuilder;
import org.keycloak.admin.client.resource.RealmResource;
import org.springframework.stereotype.Component;

import com.spring.resource.server.lab.infrastructure.config.properties.KeycloakAdminProperties;

@Component
public class KeycloakProvider {

    private final KeycloakAdminProperties keycloakAdminProperties;

    public KeycloakProvider(KeycloakAdminProperties keycloakAdminProperties) {
        this.keycloakAdminProperties = keycloakAdminProperties;
    }

    /// Builds an authenticated Admin REST client and returns the resource of the target realm.
    ///
    /// A fresh `Keycloak` client is constructed and authenticated on every invocation, so a caller
    /// that needs several resources in one operation should hold on to the one it gets back
    /// instead of asking again.
    ///
    /// @return RealmResource the resource of the configured target realm
    public RealmResource getRealmResource() {

        Keycloak keycloak = KeycloakBuilder.builder()
                .serverUrl(String.format("%s:%s", keycloakAdminProperties.host(), keycloakAdminProperties.port()))
                .realm(keycloakAdminProperties.realmMaster())
                .clientId(keycloakAdminProperties.adminCli())
                .username(keycloakAdminProperties.user())
                .password(keycloakAdminProperties.password())
                .clientSecret(keycloakAdminProperties.clientSecret())
                .resteasyClient(new ResteasyClientBuilderImpl().connectionPoolSize(10).build())
                .build();

        return keycloak.realm(keycloakAdminProperties.realmName());
    }
}
