package com.arqly.backend.entity;

import jakarta.persistence.*;
import java.time.Instant;
import java.time.OffsetDateTime;

@Entity
@Table(name = "calendar_events")
public class CalendarEvent extends BaseEntity {
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "tenant_id", nullable = false)
    private Tenant tenant;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "project_id") private Project project;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "client_id") private Client client;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "stage_id") private ProjectStage stage;
    @Column(nullable = false) private String title;
    @Column(columnDefinition = "text") private String description;
    @Enumerated(EnumType.STRING) @Column(nullable = false) private CalendarEventType type = CalendarEventType.MEETING;
    @Column(nullable = false) private OffsetDateTime startDateTime;
    private OffsetDateTime endDateTime;
    @Column(nullable = false) private boolean allDay = false;
    private String location;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "responsible_user_id") private TenantUser responsibleUser;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "created_by_id", nullable = false) private TenantUser createdBy;
    @Enumerated(EnumType.STRING) @Column(nullable = false) private ActivityVisibility visibility = ActivityVisibility.INTERNAL;
    @Enumerated(EnumType.STRING) @Column(nullable = false) private CalendarEventStatus status = CalendarEventStatus.SCHEDULED;
    @Column(nullable = false) private boolean deleted = false;
    private Instant deletedAt;
    @PreUpdate void onUpdate() { touch(); }
    public Tenant getTenant(){return tenant;} public void setTenant(Tenant value){tenant=value;}
    public Project getProject(){return project;} public void setProject(Project value){project=value;}
    public Client getClient(){return client;} public void setClient(Client value){client=value;}
    public ProjectStage getStage(){return stage;} public void setStage(ProjectStage value){stage=value;}
    public String getTitle(){return title;} public void setTitle(String value){title=value;}
    public String getDescription(){return description;} public void setDescription(String value){description=value;}
    public CalendarEventType getType(){return type;} public void setType(CalendarEventType value){type=value;}
    public OffsetDateTime getStartDateTime(){return startDateTime;} public void setStartDateTime(OffsetDateTime value){startDateTime=value;}
    public OffsetDateTime getEndDateTime(){return endDateTime;} public void setEndDateTime(OffsetDateTime value){endDateTime=value;}
    public boolean isAllDay(){return allDay;} public void setAllDay(boolean value){allDay=value;}
    public String getLocation(){return location;} public void setLocation(String value){location=value;}
    public TenantUser getResponsibleUser(){return responsibleUser;} public void setResponsibleUser(TenantUser value){responsibleUser=value;}
    public TenantUser getCreatedBy(){return createdBy;} public void setCreatedBy(TenantUser value){createdBy=value;}
    public ActivityVisibility getVisibility(){return visibility;} public void setVisibility(ActivityVisibility value){visibility=value;}
    public CalendarEventStatus getStatus(){return status;} public void setStatus(CalendarEventStatus value){status=value;}
    public boolean isDeleted(){return deleted;} public void setDeleted(boolean value){deleted=value;}
    public Instant getDeletedAt(){return deletedAt;} public void setDeletedAt(Instant value){deletedAt=value;}
}
