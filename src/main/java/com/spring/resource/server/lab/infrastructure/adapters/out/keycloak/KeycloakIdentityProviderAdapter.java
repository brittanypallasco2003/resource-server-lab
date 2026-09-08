package com.spring.resource.server.lab.infrastructure.adapters.out.keycloak;

import java.util.List;
import java.util.Set;

import org.keycloak.OAuth2Constants;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.admin.client.resource.RoleMappingResource;
import org.keycloak.admin.client.resource.UsersResource;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.spring.resource.server.lab.domain.exception.UserAlreadyExistsException;
import com.spring.resource.server.lab.domain.model.User;
import com.spring.resource.server.lab.domain.repository.IdentityProvider;
import com.spring.resource.server.lab.infrastructure.config.properties.KeycloakAdminProperties;
import com.spring.resource.server.lab.infrastructure.exception.ExternalProviderException;

import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.core.Response;

/// Outbound adapter implementing [IdentityProvider] against Keycloak's Admin REST API.
///
/// Keycloak is the identity provider, and only that. The user's *row* is stored by
/// [com.spring.resource.server.lab.domain.repository.UserRepository] in PostgreSQL; what lives here
/// is only the account that person logs in with. There used to be an adapter that made Keycloak a
/// store as well, and it went away with that separation.
///
/// Registering in Keycloak takes three calls — create the account, set the password, assign the
/// roles of this client — and the Admin REST API has no transaction. If either of the last two fails, the
/// just-created account is deleted: a half-configured account is worse than none, because it takes
/// the username and still does not let anyone in. Only a failure of the compensating delete itself
/// leaves Keycloak inconsistent, and that is logged as an error.
@Component
public class KeycloakIdentityProviderAdapter implements IdentityProvider {

    private static final Logger log = LoggerFactory.getLogger(KeycloakIdentityProviderAdapter.class);

    private final KeycloakProvider keycloakProvider;
    private final KeycloakAdminProperties properties;

    public KeycloakIdentityProviderAdapter(KeycloakProvider keycloakProvider,
            KeycloakAdminProperties properties) {
        this.keycloakProvider = keycloakProvider;
        this.properties = properties;
    }

    @Override
    public String register(User user, String rawPassword) {
        // Un unico RealmResource para toda la operacion: cada llamada a getRealmResource()
        // construye y autentica un cliente nuevo, asi que pedirlo tres veces costaba tres
        // autenticaciones contra el realm master.
        RealmResource realmResource = keycloakProvider.getRealmResource();
        UsersResource usersResource = realmResource.users();

        String ssoId = createAccount(usersResource, user);

        try {
            setPassword(usersResource, ssoId, rawPassword);
            assignClientRoles(realmResource, ssoId, user.getRoles());
        } catch (RuntimeException e) {
            undoRegistration(usersResource, ssoId);
            throw new ExternalProviderException(
                    "No se pudo completar el alta del usuario '%s' en Keycloak".formatted(user.getUsername()), e);
        }

        return ssoId;
    }

    @Override
    public void updateAccount(String ssoId, User user) {
        RealmResource realmResource = keycloakProvider.getRealmResource();

        UserRepresentation representation = new UserRepresentation();
        representation.setUsername(user.getUsername());
        representation.setEmail(user.getEmail().value());
        representation.setEnabled(user.isEnabled());

        // Nombre y apellido no se mandan: son datos de esta aplicacion, no de Keycloak. Ver
        // IdentityProvider#updateAccount.
        try {
            realmResource.users().get(ssoId).update(representation);
            replaceClientRoles(realmResource, ssoId, user.getRoles());
        } catch (NotFoundException e) {
            throw new ExternalProviderException(
                    "La cuenta '%s' ya no existe en Keycloak".formatted(ssoId), e);
        }
    }

    @Override
    public void changePassword(String ssoId, String rawPassword) {
        try {
            setPassword(keycloakProvider.getRealmResource().users(), ssoId, rawPassword);
        } catch (NotFoundException e) {
            throw new ExternalProviderException(
                    "La cuenta '%s' ya no existe en Keycloak".formatted(ssoId), e);
        }
    }

    @Override
    public void unregister(String ssoId) {
        try (Response response = keycloakProvider.getRealmResource().users().delete(ssoId)) {
            int status = response.getStatus();
            if (status >= 400 && status != Response.Status.NOT_FOUND.getStatusCode()) {
                throw new ExternalProviderException("Keycloak rechazo la baja de la cuenta '%s': %s"
                        .formatted(ssoId, response.getStatusInfo().getReasonPhrase()));
            }
        } catch (NotFoundException e) {
            log.debug("La cuenta '{}' ya no existia en Keycloak al darla de baja", ssoId);
        }
    }

