package com.arqly.backend.mapper;

import com.arqly.backend.dto.ClientDtos.ClientRequest;
import com.arqly.backend.entity.Client;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface ClientMapper {
    Client toEntity(ClientRequest request);
    void update(ClientRequest request, @MappingTarget Client client);
}
