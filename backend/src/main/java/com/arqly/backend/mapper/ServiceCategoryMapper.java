package com.arqly.backend.mapper;

import com.arqly.backend.dto.ServiceCatalogDtos.ServiceCategoryRequest;
import com.arqly.backend.entity.ServiceCategory;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface ServiceCategoryMapper {
    ServiceCategory toEntity(ServiceCategoryRequest request);
    void update(ServiceCategoryRequest request, @MappingTarget ServiceCategory category);
}