    /// Creates the account and returns the identifier Keycloak assigned to it.
    ///
    /// That identifier does not come in the response body: it comes in the `Location` header, as
    /// the last segment of the URL of the freshly created resource.
    private String createAccount(UsersResource usersResource, User user) {
        UserRepresentation representation = new UserRepresentation();
        representation.setUsername(user.getUsername());
        representation.setEmail(user.getEmail().value());
        representation.setFirstName(user.getFirstName());
        representation.setLastName(user.getLastName());
        representation.setEnabled(user.isEnabled());
        representation.setEmailVerified(true);

        try (Response response = usersResource.create(representation)) {
            int status = response.getStatus();

            // Keycloak responde 409 cuando el nombre de usuario o el correo ya estan ocupados. Es
            // un conflicto de negocio, no un fallo tecnico, asi que se traduce al dominio.
            if (status == Response.Status.CONFLICT.getStatusCode()) {
                throw new UserAlreadyExistsException(user.getUsername());
            }
            if (status != Response.Status.CREATED.getStatusCode()) {
                throw new ExternalProviderException("Keycloak rechazo el alta del usuario '%s' (HTTP %d): %s"
                        .formatted(user.getUsername(), status, response.getStatusInfo().getReasonPhrase()));
            }

            String path = response.getLocation().getPath();
            return path.substring(path.lastIndexOf('/') + 1);
        }
    }

    /// Sets the initial password.
    ///
    /// It is marked as non-temporary: were it temporary, Keycloak would force a change on the
    /// first login, which is not what a caller of this API expects.
    private void setPassword(UsersResource usersResource, String ssoId, String rawPassword) {
        CredentialRepresentation credential = new CredentialRepresentation();
        credential.setTemporary(false);
        credential.setType(OAuth2Constants.PASSWORD);
        credential.setValue(rawPassword);

        usersResource.get(ssoId).resetPassword(credential);
    }

    /// Resolves the role names against the roles defined on **this** client and assigns them.
    ///
    /// Client roles and not realm roles, because
    /// [com.spring.resource.server.lab.infrastructure.security.JwtAuthenticationConverter] reads
    /// `resource_access.<clientId>.roles`. Writing realm roles here used to mean that a user
    /// created through this API arrived with no authority at all in this resource server.
    ///
    /// The domain already guarantees the set never arrives empty (it applies its default role), so
    /// there is no need here for the "if there are no roles, use the default one" branch: that was
    /// a business rule living inside an adapter.
    private void assignClientRoles(RealmResource realmResource, String ssoId, Set<String> roleNames) {
        String clientUuid = clientUuid(realmResource);

        List<RoleRepresentation> roles = realmResource.clients().get(clientUuid).roles().list().stream()
                .filter(role -> roleNames.stream().anyMatch(name -> name.equalsIgnoreCase(role.getName())))
                .toList();

        if (roles.isEmpty()) {
            throw new ExternalProviderException(
                    "Ninguno de los roles %s existe como rol de cliente en '%s'"
                            .formatted(roleNames, properties.appClientId()));
        }

        realmResource.users().get(ssoId).roles().clientLevel(clientUuid).add(roles);
    }

    /// Resolves the internal UUID Keycloak assigned to this client.
    ///
    /// The Admin REST API addresses a client by that UUID, never by the `clientId` a human writes
    /// in the console, so every client-level call has to look it up first. That is one extra round
    /// trip per operation; fine for a lab realm, worth caching in anything busier.
    private String clientUuid(RealmResource realmResource) {
        List<ClientRepresentation> clients = realmResource.clients().findByClientId(properties.appClientId());
        if (clients.isEmpty()) {
            throw new ExternalProviderException(
                    "El cliente '%s' no existe en el realm '%s'"
                            .formatted(properties.appClientId(), properties.realmName()));
        }
        return clients.get(0).getId();
    }

    /// Leaves the account's client roles exactly as the domain asks: it removes the ones that are
    /// no longer there and adds the ones that are missing.
    ///
    /// It takes two steps because the Admin REST API has no "replace these roles": it only knows
    /// how to add and to remove. Only the roles of *this* client are touched — a role the user
    /// holds on another client is none of this application's business.
    private void replaceClientRoles(RealmResource realmResource, String ssoId, Set<String> roleNames) {
        String clientUuid = clientUuid(realmResource);
        RoleMappingResource mappings = realmResource.users().get(ssoId).roles();
        List<RoleRepresentation> current = mappings.clientLevel(clientUuid).listAll();

        List<RoleRepresentation> toRemove = current.stream()
                .filter(role -> roleNames.stream().noneMatch(name -> name.equalsIgnoreCase(role.getName())))
                .toList();
        if (!toRemove.isEmpty()) {
            mappings.clientLevel(clientUuid).remove(toRemove);
        }

        List<RoleRepresentation> toAdd = realmResource.clients().get(clientUuid).roles().list().stream()
                .filter(role -> roleNames.stream().anyMatch(name -> name.equalsIgnoreCase(role.getName())))
                .filter(role -> current.stream().noneMatch(held -> held.getName().equals(role.getName())))
                .toList();
        if (!toAdd.isEmpty()) {
            mappings.clientLevel(clientUuid).add(toAdd);
        }
    }

    /// Compensation for a partial registration. A failure while undoing must not hide the
    /// original error, so it is only logged.
    private void undoRegistration(UsersResource usersResource, String ssoId) {
        try (Response response = usersResource.delete(ssoId)) {
            log.warn("Alta incompleta: se elimino la cuenta '{}' recien creada (HTTP {})", ssoId,
                    response.getStatus());
        } catch (RuntimeException e) {
            log.error("No se pudo deshacer el alta parcial de la cuenta '{}'. Queda inconsistente en Keycloak.",
                    ssoId, e);
        }
    }

}
