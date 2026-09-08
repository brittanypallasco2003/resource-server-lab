package com.spring.resource.server.lab.infrastructure.adapters.in.rest.dto;

import java.util.Set;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/// Body of a user update request.
///
/// The password is optional: omitting it means "do not change it". `@Size` does not fire on null
/// values, so the length is only checked when the client sends a new one.
///
/// @param username the username
/// @param email the e-mail address
/// @param firstName the given name
/// @param lastName the family name
/// @param roles the assigned roles
/// @param password the new password, or absent to keep the current one
public record UpdateUserRequest(

        @NotBlank(message = "El nombre de usuario no puede estar vacío")
        @Size(max = 100, message = "El nombre de usuario no puede superar los 100 caracteres")
        String username,

        @NotBlank(message = "El correo electrónico no puede estar vacío")
        @Email(message = "El correo electrónico no tiene un formato válido")
        String email,

        @NotBlank(message = "El nombre no puede estar vacío")
        String firstName,

        @NotBlank(message = "El apellido no puede estar vacío")
        String lastName,

        Set<String> roles,

        @Size(min = 8, message = "La contraseña debe tener al menos 8 caracteres")
        String password) {

}
