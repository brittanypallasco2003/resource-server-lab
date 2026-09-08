package com.spring.resource.server.lab.application.command;

import java.util.Set;

/// Command object that encapsulates the data required to create a new user in the system. 
///
/// @param username name of the user; it is validated when constructing the value object
/// @param email email address in raw format; it is validated when constructing the value object
/// @param firstName first name
/// @param lastName last name
/// @param roles requested roles; if empty, the default role from the domain is applied
/// @param password initial password in clear text
public record CreateUserCommand(
        String username,
        String email,
        String firstName,
        String lastName,
        Set<String> roles,
        String password) {

}
