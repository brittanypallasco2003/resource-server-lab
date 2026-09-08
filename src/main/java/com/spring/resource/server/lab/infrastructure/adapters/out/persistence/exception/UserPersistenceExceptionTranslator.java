package com.spring.resource.server.lab.infrastructure.adapters.out.persistence.exception;

import java.util.Locale;

import org.hibernate.exception.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;

import com.spring.resource.server.lab.domain.exception.UserAlreadyExistsException;
import com.spring.resource.server.lab.domain.model.User;
import com.spring.resource.server.lab.infrastructure.adapters.out.persistence.jpa.UserJpaEntity;

/// Decides what a database integrity error means in business terms.
///
/// There are **two** translations on the way out of the database, and this class is the second one.
/// The first is Spring's: `@Repository` on the adapter installs the exception translation that
/// turns a vendor error into the technology-neutral `DataAccessException` hierarchy — which is why
/// the adapter catches [DataIntegrityViolationException] and not Hibernate's own type. That step
/// goes from *one technology* to *any technology*. This one goes from *technical* to *business*,
/// and only the application can decide it: no framework knows that `uk_users_username` is the last
/// line of defence of a business rule.
///
/// It is a class of its own, and not two private methods on `JpaUserRepositoryAdapter`, because the
/// two have different reasons to change. The adapter changes when *how* a user is stored changes —
/// `saveAndFlush`, transactions, soft delete. This changes when the *catalogue of constraints*
/// changes — the day `email` gets a unique index of its own. Splitting them also makes this half
/// testable with `new`, where the branch used to need an embedded database to reach.
///
/// Where it deliberately does **not** live:
///
///   - `domain/` or `application/`, because it speaks Hibernate and Spring, and both layers are
///     kept free of technology (`HexagonalArchitectureTest` fails the build over it). Note that
///     [UserAlreadyExistsException] *is* a domain type and stays there: the business concept
///     belongs to the domain, only its detection is a persistence detail.
///   - the REST advice, because an inbound adapter may not depend on an outbound one — and because
///     by then the [User] is gone, so the message could no longer name the username that collided.
@Component
public class UserPersistenceExceptionTranslator {

    /// Turns a database integrity error into the right kind of failure.
    ///
    /// Only the username index becomes a [UserAlreadyExistsException]: that collision is a business
    /// conflict and deserves a 409, the same call the Keycloak adapter makes when it sees a 409 of
    /// its own. Anything else — a null in a non-null column, a broken foreign key — is a defect in
    /// this application, not something the caller did, so it travels on untouched and surfaces as a
    /// 500 with its stack trace intact.
    ///
    /// The constraint is recognised **by name**, not by matching the message text. That is what
    /// keeps this from silently reclassifying an unrelated error the day a second index is added,
    /// and it is the same reason the domain has typed exceptions at all.
    ///
    /// It takes the whole [User] rather than just the username so that a second constraint can read
    /// whichever field it needs without changing the signature.
    ///
    /// @param e the error the driver reported
    /// @param user the user being stored, for the message
    /// @return the exception to throw: a domain conflict, or the original error
    public RuntimeException translate(DataIntegrityViolationException e, User user) {
        if (e.getCause() instanceof ConstraintViolationException violation && violatesUsernameIndex(violation)) {
            return new UserAlreadyExistsException(user.getUsername());
        }
        return e;
    }

    /// Whether the violated index is the one on `username`.
    ///
    /// The comparison is `contains` and not `equals` because each dialect decorates the name
    /// differently, and the decoration is not something this code should have to predict:
    ///
    /// ```
    /// PostgreSQL → "uk_users_username"
    /// H2         → "PUBLIC.UK_USERS_USERNAME INDEX PUBLIC.UK_USERS_USERNAME_INDEX_4"
    /// ```
    ///
    /// It is still the constraint *name* being matched, not the error message — the name is
    /// declared once in [UserJpaEntity#USERNAME_CONSTRAINT] and the database is told to use it.
    /// The loose match does mean a future index whose name contains this one as a substring would
    /// be mistaken for it; the naming convention makes that unlikely, and a second unique index
    /// would need its own branch here anyway.
    ///
    /// @param violation the constraint error Hibernate reported
    /// @return boolean true when it is the username index
    private boolean violatesUsernameIndex(ConstraintViolationException violation) {
        String name = violation.getConstraintName();
        return name != null
                && name.toUpperCase(Locale.ROOT)
                        .contains(UserJpaEntity.USERNAME_CONSTRAINT.toUpperCase(Locale.ROOT));
    }

}
