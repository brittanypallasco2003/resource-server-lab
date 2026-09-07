package com.spring.resource.server.lab.infrastructure.adapters.in.rest;

import java.net.URI;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import com.spring.resource.server.lab.application.ports.CreateUserUseCase;
import com.spring.resource.server.lab.application.ports.DeleteUserUseCase;
import com.spring.resource.server.lab.application.ports.FindAllUserUseCase;
import com.spring.resource.server.lab.application.ports.FindUserByIdUseCase;
import com.spring.resource.server.lab.application.ports.SearchUserByUsernameUseCase;
import com.spring.resource.server.lab.application.ports.UpdateUserUseCase;
import com.spring.resource.server.lab.domain.model.User;
import com.spring.resource.server.lab.infrastructure.adapters.in.rest.dto.CreateUserRequest;
import com.spring.resource.server.lab.infrastructure.adapters.in.rest.dto.UpdateUserRequest;
import com.spring.resource.server.lab.infrastructure.adapters.in.rest.dto.UserResponse;
import com.spring.resource.server.lab.infrastructure.adapters.in.rest.mapper.UserRestMapper;

import jakarta.validation.Valid;

/// Inbound REST adapter for user management.
///
/// It depends on the six inbound ports, one per use case. Six constructor parameters are the
/// visible cost of having split the service into independent use cases; in exchange, each endpoint
/// declares exactly the capability it exercises and nothing more.
///
/// It knows neither Keycloak nor JPA: that is why replacing the active outbound adapter does not
/// force a single line of this file to change.
///
/// Authorization is declared here, method by method, with `@PreAuthorize`. `SecurityConfig` only
/// decides that these routes need a token at all; **which role** each operation demands belongs
/// next to the operation, because reading the list of users and deleting one are not the same
/// privilege and URL matchers would have to encode the HTTP verb to tell them apart.
///
/// The split is: reading needs `ADMIN` or `USER`, writing needs `ADMIN`. `USER` is
/// [com.spring.resource.server.lab.domain.model.User#DEFAULT_ROLE], so every account created
/// through this API can read; only an administrator can create, modify or delete one. Note this is
/// stricter than "authenticated": a token carrying scopes but no role at all is refused.
///
/// The role names are uppercase because that is how the domain stores them and how
/// [com.spring.resource.server.lab.infrastructure.security.JwtAuthenticationConverter] emits them.
/// `hasRole('admin')` would look for `ROLE_admin` and silently never match.
///
/// None of this works without `@EnableMethodSecurity`, which is on `SecurityConfig`.
@RestController
@RequestMapping("/api/users")
public class UserController {

    private final FindAllUserUseCase findAllUserUseCase;
    private final FindUserByIdUseCase findUserByIdUseCase;
    private final SearchUserByUsernameUseCase searchUserByUsernameUseCase;
    private final CreateUserUseCase createUserUseCase;
    private final UpdateUserUseCase updateUserUseCase;
    private final DeleteUserUseCase deleteUserUseCase;
    private final UserRestMapper mapper;

    public UserController(
            FindAllUserUseCase findAllUserUseCase,
            FindUserByIdUseCase findUserByIdUseCase,
            SearchUserByUsernameUseCase searchUserByUsernameUseCase,
            CreateUserUseCase createUserUseCase,
            UpdateUserUseCase updateUserUseCase,
            DeleteUserUseCase deleteUserUseCase, UserRestMapper mapper) {
        this.findAllUserUseCase = findAllUserUseCase;
        this.findUserByIdUseCase = findUserByIdUseCase;
        this.searchUserByUsernameUseCase = searchUserByUsernameUseCase;
        this.createUserUseCase = createUserUseCase;
        this.updateUserUseCase = updateUserUseCase;
        this.deleteUserUseCase = deleteUserUseCase;
        this.mapper = mapper;
    }

    /// Lists every user.
    /// @return ResponseEntity<List<UserResponse>> 200 with the complete list
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    public ResponseEntity<List<UserResponse>> findAll() {
        List<UserResponse> users = findAllUserUseCase.execute().stream()
                .map(mapper::toResponse)
                .toList();
        return ResponseEntity.ok(users);
    }

    /// Retrieves a user by its identifier.
    /// @param id the user's identifier
    /// @return ResponseEntity<UserResponse> 200 with the user, or 404 when it does not exist
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    public ResponseEntity<UserResponse> findById(@PathVariable String id) {
        var response= mapper.toResponse(findUserByIdUseCase.execute(id));
        return ResponseEntity.ok(response);
    }

    /// Searches users by exact username.
    /// @param username the username to search for
    /// @return ResponseEntity<List<UserResponse>> 200 with the matches
    @GetMapping("/search")
    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    public ResponseEntity<Collection<UserResponse>> searchByUsername(@RequestParam String username) {
        var users = searchUserByUsernameUseCase.execute(username).stream()
                .map(mapper::toResponse)
                .collect(Collectors.toSet());
        return ResponseEntity.ok(users);
    }

    /// Creates a user.
    /// @param request the creation data
    /// @return ResponseEntity<UserResponse> 201 with the `Location` header and the created user
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<UserResponse> create(@Valid @RequestBody CreateUserRequest request) {
        User created = createUserUseCase.execute(mapper.toCommand(request));

        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(created.getId())
                .toUri();

        return ResponseEntity.created(location).body(mapper.toResponse(created));
    }

    /// Updates an existing user.
    ///
    /// It returns 204 and not the updated user: the client has just sent that data, so echoing it
    /// back adds nothing and would force the use case to return something nobody uses.
    ///
    /// @param id the user's identifier
    /// @param request the new data
    /// @return ResponseEntity<Void> 204 when updated, or 404 when it does not exist
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> update(@PathVariable String id,
            @Valid @RequestBody UpdateUserRequest request) {
        updateUserUseCase.execute(id, mapper.toCommand(request));
        return ResponseEntity.noContent().build();
    }

    /// Deletes a user.
    /// @param id the user's identifier
    /// @return ResponseEntity<Void> 204 when deleted, or 404 when it does not exist
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> delete(@PathVariable String id) {
        deleteUserUseCase.execute(id);
        return ResponseEntity.noContent().build();
    }

}
