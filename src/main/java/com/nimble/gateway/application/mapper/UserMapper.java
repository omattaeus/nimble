package com.nimble.gateway.application.mapper;

import com.nimble.gateway.application.dto.CreateUserDTO;
import com.nimble.gateway.application.dto.UserDTO;
import com.nimble.gateway.domain.entity.User;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface UserMapper {
    
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "balance", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "isActive", ignore = true)
    User toEntity(CreateUserDTO createUserDTO);
    
    UserDTO toDTO(User user);
}