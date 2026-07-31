package com.arqly.backend.mapper;

import com.arqly.backend.dto.DocumentDtos.GenerateDocumentRequest;
import com.arqly.backend.entity.GeneratedDocument;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface GeneratedDocumentMapper {
    @Mapping(target = "tenant", ignore = true)
    @Mapping(target = "template", ignore = true)
    @Mapping(target = "project", ignore = true)
    @Mapping(target = "proposal", ignore = true)
    @Mapping(target = "client", ignore = true)
    @Mapping(target = "version", ignore = true)
    @Mapping(target = "currentVersion", ignore = true)
    @Mapping(target = "seriesId", ignore = true)
    @Mapping(target = "previousVersion", ignore = true)
    @Mapping(target = "generatedBy", ignore = true)
    @Mapping(target = "generatedByName", ignore = true)
    @Mapping(target = "generatedAt", ignore = true)
    @Mapping(target = "deleted", ignore = true)
    @Mapping(target = "deletedAt", ignore = true)
    GeneratedDocument toEntity(GenerateDocumentRequest request);
}
