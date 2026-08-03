package com.spring.resource.server.lab.services;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.jspecify.annotations.NonNull;
import org.keycloak.OAuth2Constants;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.springframework.stereotype.Service;

import com.spring.resource.server.lab.controller.dto.Keycloak.UserDto;
import com.spring.resource.server.lab.util.KeycloakProvider;

@Service
public class KeycloakServiceImpl implements IKeycloakService {

    private final KeycloakProvider keycloakProvider;

    public KeycloakServiceImpl(KeycloakProvider keycloakProvider) {
        this.keycloakProvider = keycloakProvider;
    }


    @Override
    public Set<UserRepresentation> findAllUsers() {
        return keycloakProvider.getRealmResource().users().list().stream().collect(Collectors.toSet());
    }

    @Override
    public Set<UserRepresentation> searchUserByUsername(String username) {
        return keycloakProvider.getRealmResource().users().searchByUsername(username, true).stream()
                .collect(Collectors.toSet());
    }

    @Override
    public void createUser(@NonNull UserDto userDto) {
        var userResource = keycloakProvider.getUserResource();

        UserRepresentation userRepresentation = new UserRepresentation();
        userRepresentation.setUsername(userDto.username());
        userRepresentation.setEmail(userDto.email());
        userRepresentation.setFirstName(userDto.firstName());
        userRepresentation.setLastName(userDto.lastName());
        userRepresentation.setEnabled(true);
        userRepresentation.setEmailVerified(true);

        var response = userResource.create(userRepresentation);
        if (response.getStatus() != 201) {
            throw new RuntimeException("Failed to create user: " + response.getStatusInfo().getReasonPhrase());
        }
        String path = response.getLocation().getPath();
        String userId = path.substring(path.lastIndexOf('/') + 1);
        CredentialRepresentation credential = new CredentialRepresentation();
        credential.setTemporary(false);
        credential.setType(OAuth2Constants.PASSWORD);
        credential.setValue(userDto.password());

        userResource.get(userId).resetPassword(credential);

        RealmResource realmResource = keycloakProvider.getRealmResource();

        Set<RoleRepresentation> roles = new HashSet<>();
        if (userDto.roles() == null || userDto.roles().isEmpty()) {
            roles.add(realmResource.roles().get("user").toRepresentation());
        } else {
            realmResource.roles().list().stream()
                    .filter(role -> userDto.roles().stream()
                            .anyMatch(roleDto -> roleDto.equalsIgnoreCase(role.getName())))
                    .forEach(roles::add);

        }
        realmResource.users().get(userId).roles().realmLevel().add(roles.stream().toList());
    }

    @Override
    public void deleteUser(String id) {
        keycloakProvider.getUserResource().get(id).remove();
    }

    @Override
    public void updateUser(String id, UserDto userDto) {
        CredentialRepresentation credential = new CredentialRepresentation();
        credential.setTemporary(false);
        credential.setType(OAuth2Constants.PASSWORD);
        credential.setValue(userDto.password());
        
        UserRepresentation userRepresentation = new UserRepresentation();
        userRepresentation.setUsername(userDto.username());
        userRepresentation.setEmail(userDto.email());
        userRepresentation.setFirstName(userDto.firstName());
        userRepresentation.setLastName(userDto.lastName());
        userRepresentation.setEnabled(true);
        userRepresentation.setEmailVerified(true);
        userRepresentation.setCredentials(List.of(credential));

        var existingUser = keycloakProvider.getUserResource().get(id);
        existingUser.update(userRepresentation);
    }

}
