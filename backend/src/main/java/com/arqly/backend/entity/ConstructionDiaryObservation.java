package com.arqly.backend.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "construction_diary_observations")
public class ConstructionDiaryObservation extends BaseEntity {
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "diary_entry_id", nullable = false) private ConstructionDiaryEntry diaryEntry;
    @Column(nullable = false) private String title;
    @Column(columnDefinition = "text") private String description;
    @Enumerated(EnumType.STRING) @Column(nullable = false) private ConstructionDiaryObservationCategory category = ConstructionDiaryObservationCategory.OTHER;
    @Enumerated(EnumType.STRING) @Column(nullable = false) private ConstructionDiaryObservationStatus status = ConstructionDiaryObservationStatus.INFO;
    @Column(name = "display_order", nullable = false) private int order;
    @Enumerated(EnumType.STRING) @Column(nullable = false) private ActivityVisibility visibility = ActivityVisibility.INTERNAL;
    public ConstructionDiaryEntry getDiaryEntry(){return diaryEntry;} public void setDiaryEntry(ConstructionDiaryEntry value){diaryEntry=value;}
    public String getTitle(){return title;} public void setTitle(String value){title=value;}
    public String getDescription(){return description;} public void setDescription(String value){description=value;}
    public ConstructionDiaryObservationCategory getCategory(){return category;} public void setCategory(ConstructionDiaryObservationCategory value){category=value;}
    public ConstructionDiaryObservationStatus getStatus(){return status;} public void setStatus(ConstructionDiaryObservationStatus value){status=value;}
    public int getOrder(){return order;} public void setOrder(int value){order=value;}
    public ActivityVisibility getVisibility(){return visibility;} public void setVisibility(ActivityVisibility value){visibility=value;}
}
