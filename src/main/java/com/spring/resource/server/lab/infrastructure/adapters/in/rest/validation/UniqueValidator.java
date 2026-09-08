package com.spring.resource.server.lab.infrastructure.adapters.in.rest.validation;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.spring.resource.server.lab.domain.repository.CrudRepository;
import com.spring.resource.server.lab.domain.repository.UniqueField;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

/// Resolves [Unique] by asking the repository of the named aggregate.
///
/// It injects `List<CrudRepository<?, ?>>` rather than a concrete repository: that is exactly what
/// makes the annotation work for any aggregate without touching this class again. The index is
/// built from [CrudRepository#managedType()] because type erasure makes it impossible to ask an
/// instance what its `T` was at runtime.
///
/// It depends on `domain/repository`, never on `adapters/out`. The port lives in the domain, so
/// this does not break the rule that keeps the two ends of the hexagon from talking along the
/// edge.
///
/// A cost worth keeping in mind: every validated request issues one query against the active
/// store. That is one database query per POST.
public class UniqueValidator implements ConstraintValidator<Unique, String> {

    private final Map<Class<?>, CrudRepository<?, ?>> repositoriesByType;

    private Class<?> entity;
    private UniqueField field;

    public UniqueValidator(List<CrudRepository<?, ?>> repositories) {
        Map<Class<?>, CrudRepository<?, ?>> index = new HashMap<>();
        for (CrudRepository<?, ?> repository : repositories) {
            index.put(repository.managedType(), repository);
        }
        this.repositoriesByType = Map.copyOf(index);
    }

    @Override
    public void initialize(Unique annotation) {
        this.entity = annotation.entity();
        this.field = annotation.field();
    }

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (value == null || value.isBlank()) {
            return true;
        }

        CrudRepository<?, ?> repository = repositoriesByType.get(entity);
        if (repository == null) {
            // Error de configuración, no dato inválido. Fallar es preferible a dar por bueno un
            // valor que en realidad no ha comprobado nadie.
            throw new IllegalStateException(
                    "No hay ningún CrudRepository registrado para %s; revisa la anotación @Unique"
                            .formatted(entity.getName()));
        }

        return !repository.existsByField(field, value);
    }

}
