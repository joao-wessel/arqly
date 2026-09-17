package com.arqly.backend.service;

import com.arqly.backend.dto.ConstructionDiaryDtos.*;
import com.arqly.backend.entity.*;
import com.arqly.backend.repository.*;
import jakarta.persistence.criteria.Predicate;
import java.time.LocalDate;
import java.util.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ConstructionDiaryQueryService {
    private final ConstructionDiaryEntryRepository entries; private final ConstructionDiaryOccurrenceRepository occurrences; private final ConstructionDiaryPhotoRepository photos; private final ConstructionDiaryService service; private final ConstructionDiaryAuthorizationService authorization;
    public ConstructionDiaryQueryService(ConstructionDiaryEntryRepository entries, ConstructionDiaryOccurrenceRepository occurrences, ConstructionDiaryPhotoRepository photos, ConstructionDiaryService service, ConstructionDiaryAuthorizationService authorization){this.entries=entries;this.occurrences=occurrences;this.photos=photos;this.service=service;this.authorization=authorization;}
    @Transactional(readOnly=true) public DiaryResponse get(UUID tenantId, UUID id){return service.response(authorization.entry(tenantId,id));}
    @Transactional(readOnly=true) public Page<DiarySummaryResponse> list(UUID tenantId, UUID projectId, UUID stageId, UUID responsibleUserId, ConstructionDiaryEntryType type, ConstructionDiaryStatus status, ActivityVisibility visibility, Boolean critical, LocalDate from, LocalDate to, String text, Pageable pageable){return entries.findAll(spec(tenantId,projectId,stageId,responsibleUserId,type,status,visibility,critical,from,to,text),pageable).map(this::summary);}
    @Transactional(readOnly=true) public List<DiarySummaryResponse> project(UUID tenantId,UUID projectId){authorization.project(tenantId,projectId);return entries.findAllByProjectIdAndTenantIdAndDeletedFalseOrderByEntryDateDescCreatedAtDesc(projectId,tenantId).stream().map(this::summary).toList();}
    @Transactional(readOnly=true) public List<DiarySummaryResponse> stage(UUID tenantId,UUID projectId,UUID stageId){
        authorization.project(tenantId,projectId);
        return entries.findAll(spec(tenantId,projectId,stageId,null,null,null,null,null,null,null,null),
                org.springframework.data.domain.PageRequest.of(0,100,org.springframework.data.domain.Sort.by("entryDate").descending()))
                .getContent().stream().map(this::summary).toList();
    }
    @Transactional(readOnly=true) public DiaryStatsResponse stats(UUID tenantId){var last=entries.findTopByTenantIdAndDeletedFalseOrderByEntryDateDesc(tenantId).map(ConstructionDiaryEntry::getEntryDate).orElse(null);var next=entries.findTopByTenantIdAndNextVisitDateIsNotNullAndDeletedFalseOrderByNextVisitDateDesc(tenantId).map(ConstructionDiaryEntry::getNextVisitDate).orElse(null);return new DiaryStatsResponse(entries.countByTenantIdAndDeletedFalse(tenantId),occurrences.countByDiaryEntryTenantIdAndResolvedFalse(tenantId),occurrences.countByDiaryEntryTenantIdAndResolvedFalseAndSeverity(tenantId,ConstructionDiarySeverity.CRITICAL),last,next);}
    private DiarySummaryResponse summary(ConstructionDiaryEntry e){return new DiarySummaryResponse(e.getId(),e.getProject().getId(),e.getProject().getName(),e.getTitle(),e.getEntryType(),e.getStatus(),e.getEntryDate(),e.getResponsibleUser()==null?null:e.getResponsibleUser().getName(),occurrences.findAllByDiaryEntryIdOrderByCreatedAtAsc(e.getId()).size(),photos.findAllByDiaryEntryIdOrderByOrderAsc(e.getId()).size(),e.getVisibility(),e.getNextVisitDate(),e.getUpdatedAt());}
    private Specification<ConstructionDiaryEntry> spec(UUID tenantId,UUID projectId,UUID stageId,UUID responsibleUserId,ConstructionDiaryEntryType type,ConstructionDiaryStatus status,ActivityVisibility visibility,Boolean critical,LocalDate from,LocalDate to,String text){return (root,q,b)->{List<Predicate> p=new ArrayList<>();p.add(b.equal(root.get("tenant").get("id"),tenantId));p.add(b.isFalse(root.get("deleted")));if(projectId!=null)p.add(b.equal(root.get("project").get("id"),projectId));if(stageId!=null)p.add(b.equal(root.join("stages").get("id"),stageId));if(responsibleUserId!=null)p.add(b.equal(root.get("responsibleUser").get("id"),responsibleUserId));if(type!=null)p.add(b.equal(root.get("entryType"),type));if(status!=null)p.add(b.equal(root.get("status"),status));if(visibility!=null)p.add(b.equal(root.get("visibility"),visibility));if(from!=null)p.add(b.greaterThanOrEqualTo(root.get("entryDate"),from));if(to!=null)p.add(b.lessThanOrEqualTo(root.get("entryDate"),to));if(text!=null&&!text.isBlank()){var term="%"+text.toLowerCase()+"%";p.add(b.or(b.like(b.lower(root.get("title")),term),b.like(b.lower(root.get("summary")),term)));}if(Boolean.TRUE.equals(critical)){var sub=q.subquery(UUID.class);var o=sub.from(ConstructionDiaryOccurrence.class);sub.select(o.get("diaryEntry").get("id")).where(b.and(b.equal(o.get("diaryEntry").get("id"),root.get("id")),b.equal(o.get("severity"),ConstructionDiarySeverity.CRITICAL),b.isFalse(o.get("resolved"))));p.add(b.exists(sub));}return b.and(p.toArray(Predicate[]::new));};}
}
