package com.arqly.backend.mapper;

import com.arqly.backend.dto.TenantDtos.TenantRequest;
import com.arqly.backend.dto.TenantDtos.TenantResponse;
import com.arqly.backend.entity.Tenant;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface TenantMapper {
    Tenant toEntity(TenantRequest request);
    TenantResponse toResponse(Tenant tenant);
    void update(TenantRequest request, @MappingTarget Tenant tenant);
}
