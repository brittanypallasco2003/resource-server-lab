package com.spring.resource.server.lab.infrastructure.adapters.in.rest.mapper;

import com.spring.resource.server.lab.application.command.CreateUserCommand;
import com.spring.resource.server.lab.application.command.UpdateUserCommand;
import com.spring.resource.server.lab.domain.model.User;
import com.spring.resource.server.lab.infrastructure.adapters.in.rest.dto.CreateUserRequest;
import com.spring.resource.server.lab.infrastructure.adapters.in.rest.dto.UpdateUserRequest;
import com.spring.resource.server.lab.infrastructure.adapters.in.rest.dto.UserResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

import static org.mapstruct.MappingConstants.ComponentModel.SPRING;

@Mapper(componentModel = SPRING, unmappedTargetPolicy = ReportingPolicy.IGNORE, unmappedSourcePolicy = ReportingPolicy.IGNORE)
public interface UserRestMapper {

    CreateUserCommand toCommand(CreateUserRequest request);

    UpdateUserCommand toCommand(UpdateUserRequest request);

    @Mapping(source = "email.value", target = "email")
    UserResponse toResponse(User user);
}
