package com.arqly.backend.service;

import com.arqly.backend.dto.DocumentDtos.DocumentTemplateRequest;
import com.arqly.backend.dto.DocumentDtos.NewDocumentVersionRequest;
import com.arqly.backend.entity.DocumentTemplate;
import com.arqly.backend.entity.GeneratedDocument;
import com.arqly.backend.entity.GeneratedDocumentStatus;
import com.arqly.backend.mapper.DocumentTemplateMapper;
import java.time.Instant;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class DocumentVersionService {
    private final DocumentTemplateMapper templateMapper;

    public DocumentVersionService(DocumentTemplateMapper templateMapper) {
        this.templateMapper = templateMapper;
    }

    public DocumentTemplate newTemplateVersion(DocumentTemplate source, DocumentTemplateRequest request) {
        source.setCurrentVersion(false);
        source.setActive(false);
        var version = templateMapper.toEntity(request);
        version.setTenant(source.getTenant());
        version.setSeriesId(source.getSeriesId());
        version.setVersion(source.getVersion() + 1);
        version.setPreviousVersion(source);
        version.setCurrentVersion(true);
        return version;
    }

    public DocumentTemplate duplicateTemplate(DocumentTemplate source) {
        var copy = new DocumentTemplate();
        copy.setTenant(source.getTenant());
        copy.setName(source.getName() + " (cópia)");
        copy.setDescription(source.getDescription());
        copy.setCategory(source.getCategory());
        copy.setContent(source.getContent());
        copy.setActive(true);
        copy.setVersion(1);
        copy.setSeriesId(UUID.randomUUID());
        copy.setCurrentVersion(true);
        return copy;
    }

    public GeneratedDocument newDocumentVersion(GeneratedDocument source, NewDocumentVersionRequest request) {
        source.setCurrentVersion(false);
        var version = new GeneratedDocument();
        version.setTenant(source.getTenant());
        version.setTemplate(source.getTemplate());
        version.setProject(source.getProject());
        version.setProposal(source.getProposal());
        version.setClient(source.getClient());
        version.setTitle(request.title());
        version.setContent(request.content());
        version.setVersion(source.getVersion() + 1);
        version.setSeriesId(source.getSeriesId());
        version.setPreviousVersion(source);
        version.setCurrentVersion(true);
        version.setStatus(request.status() == null ? GeneratedDocumentStatus.GENERATED : request.status());
        version.setGeneratedAt(Instant.now());
        return version;
    }

    public GeneratedDocument duplicateDocument(GeneratedDocument source) {
        var copy = new GeneratedDocument();
        copy.setTenant(source.getTenant());
        copy.setTemplate(source.getTemplate());
        copy.setProject(source.getProject());
        copy.setProposal(source.getProposal());
        copy.setClient(source.getClient());
        copy.setTitle(source.getTitle() + " (cópia)");
        copy.setContent(source.getContent());
        copy.setVersion(1);
        copy.setSeriesId(UUID.randomUUID());
        copy.setCurrentVersion(true);
        copy.setStatus(GeneratedDocumentStatus.DRAFT);
        copy.setGeneratedAt(Instant.now());
        return copy;
    }
}
