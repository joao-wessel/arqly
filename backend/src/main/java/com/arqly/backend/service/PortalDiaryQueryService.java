package com.arqly.backend.service;

import com.arqly.backend.dto.PortalDtos.*;
import com.arqly.backend.entity.*;
import com.arqly.backend.exception.NotFoundException;
import com.arqly.backend.repository.*;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PortalDiaryQueryService {
 private final PortalAuthorizationService authorization; private final ProjectRepository projects; private final ConstructionDiaryEntryRepository entries; private final ConstructionDiaryObservationRepository observations; private final ConstructionDiaryOccurrenceRepository occurrences; private final ConstructionDiaryDecisionRepository decisions; private final ConstructionDiaryPhotoRepository photos;
 public PortalDiaryQueryService(PortalAuthorizationService authorization,ProjectRepository projects,ConstructionDiaryEntryRepository entries,ConstructionDiaryObservationRepository observations,ConstructionDiaryOccurrenceRepository occurrences,ConstructionDiaryDecisionRepository decisions,ConstructionDiaryPhotoRepository photos){this.authorization=authorization;this.projects=projects;this.entries=entries;this.observations=observations;this.occurrences=occurrences;this.decisions=decisions;this.photos=photos;}
 @Transactional(readOnly=true) public List<PortalDiaryResponse> project(UUID token,UUID projectId){var client=authorization.client(token);projects.findByIdAndTenantIdAndClientIdAndDeletedFalse(projectId,client.getTenant().getId(),client.getId()).orElseThrow(()->new NotFoundException("Projeto não encontrado no portal."));return entries.findAllByProjectIdAndTenantIdAndStatusAndVisibilityAndDeletedFalseOrderByEntryDateDescCreatedAtDesc(projectId,client.getTenant().getId(),ConstructionDiaryStatus.PUBLISHED,ActivityVisibility.CLIENT_VISIBLE).stream().map(this::response).toList();}
    private PortalDiaryResponse response(ConstructionDiaryEntry e){return new PortalDiaryResponse(e.getId(),e.getProject().getId(),e.getProject().getName(),e.getTitle(),e.getEntryType().name(),e.getEntryDate(),e.getResponsibleUser()==null?"Equipe Arqly":e.getResponsibleUser().getName(),e.getSummary(),e.getLocation(),e.getStages().stream().map(ProjectStage::getName).toList(),observations.findAllByDiaryEntryIdOrderByOrderAsc(e.getId()).stream().filter(o->o.getVisibility()==ActivityVisibility.CLIENT_VISIBLE).map(o->new PortalDiaryObservationResponse(o.getTitle(),o.getDescription(),o.getCategory().name(),o.getStatus().name())).toList(),occurrences.findAllByDiaryEntryIdOrderByCreatedAtAsc(e.getId()).stream().filter(o->o.getVisibility()==ActivityVisibility.CLIENT_VISIBLE).map(o->new PortalDiaryOccurrenceResponse(o.getTitle(),o.getDescription(),o.getSeverity().name(),o.isResolved(),o.getDueDate())).toList(),decisions.findAllByDiaryEntryIdOrderByDecisionDateAsc(e.getId()).stream().filter(d->d.getVisibility()==ActivityVisibility.CLIENT_VISIBLE).map(d->new PortalDiaryDecisionResponse(d.getDescription(),d.getDecidedBy(),d.getDecisionDate())).toList(),photos.findAllByDiaryEntryIdOrderByOrderAsc(e.getId()).stream().filter(photo->photo.getFile().getVisibility()==com.arqly.backend.entity.FileVisibility.CLIENT_VISIBLE&&photo.getFile().getStatus()==com.arqly.backend.entity.FileResourceStatus.ACTIVE).map(photo->new PortalDiaryPhotoResponse(photo.getFile().getId(),photo.getFile().getName(),photo.getFile().getMimeType(),photo.getCaption(),photo.getDescription())).toList(),e.getNextVisitDate(),e.getPublishedAt());}
}
