package com.arqly.backend.mapper;

import com.arqly.backend.dto.ServiceCatalogDtos.ServiceRequest;
import com.arqly.backend.entity.Service;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface ServiceMapper {
    @Mapping(target = "category", ignore = true)
    Service toEntity(ServiceRequest request);

    @Mapping(target = "category", ignore = true)
    void update(ServiceRequest request, @MappingTarget Service service);
}
