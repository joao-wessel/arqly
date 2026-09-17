package com.arqly.backend.service.calendar;

import com.arqly.backend.dto.CalendarDtos.*;
import com.arqly.backend.entity.*;
import com.arqly.backend.exception.*;
import com.arqly.backend.repository.*;
import com.arqly.backend.activity.ActivityEventPublisher;
import java.time.*;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CalendarEventService {
 private final CalendarEventRepository events; private final CalendarAuthorizationService authorization; private final CalendarConflictService conflicts; private final ActivityEventPublisher activity;
 public CalendarEventService(CalendarEventRepository events,CalendarAuthorizationService authorization,CalendarConflictService conflicts,ActivityEventPublisher activity){this.events=events;this.authorization=authorization;this.conflicts=conflicts;this.activity=activity;}
 @Transactional public CalendarEventResponse create(UUID tenant,UUID actor,CalendarEventRequest request){var author=authorization.user(tenant,actor);var event=new CalendarEvent();event.setTenant(author.getTenant());event.setCreatedBy(author);apply(event,tenant,request);events.save(event);publish(event,actor,ActivityType.CALENDAR_EVENT_CREATED,"Compromisso criado");return response(event);}
 @Transactional public CalendarEventResponse update(UUID tenant,UUID actor,UUID id,CalendarEventRequest request){var event=find(tenant,id);canEdit(event,actor);apply(event,tenant,request);publish(event,actor,ActivityType.CALENDAR_EVENT_UPDATED,"Compromisso atualizado");return response(event);}
 @Transactional public CalendarEventResponse complete(UUID tenant,UUID actor,UUID id){var event=find(tenant,id);canEdit(event,actor);event.setStatus(CalendarEventStatus.COMPLETED);publish(event,actor,ActivityType.CALENDAR_EVENT_COMPLETED,"Compromisso concluído");return response(event);}
 @Transactional public CalendarEventResponse cancel(UUID tenant,UUID actor,UUID id){var event=find(tenant,id);canEdit(event,actor);event.setStatus(CalendarEventStatus.CANCELLED);publish(event,actor,ActivityType.CALENDAR_EVENT_CANCELLED,"Compromisso cancelado");return response(event);}
 @Transactional public void delete(UUID tenant,UUID actor,UUID id){var event=find(tenant,id);canEdit(event,actor);event.setDeleted(true);event.setDeletedAt(Instant.now());}
 @Transactional(readOnly=true) public CalendarEventResponse get(UUID tenant,UUID id){return response(find(tenant,id));}
 private CalendarEvent find(UUID tenant,UUID id){return events.findByIdAndTenantIdAndDeletedFalse(id,tenant).orElseThrow(()->new NotFoundException("Evento não encontrado."));}
 private void canEdit(CalendarEvent event,UUID actor){if(event.getCreatedBy().getId().equals(actor)||(event.getResponsibleUser()!=null&&event.getResponsibleUser().getId().equals(actor)))return;throw new BusinessException("Você não pode editar este compromisso.");}
 private void apply(CalendarEvent event,UUID tenant,CalendarEventRequest r){if(r.endDateTime()!=null&&r.endDateTime().isBefore(r.startDateTime()))throw new BusinessException("O término deve ser posterior ao início.");var project=authorization.project(tenant,r.projectId());var client=authorization.client(tenant,r.clientId());if(project!=null&&client!=null&&!project.getClient().getId().equals(client.getId()))throw new BusinessException("Cliente e projeto precisam pertencer ao mesmo contexto.");if(project!=null&&client==null)client=project.getClient();var stage=authorization.stage(tenant,r.stageId(),project);event.setProject(project);event.setClient(client);event.setStage(stage);event.setResponsibleUser(authorization.user(tenant,r.responsibleUserId()));event.setTitle(r.title().trim());event.setDescription(r.description());event.setType(r.type());event.setStartDateTime(r.startDateTime());event.setEndDateTime(r.endDateTime());event.setAllDay(r.allDay());event.setLocation(r.location());event.setVisibility(r.visibility()==null?ActivityVisibility.INTERNAL:r.visibility());}
 private CalendarEventResponse response(CalendarEvent e){return new CalendarEventResponse(e.getId(),e.getTitle(),e.getDescription(),e.getType(),e.getStartDateTime(),e.getEndDateTime(),e.isAllDay(),e.getLocation(),e.getProject()==null?null:e.getProject().getId(),e.getClient()==null?null:e.getClient().getId(),e.getStage()==null?null:e.getStage().getId(),e.getResponsibleUser()==null?null:e.getResponsibleUser().getId(),e.getResponsibleUser()==null?null:e.getResponsibleUser().getName(),e.getVisibility(),e.getStatus(),e.isDeleted());}
 private void publish(CalendarEvent event,UUID actor,ActivityType type,String title){if(event.getVisibility()==ActivityVisibility.CLIENT_VISIBLE){activity.publishClient(event.getTenant().getId(),event.getProject()==null?null:event.getProject().getId(),event.getClient()==null?null:event.getClient().getId(),null,null,actor,event.getCreatedBy().getName(),type,title,event.getTitle(),null);}else{activity.publishFile(event.getTenant().getId(),event.getProject()==null?null:event.getProject().getId(),null,null,null,event.getClient()==null?null:event.getClient().getId(),actor,event.getCreatedBy().getName(),type,title,event.getTitle(),null);}}
}
