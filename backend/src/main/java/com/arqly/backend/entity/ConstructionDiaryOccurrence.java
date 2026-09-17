package com.arqly.backend.entity;

import jakarta.persistence.*;
import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(name = "construction_diary_occurrences")
public class ConstructionDiaryOccurrence extends BaseEntity {
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "diary_entry_id", nullable = false) private ConstructionDiaryEntry diaryEntry;
    @Column(nullable = false) private String title;
    @Column(columnDefinition = "text") private String description;
    @Enumerated(EnumType.STRING) @Column(nullable = false) private ConstructionDiarySeverity severity = ConstructionDiarySeverity.MEDIUM;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "responsible_user_id") private TenantUser responsibleUser;
    private LocalDate dueDate;
    @Column(nullable = false) private boolean resolved = false;
    private Instant resolvedAt;
    @Column(columnDefinition = "text") private String resolution;
    @Enumerated(EnumType.STRING) @Column(nullable = false) private ActivityVisibility visibility = ActivityVisibility.INTERNAL;
    public ConstructionDiaryEntry getDiaryEntry(){return diaryEntry;} public void setDiaryEntry(ConstructionDiaryEntry value){diaryEntry=value;}
    public String getTitle(){return title;} public void setTitle(String value){title=value;}
    public String getDescription(){return description;} public void setDescription(String value){description=value;}
    public ConstructionDiarySeverity getSeverity(){return severity;} public void setSeverity(ConstructionDiarySeverity value){severity=value;}
    public TenantUser getResponsibleUser(){return responsibleUser;} public void setResponsibleUser(TenantUser value){responsibleUser=value;}
    public LocalDate getDueDate(){return dueDate;} public void setDueDate(LocalDate value){dueDate=value;}
    public boolean isResolved(){return resolved;} public void setResolved(boolean value){resolved=value;}
    public Instant getResolvedAt(){return resolvedAt;} public void setResolvedAt(Instant value){resolvedAt=value;}
    public String getResolution(){return resolution;} public void setResolution(String value){resolution=value;}
    public ActivityVisibility getVisibility(){return visibility;} public void setVisibility(ActivityVisibility value){visibility=value;}
}
