package com.spring.resource.server.lab.infrastructure.adapters.in.rest.dto;

import java.util.Set;

/// Representation of a user in the HTTP response.
///
/// It exists as a type separate from [com.spring.resource.server.lab.domain.model.User] so the API
/// contract can evolve without dragging the domain along, and the other way round. It never
/// exposes the password nor any identity-provider type.
///
/// @param id the user's identifier
/// @param username the username
/// @param email the e-mail address
/// @param firstName the given name
/// @param lastName the family name
/// @param roles the assigned roles
/// @param enabled whether the account is usable
public record UserResponse(
        String id,
        String username,
        String email,
        String firstName,
        String lastName,
        Set<String> roles,
        boolean enabled) {

}
