package com.spring.resource.server.lab.infrastructure.adapters.in.rest.dto;

import java.util.Set;

import com.spring.resource.server.lab.domain.model.User;
import com.spring.resource.server.lab.domain.repository.UniqueField;
import com.spring.resource.server.lab.infrastructure.adapters.in.rest.validation.Unique;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/// Body of a user creation request.
///
/// The Bean Validation annotations check the *shape of the HTTP input*, which is not the same as
/// the domain invariants. It is not duplication: this layer rejects a malformed request with a 400
/// and a per-field breakdown before touching anything, while
/// [com.spring.resource.server.lab.domain.model.valueobject.Email] protects the core against
/// *any* adapter, including the ones that do not exist yet.
///
/// @param username the desired username
/// @param email the e-mail address
/// @param firstName the given name
/// @param lastName the family name
/// @param roles the requested roles; optional, the domain applies its default role when omitted
/// @param password the initial password
public record CreateUserRequest(

        @NotBlank(message = "El nombre de usuario no puede estar vacío")
        @Size(max = 100, message = "El nombre de usuario no puede superar los 100 caracteres")
        @Unique(entity = User.class, field = UniqueField.USERNAME,
                message = "El nombre de usuario ya está ocupado")
        String username,

        @NotBlank(message = "El correo electrónico no puede estar vacío")
        @Email(message = "El correo electrónico no tiene un formato válido")
        @Unique(entity = User.class, field = UniqueField.EMAIL,
                message = "El correo electrónico ya está ocupado")
        String email,

        @NotBlank(message = "El nombre no puede estar vacío")
        String firstName,

        @NotBlank(message = "El apellido no puede estar vacío")
        String lastName,

        Set<String> roles,

        @NotBlank(message = "La contraseña no puede estar vacía")
        @Size(min = 8, message = "La contraseña debe tener al menos 8 caracteres")
        String password) {

}
