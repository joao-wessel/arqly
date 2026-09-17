package com.arqly.backend.service.calendar;

import com.arqly.backend.entity.*;
import com.arqly.backend.exception.*;
import com.arqly.backend.repository.*;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class CalendarAuthorizationService {
 private final ProjectRepository projects; private final ClientRepository clients; private final ProjectStageRepository stages; private final TenantUserRepository users;
 public CalendarAuthorizationService(ProjectRepository projects,ClientRepository clients,ProjectStageRepository stages,TenantUserRepository users){this.projects=projects;this.clients=clients;this.stages=stages;this.users=users;}
 public Project project(UUID tenant,UUID id){return id==null?null:projects.findByIdAndTenantIdAndDeletedFalse(id,tenant).orElseThrow(()->new NotFoundException("Projeto não encontrado."));}
 public Client client(UUID tenant,UUID id){return id==null?null:clients.findByIdAndTenantIdAndDeletedFalse(id,tenant).orElseThrow(()->new NotFoundException("Cliente não encontrado."));}
 public ProjectStage stage(UUID tenant,UUID id,Project project){if(id==null)return null;var stage=stages.findByIdAndTenantIdAndDeletedFalse(id,tenant).orElseThrow(()->new NotFoundException("Etapa não encontrada."));if(project!=null&&!stage.getProjectPhase().getProject().getId().equals(project.getId()))throw new BusinessException("A etapa deve pertencer ao projeto selecionado.");return stage;}
 public TenantUser user(UUID tenant,UUID id){return id==null?null:users.findByIdAndTenantId(id,tenant).orElseThrow(()->new NotFoundException("Usuário não encontrado."));}
}
