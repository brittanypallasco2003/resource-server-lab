package com.spring.resource.server.lab.domain.exception;

/// Thrown when operating on a user that does not exist in the repository. It is translated to a 404.
///
/// The identifier is generic on purpose. It used to take a `UserId`, and when identity became a
/// `String` the class stopped compiling. By accepting any type, the exception does not break again
/// if tomorrow the key is composite or numeric: the only thing it needs from the identifier is
/// being able to write it into the message.
///
/// There is no constructor taking a ready-made message. There was one, and with `String`
/// identities the two overloads were indistinguishable: `new UserNotFoundException(id)` picked the
/// message one and the error ended up being the bare identifier, with no sentence at all.
public class UserNotFoundException extends DomainException {

    /// @param <ID> the type of the identifier, whatever it is
    /// @param id the identifier that was looked up
    public <ID> UserNotFoundException(ID id) {
        super("No existe un usuario con el identificador '%s'".formatted(id));
    }

}
