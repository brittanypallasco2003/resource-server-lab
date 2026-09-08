package com.spring.resource.server.lab.infrastructure.adapters.out.persistence.mapper;

import com.spring.resource.server.lab.domain.model.User;
import com.spring.resource.server.lab.infrastructure.adapters.out.persistence.jpa.UserJpaEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

import static org.mapstruct.MappingConstants.ComponentModel.SPRING;

@Mapper(componentModel = SPRING, unmappedSourcePolicy = ReportingPolicy.IGNORE, unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface UserJpaMapper {

    @Mapping(source = "email", target = "email.value")
    User toDomain(UserJpaEntity entity);

    @Mapping(source = "email.value", target = "email")
    UserJpaEntity toEntity(User user);
}
