package com.nimble.gateway.application.mapper;

import com.nimble.gateway.application.dto.PaymentDTO;
import com.nimble.gateway.domain.entity.Payment;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface PaymentMapper {
    
    PaymentDTO toDTO(Payment payment);
}