package com.spring.resource.server.lab.util;

import org.jboss.resteasy.client.jaxrs.internal.ResteasyClientBuilderImpl;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.KeycloakBuilder;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.admin.client.resource.UsersResource;
import org.springframework.stereotype.Component;

import com.spring.resource.server.lab.config.dtos.KeycloakAdminProperties;

@Component
public class KeycloakProvider {

    private final KeycloakAdminProperties keycloakAdminProperties;

    public KeycloakProvider(KeycloakAdminProperties keycloakAdminProperties) {
        this.keycloakAdminProperties = keycloakAdminProperties;
    }

    /// This method returns a RealmResource object that represents the Keycloak realm specified in the KeycloakAdminProperties.
    /// @return RealmResource the RealmResource object for the specified Keycloak realm
    public RealmResource getRealmResource() {

        Keycloak keycloack = KeycloakBuilder.builder()
                .serverUrl(String.format("%s:%s", keycloakAdminProperties.host(), keycloakAdminProperties.port()))
                .realm(keycloakAdminProperties.realmMaster())
                .clientId(keycloakAdminProperties.adminCli())
                .username(keycloakAdminProperties.user())
                .password(keycloakAdminProperties.password())
                .clientSecret(keycloakAdminProperties.clientSecret())
                .resteasyClient(new ResteasyClientBuilderImpl().connectionPoolSize(10).build())
                .build();

        return keycloack.realm(keycloakAdminProperties.realmName());
    }


    /// This method returns a UsersResource object that represents the users resource of the Keycloak realm specified in the KeycloakAdminProperties.
    /// @return UsersResource the UsersResource object for the specified Keycloak realm
    public UsersResource getUserResource() {
        RealmResource realmResource = getRealmResource();
        return realmResource.users();
    }

}
