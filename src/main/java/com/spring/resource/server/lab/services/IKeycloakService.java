package com.spring.resource.server.lab.services;

import java.util.Set;

import org.keycloak.representations.idm.UserRepresentation;

import com.spring.resource.server.lab.controller.dto.Keycloak.UserDto;

/// Interface for Keycloak service that defines methods for managing users in Keycloak.
public interface IKeycloakService {
    
    /// Method to retrieve all users from Keycloak.
    /// @return Set<UserRepresentation> a set of UserRepresentation objects representing all users in Keycloak.
    Set<UserRepresentation> findAllUsers();
    
    /// Method to search for users in Keycloak by their username.
    /// @param username the username to search for.
    /// @return Set<UserRepresentation> a set of UserRepresentation objects representing the users that match the specified username.
    Set<UserRepresentation> searchUserByUsername(String username);

    /// Method to create a new user in Keycloak.
    /// @param userDto a UserDto object containing the details of the user to be created|
    void createUser(UserDto userDto);
    
    /// Method to delete a user from Keycloak by their ID.
    /// @param id the ID of the user to be deleted.
    void deleteUser(String id);
    
    /// Method to update an existing user in Keycloak by their ID.
    /// @param id the ID of the user to be updated.
    void updateUser(String id, UserDto userDto);

}
