package com.arqly.backend.service;

import com.arqly.backend.activity.ActivityEventPublisher;
import com.arqly.backend.notification.NotificationDomainPublisher;
import com.arqly.backend.dto.ConstructionDiaryDtos.*;
import com.arqly.backend.entity.*;
import com.arqly.backend.exception.BusinessException;
import com.arqly.backend.exception.NotFoundException;
import com.arqly.backend.mapper.ConstructionDiaryMapper;
import com.arqly.backend.repository.*;
import jakarta.transaction.Transactional;
import java.time.Instant;
import java.util.*;
import org.springframework.stereotype.Service;

@Service
public class ConstructionDiaryService {
    private final ConstructionDiaryEntryRepository entries; private final ProjectStageRepository stages; private final TenantUserRepository users;
    private final ConstructionDiaryParticipantRepository participants; private final ConstructionDiaryObservationRepository observations;
    private final ConstructionDiaryOccurrenceRepository occurrences; private final ConstructionDiaryDecisionRepository decisions;
    private final ConstructionDiaryInstructionRepository instructions; private final ConstructionDiaryPhotoRepository photos;
    private final FileResourceRepository files; private final ConstructionDiaryAuthorizationService authorization;
    private final ConstructionDiaryMapper mapper; private final ActivityEventPublisher activity; private final NotificationDomainPublisher notifications;
    public ConstructionDiaryService(ConstructionDiaryEntryRepository entries, ProjectStageRepository stages, TenantUserRepository users,
        ConstructionDiaryParticipantRepository participants, ConstructionDiaryObservationRepository observations, ConstructionDiaryOccurrenceRepository occurrences,
        ConstructionDiaryDecisionRepository decisions, ConstructionDiaryInstructionRepository instructions, ConstructionDiaryPhotoRepository photos,
        FileResourceRepository files, ConstructionDiaryAuthorizationService authorization, ConstructionDiaryMapper mapper, ActivityEventPublisher activity, NotificationDomainPublisher notifications) {
        this.entries=entries;this.stages=stages;this.users=users;this.participants=participants;this.observations=observations;this.occurrences=occurrences;
        this.decisions=decisions;this.instructions=instructions;this.photos=photos;this.files=files;this.authorization=authorization;this.mapper=mapper;this.activity=activity;this.notifications=notifications;
    }
    @Transactional
    public DiaryResponse create(UUID tenantId, UUID authorId, String authorName, EntryRequest request) {
        var project=authorization.project(tenantId, request.projectId()); var entry=new ConstructionDiaryEntry();
        entry.setTenant(project.getTenant()); entry.setProject(project); entry.setCreatedBy(user(tenantId, authorId));
        // Persist the aggregate root first so child collections always reference a stable diary identifier.
        entry.setTitle(request.title()); entry.setEntryDate(request.entryDate()); entry.setEntryType(request.entryType());
        entries.save(entry); apply(entry, tenantId, request);
        activity.publishDiary(tenantId, project.getId(), null, null, authorId, authorName, ActivityType.DIARY_CREATED,
            ActivityVisibility.INTERNAL, "Diário de Obra iniciado", entry.getTitle(), null); return response(entry);
    }
    @Transactional
    public DiaryResponse update(UUID tenantId, UUID id, UUID authorId, String authorName, EntryRequest request) {
        var entry=authorization.entry(tenantId,id); if(!entry.getProject().getId().equals(request.projectId())) throw new BusinessException("Não é possível mover um registro entre projetos.");
        boolean published=entry.getStatus()==ConstructionDiaryStatus.PUBLISHED; apply(entry, tenantId, request);
        if(published){entry.setRevisionNumber(entry.getRevisionNumber()+1);entry.setLastRevisionAt(Instant.now());entry.setLastRevisionBy(user(tenantId,authorId));}
        activity.publishDiary(tenantId, entry.getProject().getId(), null, null, authorId, authorName, ActivityType.DIARY_UPDATED,
            entry.getVisibility(), "Diário de Obra atualizado", entry.getTitle(), null); return response(entry);
    }
    @Transactional
    public DiaryResponse archive(UUID tenantId, UUID id, UUID authorId, String authorName) {
        var entry=authorization.entry(tenantId,id); entry.setStatus(ConstructionDiaryStatus.ARCHIVED); entry.touch();
        activity.publishDiary(tenantId,entry.getProject().getId(),null,null,authorId,authorName,ActivityType.DIARY_UPDATED,ActivityVisibility.INTERNAL,"Registro arquivado",entry.getTitle(),null); return response(entry);
    }
    @Transactional
    public void delete(UUID tenantId, UUID id) { var entry=authorization.entry(tenantId,id); if(entry.getStatus()==ConstructionDiaryStatus.PUBLISHED) throw new BusinessException("Registros publicados devem ser arquivados, não removidos."); entry.setDeleted(true);entry.setDeletedAt(Instant.now()); }
    @Transactional
    public DiaryResponse publish(UUID tenantId, UUID id, UUID authorId, String authorName) {
        var entry=authorization.entry(tenantId,id); if(entry.getTitle().isBlank()||entry.getSummary()==null||entry.getSummary().isBlank()) throw new BusinessException("Informe o resumo da visita antes de publicar.");
        entry.setStatus(ConstructionDiaryStatus.PUBLISHED);entry.setPublishedAt(Instant.now());entry.setPublishedBy(user(tenantId,authorId)); entry.setLastRevisionAt(Instant.now());entry.setLastRevisionBy(entry.getPublishedBy());
        activity.publishDiary(tenantId,entry.getProject().getId(),null,null,authorId,authorName,ActivityType.DIARY_PUBLISHED,entry.getVisibility(),"Registro publicado no Diário de Obra",entry.getTitle(),null); return response(entry);
    }
    @Transactional
    public DiaryResponse addPhoto(UUID tenantId, UUID entryId, UUID authorId, String authorName, PhotoRequest request) {
        var entry=authorization.entry(tenantId,entryId); var file=files.findByIdAndTenantId(request.fileId(),tenantId).orElseThrow(()->new NotFoundException("Arquivo não encontrado."));
        if(file.getOwnerType()!=FileOwnerType.CONSTRUCTION_DIARY_ENTRY || !file.getOwnerId().equals(entryId)) throw new BusinessException("O arquivo deve pertencer a este registro do Diário.");
        var photo=new ConstructionDiaryPhoto();photo.setDiaryEntry(entry);photo.setFile(file);photo.setCaption(request.caption());photo.setDescription(request.description());photo.setOrder(request.order());photo.setVisibility(value(request.visibility()));photos.save(photo);
        activity.publishDiary(tenantId,entry.getProject().getId(),null,null,authorId,authorName,ActivityType.DIARY_FILE_ADDED,entry.getVisibility(),"Foto adicionada ao Diário",file.getName(),null); return response(entry);
    }
    @Transactional
    public DiaryResponse addOccurrence(UUID tenantId, UUID entryId, UUID authorId, String authorName, OccurrenceRequest request) {
        var entry=authorization.entry(tenantId,entryId); var occurrence=new ConstructionDiaryOccurrence();occurrence.setDiaryEntry(entry);copyOccurrence(occurrence,tenantId,request);occurrences.save(occurrence);
        activity.publishDiary(tenantId,entry.getProject().getId(),null,null,authorId,authorName,ActivityType.DIARY_OCCURRENCE_CREATED,entry.getVisibility(),"Ocorrência registrada",occurrence.getTitle(),null);notifications.criticalOccurrence(occurrence);return response(entry);
    }
    @Transactional
    public DiaryResponse addDecision(UUID tenantId, UUID entryId, UUID authorId, String authorName, DecisionRequest request) {
        var entry=authorization.entry(tenantId,entryId);var decision=new ConstructionDiaryDecision();decision.setDiaryEntry(entry);decision.setDescription(request.description());decision.setDecidedBy(request.decidedBy());decision.setDecisionDate(request.decisionDate());decision.setVisibility(value(request.visibility()));decisions.save(decision);
        activity.publishDiary(tenantId,entry.getProject().getId(),null,null,authorId,authorName,ActivityType.DIARY_DECISION_REGISTERED,entry.getVisibility(),"Decisão registrada",request.description(),null);return response(entry);
    }
    private void apply(ConstructionDiaryEntry entry, UUID tenantId, EntryRequest r){
        entry.setResponsibleUser(r.responsibleUserId()==null?null:user(tenantId,r.responsibleUserId()));entry.setEntryType(r.entryType());entry.setTitle(r.title());entry.setEntryDate(r.entryDate());entry.setStartTime(r.startTime());entry.setEndTime(r.endTime());entry.setLocation(r.location());entry.setSummary(r.summary());entry.setWeatherCondition(r.weatherCondition());entry.setTemperature(r.temperature());entry.setVisibility(value(r.visibility()));entry.setNextVisitDate(r.nextVisitDate());entry.setNextVisitNotes(r.nextVisitNotes());
        var selected=new LinkedHashSet<ProjectStage>();for(var stageId:optional(r.stageIds())){var stage=stages.findByIdAndTenantIdAndDeletedFalse(stageId,tenantId).orElseThrow(()->new NotFoundException("Etapa não encontrada."));if(!stage.getProjectPhase().getProject().getId().equals(entry.getProject().getId()))throw new BusinessException("A etapa informada não pertence ao projeto.");selected.add(stage);}entry.setStages(selected);
        participants.deleteByDiaryEntryId(entry.getId());observations.deleteByDiaryEntryId(entry.getId());occurrences.deleteByDiaryEntryId(entry.getId());decisions.deleteByDiaryEntryId(entry.getId());instructions.deleteByDiaryEntryId(entry.getId());
        for(var p:optional(r.participants())){var entity=new ConstructionDiaryParticipant();entity.setDiaryEntry(entry);entity.setParticipantType(p.participantType());entity.setTenantUser(p.tenantUserId()==null?null:user(tenantId,p.tenantUserId()));entity.setName(p.name());entity.setCompany(p.company());entity.setRole(p.role());entity.setPhone(p.phone());entity.setEmail(p.email());if(p.participantType()==ConstructionDiaryParticipantType.INTERNAL_USER&&entity.getTenantUser()==null)throw new BusinessException("Selecione o usuário interno.");participants.save(entity);}
        for(var o:optional(r.observations())){var entity=new ConstructionDiaryObservation();entity.setDiaryEntry(entry);entity.setTitle(o.title());entity.setDescription(o.description());entity.setCategory(o.category());entity.setStatus(o.status());entity.setOrder(o.order());entity.setVisibility(value(o.visibility()));observations.save(entity);}
        for(var o:optional(r.occurrences())){var entity=new ConstructionDiaryOccurrence();entity.setDiaryEntry(entry);copyOccurrence(entity,tenantId,o);occurrences.save(entity);}
        for(var d:optional(r.decisions())){var entity=new ConstructionDiaryDecision();entity.setDiaryEntry(entry);entity.setDescription(d.description());entity.setDecidedBy(d.decidedBy());entity.setDecisionDate(d.decisionDate());entity.setVisibility(value(d.visibility()));decisions.save(entity);}
        for(var i:optional(r.instructions())){var entity=new ConstructionDiaryInstruction();entity.setDiaryEntry(entry);entity.setDescription(i.description());entity.setResponsible(i.responsible());entity.setDeadline(i.deadline());entity.setCompleted(i.completed());entity.setVisibility(value(i.visibility()));instructions.save(entity);}
    }
    private void copyOccurrence(ConstructionDiaryOccurrence e,UUID tenantId,OccurrenceRequest r){e.setTitle(r.title());e.setDescription(r.description());e.setSeverity(r.severity());e.setResponsibleUser(r.responsibleUserId()==null?null:user(tenantId,r.responsibleUserId()));e.setDueDate(r.dueDate());e.setResolved(r.resolved());if(r.resolved())e.setResolvedAt(Instant.now());e.setResolution(r.resolution());e.setVisibility(value(r.visibility()));}
    public DiaryResponse response(ConstructionDiaryEntry e){var photosList=photos.findAllByDiaryEntryIdOrderByOrderAsc(e.getId());return new DiaryResponse(e.getId(),e.getProject().getId(),e.getProject().getName(),id(e.getResponsibleUser()),name(e.getResponsibleUser()),e.getEntryType(),e.getStatus(),e.getTitle(),e.getEntryDate(),e.getStartTime(),e.getEndTime(),e.getLocation(),e.getSummary(),e.getWeatherCondition(),e.getTemperature(),e.getVisibility(),e.getNextVisitDate(),e.getNextVisitNotes(),e.getPublishedAt(),name(e.getPublishedBy()),e.getRevisionNumber(),e.getLastRevisionAt(),name(e.getLastRevisionBy()),e.getStages().stream().map(ProjectStage::getId).toList(),e.getStages().stream().map(ProjectStage::getName).toList(),participants.findAllByDiaryEntryIdOrderByCreatedAtAsc(e.getId()).stream().map(mapper::toResponse).toList(),observations.findAllByDiaryEntryIdOrderByOrderAsc(e.getId()).stream().map(mapper::toResponse).toList(),occurrences.findAllByDiaryEntryIdOrderByCreatedAtAsc(e.getId()).stream().map(o->new OccurrenceResponse(o.getId(),o.getTitle(),o.getDescription(),o.getSeverity(),id(o.getResponsibleUser()),name(o.getResponsibleUser()),o.getDueDate(),o.isResolved(),o.getResolvedAt(),o.getResolution(),o.getVisibility())).toList(),decisions.findAllByDiaryEntryIdOrderByDecisionDateAsc(e.getId()).stream().map(d->new DecisionResponse(d.getId(),d.getDescription(),d.getDecidedBy(),d.getDecisionDate(),d.getVisibility())).toList(),instructions.findAllByDiaryEntryIdOrderByDeadlineAsc(e.getId()).stream().map(i->new InstructionResponse(i.getId(),i.getDescription(),i.getResponsible(),i.getDeadline(),i.isCompleted(),i.getVisibility())).toList(),photosList.stream().map(p->new PhotoResponse(p.getId(),p.getFile().getId(),p.getFile().getName(),p.getFile().getMimeType(),p.getFile().getSize(),p.getCaption(),p.getDescription(),p.getOrder(),p.getVisibility())).toList(),e.getCreatedAt(),name(e.getCreatedBy()));}
    private TenantUser user(UUID tenantId,UUID id){return users.findByIdAndTenantId(id,tenantId).orElseThrow(()->new NotFoundException("Usuário não encontrado."));} private UUID id(TenantUser u){return u==null?null:u.getId();}private String name(TenantUser u){return u==null?null:u.getName();}private ActivityVisibility value(ActivityVisibility v){return v==null?ActivityVisibility.INTERNAL:v;}private <T> List<T> optional(List<T> values){return values==null?List.of():values;}
}
