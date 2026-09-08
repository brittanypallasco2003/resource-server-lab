package com.spring.resource.server.lab.application.command;

import java.util.Set;

/// Command object that encapsulates the data required to update an existing user in the system.
///
/// @param username name of the user
/// @param email email address
/// @param firstName first name
/// @param lastName last name
/// @param roles assigned roles; if empty, the current roles are preserved
/// @param password new password in clear text, or `null` to keep the current one
public record UpdateUserCommand(
        String username,
        String email,
        String firstName,
        String lastName,
        Set<String> roles,
        String password) {

}
