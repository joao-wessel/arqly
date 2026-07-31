package com.arqly.backend.mapper;

import com.arqly.backend.dto.DocumentDtos.DocumentTemplateRequest;
import com.arqly.backend.entity.DocumentTemplate;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface DocumentTemplateMapper {
    @Mapping(target = "tenant", ignore = true)
    @Mapping(target = "version", ignore = true)
    @Mapping(target = "currentVersion", ignore = true)
    @Mapping(target = "seriesId", ignore = true)
    @Mapping(target = "previousVersion", ignore = true)
    @Mapping(target = "archived", ignore = true)
    @Mapping(target = "deleted", ignore = true)
    @Mapping(target = "deletedAt", ignore = true)
    DocumentTemplate toEntity(DocumentTemplateRequest request);

    @Mapping(target = "tenant", ignore = true)
    @Mapping(target = "version", ignore = true)
    @Mapping(target = "currentVersion", ignore = true)
    @Mapping(target = "seriesId", ignore = true)
    @Mapping(target = "previousVersion", ignore = true)
    @Mapping(target = "archived", ignore = true)
    @Mapping(target = "deleted", ignore = true)
    @Mapping(target = "deletedAt", ignore = true)
    void update(DocumentTemplateRequest request, @MappingTarget DocumentTemplate template);
}
