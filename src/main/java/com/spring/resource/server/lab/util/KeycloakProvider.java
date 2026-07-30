package com.spring.resource.server.lab.util;

import org.jboss.resteasy.client.jaxrs.internal.ResteasyClientBuilderImpl;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.KeycloakBuilder;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.admin.client.resource.UsersResource;
import org.springframework.stereotype.Component;

import com.spring.resource.server.lab.config.dtos.KeycloakAdminProperties;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Component
public class KeycloakProvider {

    private final KeycloakAdminProperties keycloakAdminProperties;

    public  RealmResource getRealmResource(){

        Keycloak keycloack = KeycloakBuilder.builder()
        .serverUrl(String.format("%s:%s",keycloakAdminProperties.host(), keycloakAdminProperties.port()))
        .realm(keycloakAdminProperties.realmMaster())
        .clientId(keycloakAdminProperties.adminCli())
        .username(keycloakAdminProperties.user())
        .password(keycloakAdminProperties.password())
        .clientSecret(keycloakAdminProperties.clientSecret())
        .resteasyClient(new ResteasyClientBuilderImpl().connectionPoolSize(10).build())
        .build();

        return keycloack.realm(keycloakAdminProperties.realmName());
    }


    public UsersResource getUserResource(){
        RealmResource realmResource =getRealmResource();
        return realmResource.users();
    }




}
