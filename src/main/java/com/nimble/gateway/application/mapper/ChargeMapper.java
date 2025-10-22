package com.nimble.gateway.application.mapper;

import com.nimble.gateway.application.dto.ChargeDTO;
import com.nimble.gateway.application.dto.CreateChargeDTO;
import com.nimble.gateway.domain.entity.Charge;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface ChargeMapper {
    
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "originatorId", ignore = true)
    @Mapping(target = "originatorName", ignore = true)
    @Mapping(target = "recipientId", ignore = true)
    @Mapping(target = "recipientName", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "paidAt", ignore = true)
    @Mapping(target = "cancelledAt", ignore = true)
    ChargeDTO toDTO(CreateChargeDTO createChargeDTO);
    
    ChargeDTO toDTO(Charge charge);
}