package com.arqly.backend.entity;

import jakarta.persistence.*;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.LinkedHashSet;
import java.util.Set;

@Entity
@Table(name = "construction_diary_entries")
public class ConstructionDiaryEntry extends BaseEntity {
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "tenant_id", nullable = false)
    private Tenant tenant;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "project_id", nullable = false)
    private Project project;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "created_by_id")
    private TenantUser createdBy;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "responsible_user_id")
    private TenantUser responsibleUser;
    @Enumerated(EnumType.STRING) @Column(nullable = false)
    private ConstructionDiaryEntryType entryType = ConstructionDiaryEntryType.VISIT;
    @Enumerated(EnumType.STRING) @Column(nullable = false)
    private ConstructionDiaryStatus status = ConstructionDiaryStatus.DRAFT;
    @Column(nullable = false) private String title;
    @Column(name = "entry_date", nullable = false) private LocalDate entryDate;
    private LocalTime startTime;
    private LocalTime endTime;
    private String location;
    @Column(columnDefinition = "text") private String summary;
    private String weatherCondition;
    private java.math.BigDecimal temperature;
    @Enumerated(EnumType.STRING) @Column(nullable = false)
    private ActivityVisibility visibility = ActivityVisibility.INTERNAL;
    private LocalDate nextVisitDate;
    @Column(columnDefinition = "text") private String nextVisitNotes;
    private Instant publishedAt;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "published_by_id") private TenantUser publishedBy;
    @Column(nullable = false) private int revisionNumber = 1;
    private Instant lastRevisionAt;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "last_revision_by_id") private TenantUser lastRevisionBy;
    @Column(nullable = false) private boolean deleted = false;
    private Instant deletedAt;
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(name = "construction_diary_entry_stages", joinColumns = @JoinColumn(name = "diary_entry_id"), inverseJoinColumns = @JoinColumn(name = "stage_id"))
    private Set<ProjectStage> stages = new LinkedHashSet<>();
    @PreUpdate void onUpdate() { touch(); }
    public Tenant getTenant(){return tenant;} public void setTenant(Tenant value){tenant=value;}
    public Project getProject(){return project;} public void setProject(Project value){project=value;}
    public TenantUser getCreatedBy(){return createdBy;} public void setCreatedBy(TenantUser value){createdBy=value;}
    public TenantUser getResponsibleUser(){return responsibleUser;} public void setResponsibleUser(TenantUser value){responsibleUser=value;}
    public ConstructionDiaryEntryType getEntryType(){return entryType;} public void setEntryType(ConstructionDiaryEntryType value){entryType=value;}
    public ConstructionDiaryStatus getStatus(){return status;} public void setStatus(ConstructionDiaryStatus value){status=value;}
    public String getTitle(){return title;} public void setTitle(String value){title=value;}
    public LocalDate getEntryDate(){return entryDate;} public void setEntryDate(LocalDate value){entryDate=value;}
    public LocalTime getStartTime(){return startTime;} public void setStartTime(LocalTime value){startTime=value;}
    public LocalTime getEndTime(){return endTime;} public void setEndTime(LocalTime value){endTime=value;}
    public String getLocation(){return location;} public void setLocation(String value){location=value;}
    public String getSummary(){return summary;} public void setSummary(String value){summary=value;}
    public String getWeatherCondition(){return weatherCondition;} public void setWeatherCondition(String value){weatherCondition=value;}
    public java.math.BigDecimal getTemperature(){return temperature;} public void setTemperature(java.math.BigDecimal value){temperature=value;}
    public ActivityVisibility getVisibility(){return visibility;} public void setVisibility(ActivityVisibility value){visibility=value;}
    public LocalDate getNextVisitDate(){return nextVisitDate;} public void setNextVisitDate(LocalDate value){nextVisitDate=value;}
    public String getNextVisitNotes(){return nextVisitNotes;} public void setNextVisitNotes(String value){nextVisitNotes=value;}
    public Instant getPublishedAt(){return publishedAt;} public void setPublishedAt(Instant value){publishedAt=value;}
    public TenantUser getPublishedBy(){return publishedBy;} public void setPublishedBy(TenantUser value){publishedBy=value;}
    public int getRevisionNumber(){return revisionNumber;} public void setRevisionNumber(int value){revisionNumber=value;}
    public Instant getLastRevisionAt(){return lastRevisionAt;} public void setLastRevisionAt(Instant value){lastRevisionAt=value;}
    public TenantUser getLastRevisionBy(){return lastRevisionBy;} public void setLastRevisionBy(TenantUser value){lastRevisionBy=value;}
    public boolean isDeleted(){return deleted;} public void setDeleted(boolean value){deleted=value;}
    public Instant getDeletedAt(){return deletedAt;} public void setDeletedAt(Instant value){deletedAt=value;}
    public Set<ProjectStage> getStages(){return stages;} public void setStages(Set<ProjectStage> value){stages=value;}
}
